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

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/v1/event")
public class MonitorEventTapResource
{
    private final Set<Monitor> monitors;
    private volatile ConcurrentMap<String, AtomicInteger> history = new MapMaker().makeMap();

    @Inject
    public MonitorEventTapResource(Set<Monitor> monitors)
    {
        this.monitors = monitors;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(List<Event> events)
    {
        if (events.isEmpty()) {
            return;
        }

        for (Monitor monitor : monitors) {
            monitor.processEvents(events);
        }

        // Assumes batches are homogeneous
        String key = events.get(0).getType();

        ConcurrentMap<String, AtomicInteger> historyRef = this.history;
        historyRef.putIfAbsent(key, new AtomicInteger(0));
        historyRef.get(key).addAndGet(events.size());
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getStats()
    {
        return Maps.transformValues(history, new Function<AtomicInteger, Integer>()
        {
            @Override
            public Integer apply(@Nullable AtomicInteger atomicInteger)
            {
                return atomicInteger.get();
            }
        });
    }

    @DELETE
    @Path("/stats")
    public void resetStats()
    {
        history = new MapMaker().makeMap();
    }
}
