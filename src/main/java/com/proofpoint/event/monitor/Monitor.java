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

import com.google.common.base.Preconditions;
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
    private final Double minimumOneMinuteRate;
    private final Double maximumOneMinuteRate;
    private final AtomicBoolean failed = new AtomicBoolean();
    private ScheduledFuture<?> scheduledFuture;

    public Monitor(String name, String eventType, ScheduledExecutorService executor, Predicate<Event> filter, Double minimumOneMinuteRate, Double maximumOneMinuteRate, Alerter alerter)
    {
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkNotNull(eventType, "eventType is null");
        Preconditions.checkNotNull(executor, "executor is null");
        Preconditions.checkNotNull(filter, "filter is null");
        Preconditions.checkArgument(minimumOneMinuteRate != null || maximumOneMinuteRate != null, "A minimum value or maximum value must be provided");
        Preconditions.checkNotNull(alerter, "alerter is null");

        this.name = name;
        this.eventType = eventType;
        this.executor = executor;
        this.filter = filter;
        this.alerter = alerter;
        counterStat = new CounterStat(executor);
        this.minimumOneMinuteRate = minimumOneMinuteRate;
        this.maximumOneMinuteRate = maximumOneMinuteRate;
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

    @Managed(description = "Minimum value for the one minute rate")
    public Double getMinimumOneMinuteRate()
    {
        return minimumOneMinuteRate;
    }

    @Managed(description = "Maximum value for the one minute rate")
    public Double getMaximumOneMinuteRate()
    {
        return maximumOneMinuteRate;
    }

    @Managed(description = "Is this monitor in the failed state?")
    public boolean isFailed()
    {
        return failed.get();
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
        if (minimumOneMinuteRate != null && maximumOneMinuteRate != null) {
            if (minimumOneMinuteRate <= oneMinuteRate && oneMinuteRate <= maximumOneMinuteRate) {
                recovered(String.format("The oneMinuteRate is now between %s and %s", minimumOneMinuteRate, maximumOneMinuteRate));
            }
            else {
                failed(String.format("Expected oneMinuteRate to be between %s and %s, but is %s", minimumOneMinuteRate, maximumOneMinuteRate, oneMinuteRate));
            }
        } else if (minimumOneMinuteRate != null) {
            if (minimumOneMinuteRate <= oneMinuteRate) {
                recovered(String.format("The oneMinuteRate is now greater than %s", minimumOneMinuteRate));
            }
            else {
                failed(String.format("Expected oneMinuteRate to be greater than %s, but is %s", minimumOneMinuteRate, oneMinuteRate));
            }
        } else if (maximumOneMinuteRate != null) {
            if (oneMinuteRate <= maximumOneMinuteRate) {
                recovered(String.format("The oneMinuteRate is now less than %s", maximumOneMinuteRate));
            }
            else {
                failed(String.format("Expected oneMinuteRate to be less than %s, but is %s", maximumOneMinuteRate, oneMinuteRate));
            }
        }
    }

    private void failed(String description)
    {
        if (failed.compareAndSet(false, true)) {
            // fire error message
            alerter.failed(name, description);
        }
    }

    private void recovered(String description)
    {
        if (failed.compareAndSet(true, false)) {
            // fire recovery message
            alerter.recovered(name, description);
        }
    }

    public void processEvents(Iterable<Event> events)
    {
        int count = Iterables.size(Iterables.filter(events, filter));
        counterStat.update(count);
    }
}
