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
}
