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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MonitorJson
{
    private final String eventType;
    private final String eventFilter;
    private final Double minOneMinuteRate;
    private final Double maxOneMinuteRate;

    @JsonCreator
    public MonitorJson(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventFilter") String eventFilter,
            @JsonProperty("minOneMinuteRate") Double minOneMinuteRate,
            @JsonProperty("maxOneMinuteRate") Double maxOneMinuteRate)
    {
        this.eventType = eventType;
        this.eventFilter = eventFilter;
        this.minOneMinuteRate = minOneMinuteRate;
        this.maxOneMinuteRate = maxOneMinuteRate;
    }

    @JsonProperty
    public String getEventType()
    {
        return eventType;
    }

    @JsonProperty
    public String getEventFilter()
    {
        return eventFilter;
    }

    public EventPredicate getEventPredicate()
    {
        return new EventPredicate(eventType, eventFilter);
    }

    @JsonProperty
    public Double getMinOneMinuteRate()
    {
        return minOneMinuteRate;
    }

    @JsonProperty
    public Double getMaxOneMinuteRate()
    {
        return maxOneMinuteRate;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("MonitorJson");
        sb.append("{eventType='").append(eventType).append('\'');
        sb.append(", filter=").append(eventFilter);
        sb.append(", minOneMinuteRate=").append(minOneMinuteRate);
        sb.append(", maxOneMinuteRate=").append(maxOneMinuteRate);
        sb.append('}');
        return sb.toString();
    }
}
