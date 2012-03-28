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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.weakref.jmx.Managed;
import org.weakref.jmx.Nested;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Monitor
{
    private final String name;
    private final String eventType;
    private final ScheduledExecutorService executor;
    private final Predicate<Event> filter;
    private final Alerter alerter;
    private final CounterStat counterStat;
    private final double minOneMinuteRate;
    private final AtomicBoolean failed = new AtomicBoolean();
    private ScheduledFuture<?> scheduledFuture;

    public Monitor(String name, String eventType, ScheduledExecutorService executor, Predicate<Event> filter, double minOneMinuteRate, Alerter alerter)
    {
        this.name = name;
        this.eventType = eventType;
        this.executor = executor;
        this.filter = filter;
        this.alerter = alerter;
        counterStat = new CounterStat(executor);
        this.minOneMinuteRate = minOneMinuteRate;
    }

    @PostConstruct
    public synchronized void start()
    {
        if (scheduledFuture == null) {
            scheduledFuture = executor.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    checkState();
                }
            }, 5 * 60, 30, TimeUnit.SECONDS);
            counterStat.start();
        }
    }

    @PreDestroy
    public synchronized void stop()
    {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }

        counterStat.stop();
    }

    @Managed
    public String getName()
    {
        return name;
    }

    @Managed
    public String getEventType()
    {
        return eventType;
    }

    @Managed
    @Nested
    public CounterStat getEvents()
    {
        return counterStat;
    }

    @Managed
    public void checkState()
    {
        double oneMinuteRate = counterStat.getOneMinuteRate();
        if (oneMinuteRate < minOneMinuteRate) {
            if (failed.compareAndSet(false, true)) {
                // fire error message
                alerter.failed(name, String.format("FAILED: Expected oneMinuteRate to be greater than %s, but was %s", minOneMinuteRate, oneMinuteRate));
            }
        }
        else {
            if (failed.compareAndSet(true, false)) {
                // fire recovery message
                alerter.recovered(name, String.format("RECOVERED: The oneMinuteRate is now greater than %s", minOneMinuteRate));
            }
        }
    }

    public void processEvents(Iterable<Event> events)
    {
        int count = Iterables.size(Iterables.filter(events, filter));
        counterStat.update(count);
    }
}
