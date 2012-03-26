package com.proofpoint.event.monitor;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.proofpoint.node.NodeInfo;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

}
