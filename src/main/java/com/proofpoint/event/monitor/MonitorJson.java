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
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.proofpoint.event.monitor.EventPredicates.EventTypeEventPredicate;
import com.proofpoint.event.monitor.EventPredicates.StartsWithEventPredicate;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MonitorJson
{
    private final String eventType;
    private final FilterJson filter;
    private final double minOneMinuteRate;

    @JsonCreator
    public MonitorJson(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("filter") FilterJson filter,
            @JsonProperty("minOneMinuteRate") double minOneMinuteRate)
    {
        this.eventType = eventType;
        if (filter != null) {
            this.filter = filter;
        }
        else {
            this.filter = new FilterJson(ImmutableMap.<String, String>of());
        }
        this.minOneMinuteRate = minOneMinuteRate;
    }

    @JsonProperty
    public String getEventType()
    {
        return eventType;
    }

    @JsonProperty
    public FilterJson getFilter()
    {
        return filter;
    }

    public Predicate<Event> getEventPredicate()
    {
        return Predicates.and(new EventTypeEventPredicate(eventType), filter.toPredicate());
    }

    @JsonProperty
    public double getMinOneMinuteRate()
    {
        return minOneMinuteRate;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("MonitorJson");
        sb.append("{eventType='").append(eventType).append('\'');
        sb.append(", filter=").append(filter);
        sb.append(", minOneMinuteRate=").append(minOneMinuteRate);
        sb.append('}');
        return sb.toString();
    }

    public static class FilterJson
    {
        private final Map<String, String> startsWith;

        @JsonCreator
        public FilterJson(
                @JsonProperty("startsWith") Map<String, String> startsWith
        )
        {
            this.startsWith = ImmutableMap.copyOf(startsWith);
        }

        @JsonProperty
        public Map<String, String> getStartsWith()
        {
            return startsWith;
        }

        public Predicate<Event> toPredicate()
        {
            if (startsWith.isEmpty()) {
                return Predicates.alwaysTrue();
            }

            List<Predicate<Event>> and = new ArrayList<Predicate<Event>>();
            for (final Entry<String, String> startWithEntry : startsWith.entrySet()) {
                and.add(new StartsWithEventPredicate(startWithEntry.getKey(), startWithEntry.getValue()));
            }
            return Predicates.and(and);
        }


        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("FilterJson");
            sb.append("{startsWith=").append(startsWith);
            sb.append('}');
            return sb.toString();
        }
    }
}
