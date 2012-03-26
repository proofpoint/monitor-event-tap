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
import java.util.concurrent.TimeUnit;

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
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 1, alerter);
        Assert.assertEquals(monitor.getName(), "foo");
        Assert.assertEquals(monitor.getEventType(), "event");
    }

    @Test
    public void testProcessEvents()
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 1, alerter);

        monitor.processEvents(nCopies(100, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));
        Assert.assertEquals(monitor.getEvents().getCount(), 100);
    }

    @Test
    public void testFilterEvents()
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, new EventTypeEventPredicate("event"), 1, alerter);

        monitor.processEvents(concat(
                nCopies(100, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                nCopies(100, new Event("not-event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of()))
        ));
        Assert.assertEquals(monitor.getEvents().getCount(), 100);
    }

    @Test
    public void testFailRecovery()
            throws Exception
    {

        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor monitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 1, alerter);
        Assert.assertEquals(monitor.getName(), "foo");
        Assert.assertEquals(monitor.getEventType(), "event");

        Assert.assertEquals(alerter.getAlerts().size(), 0);

        monitor.checkState();

        Assert.assertEquals(alerter.getAlerts().size(), 1);
        InMemoryAlert alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("failed"));

        alerter.getAlerts().clear();

        int eventCount = (int) (2 * TimeUnit.MINUTES.toSeconds(5));
        monitor.processEvents(nCopies(eventCount, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));

        monitor.getEvents().tick();
        monitor.checkState();

        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertFalse(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("recovered"));

        alerter.getAlerts().clear();

        // tick off 5 minutes in 5 second intervals
        long fiveSecondIntervalsIn5Minutes = TimeUnit.MINUTES.toSeconds(5) / 5;
        for (int i = 0; i < fiveSecondIntervalsIn5Minutes; i++) {
            monitor.getEvents().tick();
        }
        monitor.checkState();

        Assert.assertEquals(alerter.getAlerts().size(), 1);
        alert = alerter.getAlerts().get(0);
        Assert.assertEquals(alert.getName(), "foo");
        Assert.assertTrue(alert.isFailed());
        Assert.assertTrue(alert.getDescription().toLowerCase().contains("failed"));
    }
}
