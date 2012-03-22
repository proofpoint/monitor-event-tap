package com.proofpoint.event.monitor;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

@Path("/v1/event")
public class MonitorEventTapResource
{
    private final Set<Monitor> monitors;

    @Inject
    public MonitorEventTapResource(Set<Monitor> monitors)
    {
        this.monitors = monitors;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(List<Event> events)
    {
        for (Monitor monitor : monitors) {
            monitor.processEvents(events);
        }
    }
}
