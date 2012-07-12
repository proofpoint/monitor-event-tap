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
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import javax.inject.Singleton;

import static com.proofpoint.configuration.ConfigurationModule.bindConfig;

public class AmazonModule
        implements Module
{
    public void configure(Binder binder)
    {
        binder.requireExplicitBindings();
        binder.disableCircularProxies();

        binder.bind(Alerter.class).to(AmazonEmailAlerter.class).in(Scopes.SINGLETON);
        bindConfig(binder).to(AmazonConfig.class);

        binder.bind(CloudWatchUpdater.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    private AmazonSimpleEmailService provideAmazonS3(AWSCredentials credentials)
    {
        return new AmazonSimpleEmailServiceClient(credentials);
    }

    @Provides
    @Singleton
    private AmazonCloudWatch provideAmazonCloudWatch(AWSCredentials credentials)
    {
        return new AmazonCloudWatchClient(credentials);
    }

    @Provides
    @Singleton
    private AWSCredentials provideProviderCredentials(AmazonConfig config)
    {
        if (!config.isAlertingEnabled()) {
            return new BasicAWSCredentials("", "");
        }

        return new BasicAWSCredentials(config.getAwsAccessKey(), config.getAwsSecretKey());
    }
}
