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
