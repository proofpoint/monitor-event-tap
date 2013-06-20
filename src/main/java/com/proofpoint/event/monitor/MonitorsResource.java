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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.Set;

@Path("/v1/monitor")
public class MonitorsResource
{
    private final Map<String, Monitor> monitors;

    @Inject
    public MonitorsResource(Set<Monitor> monitors)
    {
        ImmutableMap.Builder<String, Monitor> monitorBuilder = ImmutableMap.builder();
        for (Monitor monitor : monitors) {
            monitorBuilder.put(monitor.getName(), monitor);
        }
        this.monitors = monitorBuilder.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Iterable<MonitorRepresentation> getAll(@Context final UriInfo uriInfo)
    {
        return Iterables.transform(monitors.values(), new Function<Monitor, MonitorRepresentation>()
        {
            @Override
            public MonitorRepresentation apply(@Nullable Monitor monitor)
            {
                return MonitorRepresentation.of(monitor, uriInfo);
            }
        });
    }

    @GET
    @Path("/{monitor}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitorRepresentation(@PathParam("monitor") final String monitorName, @Context UriInfo uriInfo)
    {
        Monitor monitor = monitors.get(monitorName);

        if (monitor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(MonitorRepresentation.of(monitor, uriInfo)).build();
    }

    @VisibleForTesting
    static class MonitorRepresentation
    {
        private final String name;
        private final boolean ok;
        private final Double minimumOneMinuteRate;
        private final Double maximumOneMinuteRate;
        private final double oneMinuteRate;
        private final double fiveMinuteRate;
        private final double fifteenMinuteRate;
        private final URI self;

        static MonitorRepresentation of(Monitor monitor, UriInfo context)
        {
            Preconditions.checkNotNull(monitor, "Monitor is null");
            CounterStat stats = Preconditions.checkNotNull(monitor.getEvents(), "monitor.getEvents() is null");
            return new MonitorRepresentation(monitor.getName(), !monitor.isFailed(), monitor.getMinimumOneMinuteRate(), monitor.getMaximumOneMinuteRate(), stats.getOneMinuteRate(), stats.getFiveMinuteRate(), stats.getFiveMinuteRate(), selfUri(monitor, context));
        }

        private MonitorRepresentation(String name, boolean ok, Double minimumOneMinuteRate, Double maximumOneMinuteRate, double oneMinuteRate, double fiveMinuteRate, double fifteenMinuteRate, URI self)
        {
            this.name = name;
            this.ok = ok;
            this.minimumOneMinuteRate = minimumOneMinuteRate;
            this.maximumOneMinuteRate = maximumOneMinuteRate;
            this.oneMinuteRate = oneMinuteRate;
            this.fiveMinuteRate = fiveMinuteRate;
            this.fifteenMinuteRate = fifteenMinuteRate;
            this.self = self;
        }

        @JsonProperty
        public String getName()
        {
            return name;
        }

        @JsonProperty
        public boolean isOk()
        {
            return ok;
        }

        @JsonProperty
        public Double getMinimumOneMinuteRate()
        {
            return minimumOneMinuteRate;
        }

        @JsonProperty
        public Double getMaximumOneMinuteRate()
        {
            return maximumOneMinuteRate;
        }

        @JsonProperty
        public double getOneMinuteRate()
        {
            return oneMinuteRate;
        }

        @JsonProperty
        public double getFiveMinuteRate()
        {
            return fiveMinuteRate;
        }

        @JsonProperty
        public double getFifteenMinuteRate()
        {
            return fifteenMinuteRate;
        }

        @JsonProperty
        public URI getSelf()
        {
            return self;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MonitorRepresentation that = (MonitorRepresentation) o;

            if (Double.compare(that.fifteenMinuteRate, fifteenMinuteRate) != 0) {
                return false;
            }
            if (Double.compare(that.fiveMinuteRate, fiveMinuteRate) != 0) {
                return false;
            }
            if (ok != that.ok) {
                return false;
            }
            if (Double.compare(that.oneMinuteRate, oneMinuteRate) != 0) {
                return false;
            }
            if (maximumOneMinuteRate != null ? !maximumOneMinuteRate.equals(that.maximumOneMinuteRate) : that.maximumOneMinuteRate != null) {
                return false;
            }
            if (minimumOneMinuteRate != null ? !minimumOneMinuteRate.equals(that.minimumOneMinuteRate) : that.minimumOneMinuteRate != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            if (self != null ? !self.equals(that.self) : that.self != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result;
            long temp;
            result = name != null ? name.hashCode() : 0;
            result = 31 * result + (ok ? 1 : 0);
            result = 31 * result + (minimumOneMinuteRate != null ? minimumOneMinuteRate.hashCode() : 0);
            result = 31 * result + (maximumOneMinuteRate != null ? maximumOneMinuteRate.hashCode() : 0);
            temp = oneMinuteRate != +0.0d ? Double.doubleToLongBits(oneMinuteRate) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = fiveMinuteRate != +0.0d ? Double.doubleToLongBits(fiveMinuteRate) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = fifteenMinuteRate != +0.0d ? Double.doubleToLongBits(fifteenMinuteRate) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (self != null ? self.hashCode() : 0);
            return result;
        }
    }

    private static URI selfUri(Monitor monitor, UriInfo uriInfo)
    {
        return uriInfo.getBaseUriBuilder().path("/v1/monitor/{monitor}").build(monitor.getName());
    }
}
