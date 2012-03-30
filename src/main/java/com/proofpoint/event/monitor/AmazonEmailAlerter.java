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

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.google.common.base.Preconditions;
import com.proofpoint.log.Logger;
import org.weakref.jmx.Managed;

import javax.inject.Inject;

public class AmazonEmailAlerter implements Alerter
{
    private static final Logger log = Logger.get(AmazonEmailAlerter.class);
    private final String fromAddress;
    private final String toAddress;
    private final AmazonSimpleEmailService emailService;

    @Inject
    public AmazonEmailAlerter(AmazonConfig config, AmazonSimpleEmailService emailService)
    {
        Preconditions.checkNotNull(config, "config is null");
        Preconditions.checkNotNull(emailService, "emailService is null");

        fromAddress = config.getFromAddress();
        toAddress = config.getToAddress();
        this.emailService = emailService;
    }

    @Override
    public void failed(String name, String description)
    {
        try {
            sendMessage("Failed: " + name, "Failed " + name + ": " + description);
        }
        catch (Exception e) {
            log.error(e, "Failed to send failed alert");
        }
    }

    @Override
    public void recovered(String name, String description)
    {
        try {
            sendMessage("Recovered: " + name, "RECOVERED " + name + ": " + description);
        }
        catch (Exception e) {
            log.error(e, "Failed to send recovered alert");
        }
    }

    @Managed
    public void sendMessage(String subject, String body)
    {
        SendEmailRequest request = new SendEmailRequest()
                .withSource(fromAddress)
                .withDestination(new Destination().withToAddresses(toAddress))
                .withMessage(new Message()
                        .withSubject(new Content(subject))
                        .withBody(new Body(new Content(body))));

        emailService.sendEmail(request);
    }
}
