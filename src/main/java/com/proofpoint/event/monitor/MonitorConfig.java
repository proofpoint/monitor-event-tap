package com.proofpoint.event.monitor;

import com.proofpoint.configuration.Config;

import javax.validation.constraints.NotNull;

public class MonitorConfig
{
    private String monitorRulesFile = "etc/monitor.json";

    @NotNull
    public String getMonitorRulesFile()
    {
        return monitorRulesFile;
    }

    @Config("monitor.file")
    public MonitorConfig setMonitorRulesFile(String monitorRulesFile)
    {
        this.monitorRulesFile = monitorRulesFile;
        return this;
    }
}
