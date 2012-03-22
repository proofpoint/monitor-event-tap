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
            if (value instanceof String) {
                String stringValue = (String) value;
                return stringValue.startsWith(prefix);
            }
            return false;
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
