package com.proofpoint.event.monitor;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.base.Preconditions;
import com.proofpoint.log.Logger;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.units.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CloudWatchUpdater
{
    private static final Logger log = Logger.get(CloudWatchUpdater.class);
    private final Duration updateTime;
    private final AmazonCloudWatch cloudWatch;
    private final ScheduledExecutorService executorService;
    private final NodeInfo nodeInfo;
    private ScheduledFuture<?> future;

    @Inject
    public CloudWatchUpdater(AmazonConfig config, AmazonCloudWatch cloudWatch, @MonitorExecutorService ScheduledExecutorService executorService, NodeInfo nodeInfo)
    {
        Preconditions.checkNotNull(config, "config is null");
        Preconditions.checkNotNull(cloudWatch, "cloudWatch is null");
        Preconditions.checkNotNull(executorService, "executorService is null");
        Preconditions.checkNotNull(nodeInfo, "nodeInfo is null");

        this.updateTime = config.getCloudWatchUpdateTime();
        this.cloudWatch = cloudWatch;
        this.executorService = executorService;
        this.nodeInfo = nodeInfo;
    }

    @PostConstruct
    public synchronized void start()
    {
        if (future == null) {
            future = executorService.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    try {
                        updateCloudWatch();
                    }
                    catch (Exception e) {
                        log.error(e, "CloudWatch update failed");
                    }
                }
            }, (long) updateTime.toMillis(), (long) updateTime.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public synchronized void stop()
    {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    public void updateCloudWatch()
    {
        MetricDatum datum = new MetricDatum()
                .withMetricName("Heartbeat")
                .withUnit(StandardUnit.None.toString())
                .withValue(1.0d)
                .withDimensions(
                        new Dimension().withName("Environment").withValue(nodeInfo.getEnvironment()),
                        new Dimension().withName("NodeId").withValue(nodeInfo.getNodeId()),
                        new Dimension().withName("Pool").withValue(nodeInfo.getPool()),
                        new Dimension().withName("Location").withValue(nodeInfo.getLocation())
                );

        if (nodeInfo.getBinarySpec() != null) {
            datum.withDimensions(new Dimension().withName("Binary").withValue(nodeInfo.getBinarySpec()));
        }
        if (nodeInfo.getConfigSpec() != null) {
            datum.withDimensions(new Dimension().withName("Config").withValue(nodeInfo.getConfigSpec()));
        }

        cloudWatch.putMetricData(new PutMetricDataRequest()
                .withNamespace("PP/Monitor")
                .withMetricData(datum)
        );
    }
}
