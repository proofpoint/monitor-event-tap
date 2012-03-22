package com.proofpoint.event.monitor;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class InMemoryAlerter implements Alerter
{
    private List<InMemoryAlert> alerts = newArrayList();

    public List<InMemoryAlert> getAlerts()
    {
        return alerts;
    }

    @Override
    public void failed(String name, String description)
    {
        alerts.add(new InMemoryAlert(name, true, description));
    }

    @Override
    public void recovered(String name, String description)
    {
        alerts.add(new InMemoryAlert(name, false, description));
    }

    public static class InMemoryAlert
    {
        private String name;
        private boolean failed;
        private String description;

        public InMemoryAlert(String name, boolean failed, String description)
        {
            this.name = name;
            this.failed = failed;
            this.description = description;
        }

        public String getName()
        {
            return name;
        }

        public boolean isFailed()
        {
            return failed;
        }

        public String getDescription()
        {
            return description;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("InMemoryAlert");
            sb.append("{name='").append(name).append('\'');
            sb.append(", failed=").append(failed);
            sb.append(", description='").append(description).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
