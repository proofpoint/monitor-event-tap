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
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TestAmazonEmailAlerter
{
    private AmazonEmailAlerter alerter;

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
        AmazonSimpleEmailService service = new AmazonSimpleEmailServiceClient(awsCredentials);
        alerter = new AmazonEmailAlerter(
                new AmazonConfig()
                        .setFromAddress("anomalytics@proofpoint.com")
                        .setToAddress("anomalytics@proofpoint.com"),
                service);
    }

    @Test(groups = "aws")
    public void testSendMessage()
    {
        alerter.sendMessage("test subject", "test body");
    }

    @Test
    public void testSkipsIfDisabled()
    {
        AmazonSimpleEmailService mockEmailService = mock(AmazonSimpleEmailService.class);

        alerter = new AmazonEmailAlerter(
                        new AmazonConfig().setAlertingEnabled(false),
                        mockEmailService);

        alerter.sendMessage("test", "should not be sent");
        verifyZeroInteractions(mockEmailService);
    }
}
