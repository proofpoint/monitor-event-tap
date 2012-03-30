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

public class EventPredicates
{
    public static class EventTypeEventPredicate implements Predicate<Event>
    {
        private final String eventType;

        public EventTypeEventPredicate(String eventType)
        {
            this.eventType = eventType;
        }

        @Override
        public boolean apply(Event event)
        {
            return eventType.equals(event.getType());
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("EventTypeEventPredicate");
            sb.append("{eventType='").append(eventType).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static class StartsWithEventPredicate implements Predicate<Event>
    {
        private final String field;
        private final String prefix;

        public StartsWithEventPredicate(String field, String prefix)
        {
            this.field = field;
            this.prefix = prefix;
        }

        @Override
        public boolean apply(Event event)
        {
            Object value = event.getData().get(field);
            if (value == null) {
                return false;
            }
            String stringValue = String.valueOf(value);
            return stringValue.startsWith(prefix);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("StartsWithEventPredicate");
            sb.append("{field='").append(field).append('\'');
            sb.append(", prefix='").append(prefix).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
