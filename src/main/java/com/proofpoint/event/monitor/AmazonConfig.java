package com.proofpoint.event.monitor;

import com.proofpoint.configuration.Config;
import com.proofpoint.units.Duration;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

public class AmazonConfig
{
    private Duration cloudWatchUpdateTime = new Duration(10, TimeUnit.SECONDS);
    private String fromAddress;
    private String toAddress;
    private String awsAccessKey;
    private String awsSecretKey;

    @NotNull
    public Duration getCloudWatchUpdateTime()
    {
        return cloudWatchUpdateTime;
    }

    @Config("cloud-watch.update")
    public AmazonConfig setCloudWatchUpdateTime(Duration cloudWatchUpdateTime)
    {
        this.cloudWatchUpdateTime = cloudWatchUpdateTime;
        return this;
    }

    @NotNull
    public String getFromAddress()
    {
        return fromAddress;
    }

    @Config("alerter.from")
    public AmazonConfig setFromAddress(String fromAddress)
    {
        this.fromAddress = fromAddress;
        return this;
    }

    @NotNull
    public String getToAddress()
    {
        return toAddress;
    }

    @Config("alerter.to")
    public AmazonConfig setToAddress(String toAddress)
    {
        this.toAddress = toAddress;
        return this;
    }

    @NotNull
    public String getAwsAccessKey()
    {
        return awsAccessKey;
    }

    @Config("alerter.aws-access-key")
    public AmazonConfig setAwsAccessKey(String awsAccessKey)
    {
        this.awsAccessKey = awsAccessKey;
        return this;
    }

    @NotNull
    public String getAwsSecretKey()
    {
        return awsSecretKey;
    }

    @Config("alerter.aws-secret-key")
    public AmazonConfig setAwsSecretKey(String awsSecretKey)
    {
        this.awsSecretKey = awsSecretKey;
        return this;
    }
}
