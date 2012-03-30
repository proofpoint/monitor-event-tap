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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.newHashMap;
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

        Map<String, Monitor> monitors = newHashMap();
        for (Monitor monitor : loader.load(json)) {
            monitors.put(monitor.getName(), monitor);
        }

        Assert.assertEquals(monitors.size(), 5);

        Monitor scorerHttpMonitor = monitors.get("ScorerHttpMonitor");
        Assert.assertNotNull(scorerHttpMonitor);
        Assert.assertEquals(scorerHttpMonitor.getName(), "ScorerHttpMonitor");
        Assert.assertEquals(scorerHttpMonitor.getEventType(), "HttpRequest");
        scorerHttpMonitor.processEvents(concat(
                nCopies(100, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/v1/scorer/foo", "responseCode", 204))),
                nCopies(100, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/v1/scorer/foo", "responseCode", 400))),
                nCopies(100, new Event("not-HttpRequest", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                nCopies(100, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/other/path")))
        ));
        Assert.assertEquals(scorerHttpMonitor.getEvents().getCount(), 100);

        Monitor prsMessageMonitor = monitors.get("PrsMessageMonitor");
        Assert.assertNotNull(prsMessageMonitor);
        Assert.assertEquals(prsMessageMonitor.getName(), "PrsMessageMonitor");
        Assert.assertEquals(prsMessageMonitor.getEventType(), "PrsMessage");
        prsMessageMonitor.processEvents(concat(
                nCopies(100, new Event("PrsMessage", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                nCopies(100, new Event("not-PrsMessage", "id", "host", new DateTime(), ImmutableMap.<String, Object>of()))
        ));
        Assert.assertEquals(prsMessageMonitor.getEvents().getCount(), 100);

        Monitor minMonitor = monitors.get("Min");
        Assert.assertNotNull(minMonitor);
        Assert.assertEquals(minMonitor.getName(), "Min");
        Assert.assertEquals(minMonitor.getEventType(), "MinEvent");
        Assert.assertEquals(minMonitor.getMinimumOneMinuteRate(), 11.0);
        Assert.assertNull(minMonitor.getMaximumOneMinuteRate());

        Monitor maxMonitor = monitors.get("Max");
        Assert.assertNotNull(maxMonitor);
        Assert.assertEquals(maxMonitor.getName(), "Max");
        Assert.assertEquals(maxMonitor.getEventType(), "MaxEvent");
        Assert.assertNull(maxMonitor.getMinimumOneMinuteRate());
        Assert.assertEquals(maxMonitor.getMaximumOneMinuteRate(), 33.0);

        Monitor betweenMonitor = monitors.get("Between");
        Assert.assertNotNull(betweenMonitor);
        Assert.assertEquals(betweenMonitor.getName(), "Between");
        Assert.assertEquals(betweenMonitor.getEventType(), "BetweenEvent");
        Assert.assertEquals(betweenMonitor.getMinimumOneMinuteRate(), 111.0);
        Assert.assertEquals(betweenMonitor.getMaximumOneMinuteRate(), 333.0);
    }
}
