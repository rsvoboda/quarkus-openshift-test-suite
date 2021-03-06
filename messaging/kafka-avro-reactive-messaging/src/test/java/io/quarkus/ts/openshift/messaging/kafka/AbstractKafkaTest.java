package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.ts.openshift.common.injection.TestResource;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSource;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractKafkaTest {

    private static final int TIMEOUT_SEC = 25;
    private static final int EVENTS_AMOUNT = 3;

    private String endpoint;
    private Client client = ClientBuilder.newClient();
    private List<String> receive = new CopyOnWriteArrayList<>();
    private boolean completed;

    @TestResource
    private URL sseEndpoint;

    @Test
    public void testAlertMonitorEventStream() throws InterruptedException {
        givenAnApplicationEndpoint(getEndpoint() + "/stock-price/stream");
        whenRequestSomeEvents(EVENTS_AMOUNT);
        thenVerifyAllEventsArrived();
    }

    protected void givenAnApplicationEndpoint(String endpoint) {
       this.endpoint = endpoint;
    }

    protected void whenRequestSomeEvents(int amount) throws InterruptedException {
        WebTarget target = client.target(endpoint);
        final CountDownLatch latch = new CountDownLatch(amount);

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> {
            receive.add(inboundSseEvent.readData(String.class, MediaType.APPLICATION_JSON_TYPE));
            latch.countDown();
        });

        source.open();
        completed = latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        source.close();
    }

    protected void thenVerifyAllEventsArrived(){
        assertTrue(completed, "Not all expected kafka events has been consumed.");
    }

    protected String getEndpoint() {
        return sseEndpoint.toString();
    }
}
