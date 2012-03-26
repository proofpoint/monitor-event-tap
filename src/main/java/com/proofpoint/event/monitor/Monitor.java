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
    private final double minFiveMinuteRate;
    private final AtomicBoolean failed = new AtomicBoolean();
    private ScheduledFuture<?> scheduledFuture;

    public Monitor(String name, String eventType, ScheduledExecutorService executor, Predicate<Event> filter, double minFiveMinuteRate, Alerter alerter)
    {
        this.name = name;
        this.eventType = eventType;
        this.executor = executor;
        this.filter = filter;
        this.alerter = alerter;
        counterStat = new CounterStat(executor);
        this.minFiveMinuteRate = minFiveMinuteRate;
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
        double fiveMinuteRate = counterStat.getFiveMinuteRate();
        if (fiveMinuteRate < minFiveMinuteRate) {
            if (failed.compareAndSet(false, true)) {
                // fire error message
                alerter.failed(name, String.format("FAILED: Expected fiveMinuteRate to be greater than %s, but was %s", minFiveMinuteRate, fiveMinuteRate));
            }
        }
        else {
            if (failed.compareAndSet(true, false)) {
                // fire recovery message
                alerter.recovered(name, String.format("RECOVERED: The fiveMinuteRate is now greater than %s", minFiveMinuteRate));
            }
        }
    }

    public void processEvents(Iterable<Event> events)
    {
        int count = Iterables.size(Iterables.filter(events, filter));
        counterStat.update(count);
    }
}
