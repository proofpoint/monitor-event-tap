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
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TestMonitorEventTapResource
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
    public void testPostEvents()
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        Monitor fooMonitor = new Monitor("foo", "event", executor, Predicates.<Event>alwaysTrue(), 1, alerter);
        Monitor barMonitor = new Monitor("bar", "event", executor, Predicates.<Event>alwaysTrue(), 1, alerter);
        MonitorEventTapResource resource = new MonitorEventTapResource(ImmutableSet.of(fooMonitor, barMonitor));
        resource.post(Collections.nCopies(100, new Event("event", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())));

        Assert.assertEquals(fooMonitor.getEvents().getCount(), 100);
        Assert.assertEquals(barMonitor.getEvents().getCount(), 100);
    }
}
