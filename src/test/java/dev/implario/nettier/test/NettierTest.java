package dev.implario.nettier.test;

import com.google.gson.Gson;
import dev.implario.nettier.*;
import dev.implario.nettier.impl.client.NettierClientImpl;
import dev.implario.nettier.impl.server.NettierServerImpl;
import implario.LoggerUtils;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NettierTest {

    @Data
    public static class InverseRequest {


        private final double value;

    }
    @Data
    public static class InverseResponse {


        private final double inverse;

    }
    @Data
    public static class EvalError {


        private final String message;

    }
    public static class EvalException extends RuntimeException {

        public EvalException(String message) {
            super(message);
        }


    }

    private static NettierServer server;
    private static NettierClient client;

    private static final int TEST_PORT = 49146;
    private static final Gson gson = new Gson();

    @BeforeAll
    public static void prepare() throws InterruptedException {

        server = Nettier.createServer(gson, LoggerUtils.simpleLogger("Server"));
        client = Nettier.createClient(gson, LoggerUtils.simpleLogger("Client"));

//        ((NettierServerImpl) server).setDebugReads(true);
//        ((NettierServerImpl) server).setDebugWrites(true);

        server.addListener(InverseRequest.class, (talk, request) -> {
            if (request.getValue() == 0)
                talk.respond(new EvalError("Unable to inverse zero."));
            else
                talk.respond(1.0 / request.getValue());
        });

        server.start(TEST_PORT).await();

        client.setHandshakeHandler(r -> {});

        client.connect("127.0.0.1", TEST_PORT);

//        ((NettierClientImpl) client).setDebugReads(true);
//        ((NettierClientImpl) client).setDebugWrites(true);

        client.waitUntilReady();

    }

    @Test
    public void testSimpleCommunication() {

        Double result = client.send(new InverseRequest(PI)).await(Double.class);

        assertEquals(1.0 / PI, result);

    }

    @Test
    public void testTranslatorErrors() {

        client.setPacketTranslator((packet, type) -> {
            if (packet instanceof EvalError) {
                throw new EvalException(((EvalError) packet).getMessage());
            }
            return packet;
        });

        assertThrows(EvalException.class, () ->
                client.send(new InverseRequest(0)).await(InverseResponse.class)
        );
    }

    @Test
    public void testDelayedAwait() throws Exception {

        Talk talk = client.send(new InverseRequest(PI));

        Thread.sleep(100);

        Double result = talk.await(Double.class);

        assertEquals(1.0 / PI, result);

    }

    @Test
    public void testPacketQualification() {

        PacketQualifier qualifier = new PacketQualifier() {
            @Override
            public String getTypeForPacket(Object packet) {
                return packet instanceof Double ? "double" : null;
            }

            @Override
            public Class<?> getClassForType(String type) {
                return type.equals("double") ? Double.class : null;
            }
        };

        client.getQualifier().getQualifiers().add(0, qualifier);
        server.getQualifier().getQualifiers().add(0, qualifier);

        ((NettierServerImpl) server).setDebugWrites(true);
        ((NettierClientImpl) client).setDebugReads(true);

        Double result = client.send(new InverseRequest(PI)).await(Double.class);

        assertEquals(1.0 / PI, result);

        ((NettierServerImpl) server).setDebugWrites(false);
        ((NettierClientImpl) client).setDebugReads(false);

        client.getQualifier().getQualifiers().remove(0);
        server.getQualifier().getQualifiers().remove(0);

    }

    @Test
    public void testBroadcast() throws InterruptedException, ExecutionException, TimeoutException {

        CompletableFuture<String> future = new CompletableFuture<>();
        client.addListener(String.class, (talk, s) -> future.complete(s));

        server.broadcast("hello");

        assertEquals("hello", future.get(1, TimeUnit.SECONDS));

    }

    @Test
    public void testHandshake() throws Exception {

        NettierClient client = Nettier.createClient(gson, LoggerUtils.simpleLogger("TestClient"));

        CompletableFuture<Double> future = new CompletableFuture<>();

        client.setHandshakeHandler(remote -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) { }
            System.out.println("hello");
            client.send(new InverseRequest(PI)).awaitFuture(Double.class).thenAccept(future::complete);
        });

        client.connect("127.0.0.1", TEST_PORT);
        client.waitUntilReady();

        Thread.sleep(100);
        System.out.println(((NettierServerImpl) server).getClients().size() + " clients connected");

        assertEquals(1.0 / PI, future.get(10, TimeUnit.SECONDS));

        client.close();

    }

    @Test
    public void testMultipleClients() throws Exception {

        List<NettierClient> clients = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            NettierClient client = Nettier.createClient(gson, LoggerUtils.simpleLogger("Client#" + (i + 1)));

            client.connect("127.0.0.1", TEST_PORT);

            clients.add(client);
        }

        for (NettierClient nettierClient : clients) {
            nettierClient.waitUntilReady();
        }

        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (NettierClient nettierClient : clients) {
            futures.add(nettierClient.send(new InverseRequest(PI)).awaitFuture(Double.class)
                    .thenAccept(d -> assertEquals(1.0 / PI, d)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(1, TimeUnit.SECONDS);


    }



}
