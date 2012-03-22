package com.proofpoint.event.monitor;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.proofpoint.json.JsonCodec;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.nCopies;

public class TestMonitorLoader
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
    public void testLoad()
            throws Exception
    {
        String json = Resources.toString(Resources.getResource("monitor.json"), Charsets.UTF_8);
        MonitorLoader loader = new MonitorLoader(executor, new InMemoryAlerter(), JsonCodec.mapJsonCodec(String.class, MonitorJson.class));
        List<Monitor> monitors = newArrayList(loader.load(json));

        Assert.assertEquals(monitors.size(), 2);

        Monitor scorerHttpMonitor;
        Monitor prsMessageMonitor;
        if (monitors.get(0).getName().equals("ScorerHttpMonitor")) {
            scorerHttpMonitor = monitors.get(0);
            prsMessageMonitor = monitors.get(1);
        }
        else {
            scorerHttpMonitor = monitors.get(1);
            prsMessageMonitor = monitors.get(0);
        }

        Assert.assertEquals(scorerHttpMonitor.getName(), "ScorerHttpMonitor");
        Assert.assertEquals(scorerHttpMonitor.getEventType(), "HttpRequest");
        scorerHttpMonitor.processEvents(concat(
                        nCopies(100, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/v1/scorer/foo"))),
                        nCopies(100, new Event("not-HttpRequest", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                        nCopies(100, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/other/path")))
                ));
        Assert.assertEquals(scorerHttpMonitor.getEvents().getCount(), 100);

        Assert.assertEquals(prsMessageMonitor.getName(), "PrsMessageMonitor");
        Assert.assertEquals(prsMessageMonitor.getEventType(), "PrsMessage");
        prsMessageMonitor.processEvents(concat(
                        nCopies(100, new Event("PrsMessage", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                        nCopies(100, new Event("not-PrsMessage", "id", "host", new DateTime(), ImmutableMap.<String, Object>of()))
                ));
        Assert.assertEquals(prsMessageMonitor.getEvents().getCount(), 100);
    }
}
