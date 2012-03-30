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

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class TestEventPredicate
{
    @Test
    public void test()
    {
        Assert.assertTrue(applyPredicate(null, null, null));
        Assert.assertTrue(applyPredicate(null, "/v1/scorer/foo", 204));
        Assert.assertTrue(applyPredicate("true", "/v1/scorer/foo", 204));
        Assert.assertFalse(applyPredicate("false", "/v1/scorer/foo", 204));

        Assert.assertTrue(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", "/v1/scorer", 204));
        Assert.assertTrue(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", "/v1/scorer/foo", 204));
        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", "/v1/foo", 204));
        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", "/v1/scorer/foo", 100));
        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", "/v1/scorer/foo", 400));

        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", null, null));
        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", "/v1/scorer", null));
        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && responseCode >= 200 && responseCode < 300", null, 200));

        Assert.assertTrue(applyPredicate("requestUri.startsWith('/v1/scorer') && (responseCode < 200 || responseCode >= 300)", "/v1/scorer", 100));
        Assert.assertTrue(applyPredicate("requestUri.startsWith('/v1/scorer') && (responseCode < 200 || responseCode >= 300)", "/v1/scorer", 400));
        Assert.assertFalse(applyPredicate("requestUri.startsWith('/v1/scorer') && (responseCode < 200 || responseCode >= 300)", "/v1/scorer", 200));

    }

    private boolean applyPredicate(String eventFilter, String uri, Integer responseCode)
    {
        return new EventPredicate("HttpRequest", eventFilter).apply(createHttpRequestEvent(uri, responseCode));
    }

    private Event createHttpRequestEvent(String uri, Integer responseCode)
    {
        Map<String, Object> data = newHashMap();
        data.put("requestUri", uri);
        data.put("responseCode", responseCode);

        return new Event("HttpRequest",
                "id",
                "host",
                new DateTime(),
                data);
    }
}
