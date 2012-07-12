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
                .setAlertingEnabled(true)
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
                .put("alerter.enabled", "false")
                .build();

        AmazonConfig expected = new AmazonConfig()
                .setCloudWatchUpdateTime(new Duration(30, TimeUnit.SECONDS))
                .setAwsAccessKey("access")
                .setAwsSecretKey("secret")
                .setFromAddress("from")
                .setToAddress("to")
                .setAlertingEnabled(false);

        ConfigAssertions.assertFullMapping(properties, expected);
    }

}
