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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.proofpoint.node.NodeInfo;
import org.logicalshift.concurrent.SerialScheduledExecutorService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TestCloudWatchUpdater
{
    private ScheduledExecutorService executor;
    private CloudWatchUpdater cloudWatchUpdater;

    @BeforeClass
    protected void setUp()
            throws Exception
    {
    }

    @BeforeClass(groups = "aws")
    @Parameters("aws-credentials-file")
    public void setUp(String awsCredentialsFile)
            throws Exception
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream(awsCredentialsFile));
        String awsAccessKey = properties.getProperty("aws.access-key");
        String awsSecretKey = properties.getProperty("aws.secret-key");

        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

        cloudWatchUpdater = new CloudWatchUpdater(new AmazonConfig(), new AmazonCloudWatchClient(awsCredentials), executor, new NodeInfo("test"));
    }


    @AfterClass(groups = "aws")
    public void tearDown()
            throws Exception
    {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Test(groups = "aws")
    public void testSendMessage()
            throws Exception
    {
        for (int i = 0; i < 100; i++) {
            cloudWatchUpdater.updateCloudWatch();
            Thread.sleep(30000);
        }
    }

    @Test
    public void testSkipsIfDisabled()
    {
        AmazonCloudWatch mockCloudWatch = mock(AmazonCloudWatch.class);

        cloudWatchUpdater =
                new CloudWatchUpdater(
                        new AmazonConfig().setAlertingEnabled(false),
                        mockCloudWatch,
                        new SerialScheduledExecutorService(),
                        new NodeInfo("test"));

        cloudWatchUpdater.updateCloudWatch();
        verifyZeroInteractions(mockCloudWatch);
    }
}
