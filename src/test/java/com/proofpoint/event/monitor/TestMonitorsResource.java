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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.proofpoint.event.monitor.MonitorsResource.MonitorRepresentation;
import com.proofpoint.jaxrs.testing.MockUriInfo;
import com.proofpoint.testing.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.testng.Assert.assertEquals;

public class TestMonitorsResource
{
    private ScheduledExecutorService executor;
    private Monitor fooMonitor;
    private Monitor barMonitor;
    private MonitorsResource resource;

    @BeforeClass
    protected void setupOnce()
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

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        InMemoryAlerter alerter = new InMemoryAlerter();
        fooMonitor = new Monitor("foo", "event", executor, new EventPredicate("event", "true"), 1.0, 2.0, alerter);
        barMonitor = new Monitor("bar", "event", executor, new EventPredicate("event", "true"), 1.0, 2.0, alerter);
        resource = new MonitorsResource(ImmutableSet.of(fooMonitor, barMonitor));
    }

    @Test
    public void testListAllMonitors()
    {
        MockUriInfo uriInfo = new MockUriInfo(URI.create("http://example.com/v1/monitor"));
        Assertions.assertEqualsIgnoreOrder(
                resource.getAll(uriInfo),
                ImmutableList.of(MonitorsResource.MonitorRepresentation.of(fooMonitor, uriInfo), MonitorRepresentation.of(barMonitor, uriInfo)));

    }

    @Test
    public void testGetMonitor()
            throws Exception
    {
        MockUriInfo uriInfo = new MockUriInfo(URI.create("http://example.com/v1/monitor/foo"));

        Response response = resource.getMonitorRepresentation("foo", uriInfo);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        MonitorsResource.MonitorRepresentation representation = (MonitorsResource.MonitorRepresentation) response.getEntity();
        assertEquals(representation.getName(), "foo");
        assertEquals(representation.isOk(), true);
        assertEquals(representation.getMinimumOneMinuteRate(), 1.0);
        assertEquals(representation.getMaximumOneMinuteRate(), 2.0);
        assertEquals(representation.getOneMinuteRate(), 0.0);
    }

    @Test
    public void testGetMonitorNotFound()
            throws Exception
    {
        MockUriInfo uriInfo = new MockUriInfo(URI.create("http://example.com/v1/monitor/nothere"));
        Response response = resource.getMonitorRepresentation("nothere", uriInfo);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }
}

