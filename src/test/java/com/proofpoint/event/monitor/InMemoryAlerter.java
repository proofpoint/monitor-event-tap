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
