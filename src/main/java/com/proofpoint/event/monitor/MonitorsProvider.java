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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.proofpoint.discovery.client.Announcer;
import com.proofpoint.discovery.client.ServiceAnnouncement;
import com.proofpoint.http.server.HttpServerInfo;
import com.proofpoint.node.NodeInfo;
import org.weakref.jmx.MBeanExporter;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.inject.name.Names.named;
import static org.weakref.jmx.ObjectNames.generatedNameOf;

public class MonitorsProvider implements Provider<Set<Monitor>>
{
    private final MonitorLoader monitorLoader;
    private final String monitorRulesFile;
    private final HttpServerInfo httpServerInfo;
    private final String flowInfo;
    private final Announcer announcer;

    private Set<Monitor> monitors;
    private final List<ServiceAnnouncement> announcements = newArrayList();
    private final List<String> mbeanNames = newArrayList();
    private final MBeanExporter exporter;

    @Inject
    public MonitorsProvider(MonitorLoader monitorLoader,
            MonitorConfig config,
            HttpServerInfo httpServerInfo,
            NodeInfo nodeInfo,
            Announcer announcer,
            MBeanServer mbeanServer)
    {
        Preconditions.checkNotNull(monitorLoader, "monitorLoader is null");
        Preconditions.checkNotNull(config, "config is null");
        Preconditions.checkNotNull(httpServerInfo, "httpServerInfo is null");
        Preconditions.checkNotNull(nodeInfo, "nodeInfo is null");

        this.monitorLoader = monitorLoader;
        monitorRulesFile = config.getMonitorRulesFile();

        this.httpServerInfo = httpServerInfo;
        flowInfo = nodeInfo.getNodeId();

        this.announcer = announcer;

        if (mbeanServer != null) {
            exporter = new MBeanExporter(mbeanServer);
        }
        else {
            exporter = null;
        }
    }

    public MonitorsProvider(MonitorLoader monitorLoader,
            String monitorRulesFile,
            HttpServerInfo httpServerInfo,
            String flowInfo,
            Announcer announcer,
            MBeanServer mbeanServer)
    {
        Preconditions.checkNotNull(monitorLoader, "monitorLoader is null");
        Preconditions.checkNotNull(monitorRulesFile, "monitorRulesFile is null");
        Preconditions.checkNotNull(httpServerInfo, "httpServerInfo is null");
        Preconditions.checkNotNull(flowInfo, "flowInfo is null");

        this.monitorLoader = monitorLoader;
        this.monitorRulesFile = monitorRulesFile;
        this.httpServerInfo = httpServerInfo;
        this.flowInfo = flowInfo;

        this.announcer = announcer;

        if (mbeanServer != null) {
            exporter = new MBeanExporter(mbeanServer);
        }
        else {
            exporter = null;
        }
    }

    @Override
    public synchronized Set<Monitor> get()
    {
        if (monitors == null) {
            try {
                String json = Files.toString(new File(monitorRulesFile), Charsets.UTF_8);
                monitors = monitorLoader.load(json);
            }
            catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        for (Monitor monitor : monitors) {
            monitor.start();
        }

        Set<String> eventTypes = newHashSet();
        for (Monitor monitor : monitors) {
            eventTypes.add(monitor.getEventType());
        }

        if (announcer != null) {
            for (String eventType : eventTypes) {
                ServiceAnnouncement announcement = ServiceAnnouncement.serviceAnnouncement("eventTap")
                        .addProperty("http", httpServerInfo.getHttpUri().toString() + "/v1/event")
                        .addProperty("tapId", flowInfo)
                        .addProperty("eventType", eventType)
                        .build();
                announcer.addServiceAnnouncement(announcement);
                announcements.add(announcement);
            }
        }

        if (exporter != null) {
            for (Monitor monitor : monitors) {
                String name = generatedNameOf(Monitor.class, named(monitor.getName()));
                exporter.export(name, monitor);
                mbeanNames.add(name);
            }
        }

        return monitors;
    }

    @PreDestroy
    public synchronized void stop()
    {
        if (monitors == null) {
            return;
        }

        if (announcer != null) {
            for (ServiceAnnouncement announcement : announcements) {
                announcer.removeServiceAnnouncement(announcement.getId());
            }
        }

        if (exporter != null) {
            for (String name : mbeanNames) {
                try {
                    // TODO: use unexportAll when jmxutils is upgraded to 1.11+
                    exporter.unexport(name);
                }
                catch (Exception ignored) {
                }
            }
        }

        for (Monitor monitor : monitors) {
            monitor.stop();
        }
    }
}
