package com.proofpoint.event.monitor;

import com.google.common.collect.ImmutableSet;
import com.proofpoint.json.JsonCodec;

import javax.inject.Inject;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class MonitorLoader
{
    private final ScheduledExecutorService executor;
    private final Alerter alerter;
    private final JsonCodec<Map<String, MonitorJson>> codec;

    @Inject
    public MonitorLoader(@MonitorExecutorService ScheduledExecutorService executor, Alerter alerter, JsonCodec<Map<String, MonitorJson>> codec)
    {
        this.executor = executor;
        this.alerter = alerter;
        this.codec = codec;
    }

    public Set<Monitor> load(String json)
    {
        ImmutableSet.Builder<Monitor> monitors = ImmutableSet.builder();
        Map<String, MonitorJson> monitorJsonMap = codec.fromJson(json);
        for (Entry<String, MonitorJson> entry : monitorJsonMap.entrySet()) {
            String name = entry.getKey();
            MonitorJson monitorJson = entry.getValue();
            Monitor monitor = new Monitor(name, monitorJson.getEventType(), executor, monitorJson.getEventPredicate(), monitorJson.getMinOneMinuteRate(), alerter);
            monitors.add(monitor);
        }
        return monitors.build();
    }
}
