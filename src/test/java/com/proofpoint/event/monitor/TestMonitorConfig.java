package com.proofpoint.event.monitor;

import com.google.common.collect.ImmutableMap;
import com.proofpoint.configuration.testing.ConfigAssertions;
import org.testng.annotations.Test;

import java.util.Map;

public class TestMonitorConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(MonitorConfig.class)
                .setMonitorRulesFile("etc/monitor.json")
        );
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("monitor.file", "file")
                .build();

        MonitorConfig expected = new MonitorConfig()
                .setMonitorRulesFile("file");

        ConfigAssertions.assertFullMapping(properties, expected);
    }

}
