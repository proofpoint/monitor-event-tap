package com.proofpoint.event.monitor;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.proofpoint.configuration.ConfigurationFactory;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.discovery.client.DiscoveryModule;
import com.proofpoint.experimental.jmx.JmxHttpModule;
import com.proofpoint.http.server.testing.TestingHttpServer;
import com.proofpoint.http.server.testing.TestingHttpServerModule;
import com.proofpoint.jaxrs.JaxrsModule;
import com.proofpoint.jmx.JmxModule;
import com.proofpoint.json.JsonCodec;
import com.proofpoint.json.JsonModule;
import com.proofpoint.node.testing.TestingNodeModule;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.nCopies;
import static org.testng.Assert.assertEquals;

public class TestServer
{
    private AsyncHttpClient client;
    private TestingHttpServer server;
    Monitor scorerHttpMonitor;
    Monitor prsMessageMonitor;

    @BeforeMethod
    public void setup()
            throws Exception
    {
        ImmutableMap<String, String> config = ImmutableMap.of("monitor.file", "src/test/resources/monitor.json");

        Injector injector = Guice.createInjector(
                new TestingNodeModule(),
                new TestingHttpServerModule(),
                new JsonModule(),
                new JaxrsModule(),
                new JmxHttpModule(),
                new JmxModule(),
                new DiscoveryModule(),
                new MainModule(),
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        binder.bind(Alerter.class).to(InMemoryAlerter.class).in(Scopes.SINGLETON);
                    }
                },
                new ConfigurationModule(new ConfigurationFactory(config)));

        server = injector.getInstance(TestingHttpServer.class);

        List<Monitor> monitors = newArrayList(injector.getInstance(Key.get(new TypeLiteral<Set<Monitor>>() { })));
        Assert.assertEquals(monitors.size(), 2);

        if (monitors.get(0).getName().equals("ScorerHttpMonitor")) {
            scorerHttpMonitor = monitors.get(0);
            prsMessageMonitor = monitors.get(1);
        }
        else {
            scorerHttpMonitor = monitors.get(1);
            prsMessageMonitor = monitors.get(0);
        }

        server.start();
        client = new AsyncHttpClient();
    }

    @AfterMethod
    public void teardown()
            throws Exception
    {
        if (server != null) {
            server.stop();
        }

        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testPostEvents()
            throws Exception
    {
        List<Event> events = newArrayList(concat(
                nCopies(3, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/v1/scorer/foo"))),
                nCopies(5, new Event("not-HttpRequest", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                nCopies(7, new Event("HttpRequest", "id", "host", new DateTime(), ImmutableMap.of("requestUri", "/other/path"))),
                nCopies(11, new Event("PrsMessage", "id", "host", new DateTime(), ImmutableMap.<String, Object>of())),
                nCopies(13, new Event("not-PrsMessage", "id", "host", new DateTime(), ImmutableMap.<String, Object>of()))
        ));
        String json = JsonCodec.listJsonCodec(Event.class).toJson(events);

        Response response = client.preparePost(urlFor("/v1/event"))
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(json)
                .execute()
                .get();

        assertEquals(response.getStatusCode(), Status.NO_CONTENT.getStatusCode());
        Assert.assertEquals(scorerHttpMonitor.getEvents().getCount(), 3);
        Assert.assertEquals(prsMessageMonitor.getEvents().getCount(), 11);
    }

    private String urlFor(String path)
    {
        return server.getBaseUrl().resolve(path).toString();
    }
}
