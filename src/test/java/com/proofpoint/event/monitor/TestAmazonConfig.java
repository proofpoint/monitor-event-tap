package com.proofpoint.event.monitor;

import com.google.common.collect.ImmutableMap;
import com.proofpoint.configuration.testing.ConfigAssertions;
import com.proofpoint.units.Duration;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestAmazonConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(AmazonConfig.class)
                .setCloudWatchUpdateTime(new Duration(10, TimeUnit.SECONDS))
                .setAwsAccessKey(null)
                .setAwsSecretKey(null)
                .setFromAddress(null)
                .setToAddress(null)
        );
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("cloud-watch.update", "30s")
                .put("alerter.from", "from")
                .put("alerter.to", "to")
                .put("alerter.aws-access-key", "access")
                .put("alerter.aws-secret-key", "secret")
                .build();

        AmazonConfig expected = new AmazonConfig()
                .setCloudWatchUpdateTime(new Duration(30, TimeUnit.SECONDS))
                .setAwsAccessKey("access")
                .setAwsSecretKey("secret")
                .setFromAddress("from")
                .setToAddress("to");

        ConfigAssertions.assertFullMapping(properties, expected);
    }

}
