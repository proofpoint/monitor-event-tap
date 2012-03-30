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
import org.mvel2.MVEL;

public class EventPredicate implements Predicate<Event>
{
    private final String eventType;
    private final String eventFilter;
    private final Object expression;

    public EventPredicate(String eventType, String eventFilter)
    {
        this.eventType = eventType;
        this.eventFilter = eventFilter;
        expression = MVEL.compileExpression(eventFilter);
    }

    public String getEventType()
    {
        return eventType;
    }

    public String getEventFilter()
    {
        return eventFilter;
    }

    @Override
    public boolean apply(Event event)
    {
        if (!event.getType().equals(eventType)) {
            return false;
        }

        if (eventFilter == null) {
            return true;
        }

        try {
            // NOTE: null is considered false
            return MVEL.executeExpression(expression, event.getData(), Boolean.class) == Boolean.TRUE;
        }
        catch (Exception ignored) {
            // exceptions will be caused by bad data like missing fields
            return false;
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("EventPredicate");
        sb.append("{eventType='").append(eventType).append('\'');
        sb.append(", eventFilter='").append(eventFilter).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
