/*
 * Copyright 2011 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.event.monitor;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.proofpoint.event.monitor.EventPredicates.EventTypeEventPredicate;
import com.proofpoint.event.monitor.InMemoryAlerter.InMemoryAlert;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Iterables.concat;
import static java.util.Collections.nCopies;

public class TestMonitor
{

    private ScheduledExecutorService executor;

    @BeforeClass
    protected void setUp()
            throws Exception
    {
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
    }

    @AfterClass
    public void tearDown()
            throws Exception
    {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Test
    public void testBasics()
    {

        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 1.0, 2.0, alerter);
        Assert.assertEquals(monitor.getName(), "foo");
        Assert.assertEquals(monitor.getEventType(), "event");
        Assert.assertEquals(monitor.getMinimumOneMinuteRate(), 1.0);
        Assert.assertEquals(monitor.getMaximumOneMinuteRate(), 2.0);
        Assert.assertEquals(monitor.getEvents().getCount(), 0);
        Assert.assertEquals(monitor.isFailed(), false);
    }

    @Test
    public void testProcessEvents()
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 1.0, 2.0, alerter);

        monitor.processEvents(nCopies(100, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));
        Assert.assertEquals(monitor.getEvents().getCount(), 100);
    }

    @Test
    public void testFilterEvents()
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, new EventTypeEventPredicate("event"), 1.0, 2.0, alerter);

        monitor.processEvents(concat(
                nCopies(100, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                nCopies(100, new Event("not-event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of()))
        ));
        Assert.assertEquals(monitor.getEvents().getCount(), 100);
    }

    @Test
    public void testFailRecoveryMinimum()
            throws Exception
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 2.0, null, alerter);
        Assert.assertEquals(monitor.getName(), "foo");
        Assert.assertEquals(monitor.getEventType(), "event");

        // not-failed : initial state
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 0);

        monitor.checkState();

        // failed : below threshold
        Assert.assertTrue(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        InMemoryAlert alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("greater than"));

        alerter.getAlerts().clear();

        // set rate to be greater than the min value
        while (monitor.getEvents().getOneMinuteRate() < monitor.getMinimumOneMinuteRate()) {
            int eventCount = (int) (5 * monitor.getMinimumOneMinuteRate() * 5);
            monitor.processEvents(nCopies(eventCount, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // not-failed : above threshold
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertFalse(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("greater than"));

        alerter.getAlerts().clear();

        // wait for value to drift below minimum threshold
        while (monitor.getEvents().getOneMinuteRate() > monitor.getMinimumOneMinuteRate()) {
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // failed : below threshold
        Assert.assertTrue(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("greater than"));
    }

    @Test
    public void testFailRecoveryMaximum()
            throws Exception
    {

        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), null, 10.0, alerter);
        Assert.assertEquals(monitor.getName(), "foo");
        Assert.assertEquals(monitor.getEventType(), "event");

        // not-failed : initial state
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 0);

        monitor.checkState();

        // not-failed : no events processed so below max threshold
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 0);

        // set rate to be greater than the max value
        while (monitor.getEvents().getOneMinuteRate() < monitor.getMaximumOneMinuteRate()) {
            int eventCount = (int) (5 * monitor.getMaximumOneMinuteRate() * 5);
            monitor.processEvents(nCopies(eventCount, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // failed : over threshold
        Assert.assertTrue(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        InMemoryAlert alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("less than"));

        alerter.getAlerts().clear();

        // wait for value to drift below maximum threshold
        while (monitor.getEvents().getOneMinuteRate() > monitor.getMaximumOneMinuteRate()) {
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // not-failed : rate has decayed below threshold
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertFalse(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("less than"));

        alerter.getAlerts().clear();
    }

    @Test
    public void testFailRecoveryBetween()
            throws Exception
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 2.0, 100.0, alerter);
        Assert.assertEquals(monitor.getName(), "foo");
        Assert.assertEquals(monitor.getEventType(), "event");

        // not-failed : initial state
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 0);

        monitor.checkState();

        // failed : below minimum threshold
        Assert.assertTrue(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        InMemoryAlert alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("between"));

        alerter.getAlerts().clear();

        // set rate to be greater than the min value
        while (monitor.getEvents().getOneMinuteRate() < monitor.getMinimumOneMinuteRate()) {
            int eventCount = (int) (5 * monitor.getMinimumOneMinuteRate() * 5);
            monitor.processEvents(nCopies(eventCount, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // not-failed : between thresholds
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertFalse(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("between"));

        alerter.getAlerts().clear();

        // set rate to be greater than the max value
        while (monitor.getEvents().getOneMinuteRate() < monitor.getMaximumOneMinuteRate()) {
            int eventCount = (int) (5 * monitor.getMaximumOneMinuteRate() * 5);
            monitor.processEvents(nCopies(eventCount, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // failed : above maximum threshold
        Assert.assertTrue(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("between"));

        alerter.getAlerts().clear();

        // wait for value to drift below maximum threshold
        while (monitor.getEvents().getOneMinuteRate() > monitor.getMaximumOneMinuteRate()) {
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // not-failed : between thresholds
        Assert.assertFalse(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertFalse(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("between"));

        alerter.getAlerts().clear();

        // wait for value to drift below minimum threshold
        while (monitor.getEvents().getOneMinuteRate() > monitor.getMinimumOneMinuteRate()) {
            monitor.getEvents().tick();
        }
        monitor.checkState();

        // failed : below min threshold
        Assert.assertTrue(monitor.isFailed());
        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("between"));
    }
}
