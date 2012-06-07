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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.proofpoint.discovery.client.DiscoveryBinder;

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.proofpoint.configuration.ConfigurationModule.bindConfig;
import static com.proofpoint.json.JsonCodecBinder.jsonCodecBinder;

public class MainModule
        implements Module
{
    public void configure(Binder binder)
    {
        binder.requireExplicitBindings();
        binder.disableCircularProxies();

        binder.bind(MonitorEventTapResource.class).in(Scopes.SINGLETON);
        binder.bind(MonitorsResource.class).in(Scopes.SINGLETON);
        binder.bind(MonitorLoader.class).in(Scopes.SINGLETON);
        binder.bind(new TypeLiteral<Set<Monitor>>() {}).toProvider(MonitorsProvider.class).in(Scopes.SINGLETON);

        bindConfig(binder).to(MonitorConfig.class);
        jsonCodecBinder(binder).bindMapJsonCodec(String.class, MonitorJson.class);

        DiscoveryBinder.discoveryBinder(binder).bindHttpAnnouncement("monitor-event-tap");
    }

    @Provides
    @Singleton
    @MonitorExecutorService
    public ScheduledExecutorService createMonitorExecutorService()
    {
        return Executors.newScheduledThreadPool(5, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("monitor-%s").build());
    }
}
