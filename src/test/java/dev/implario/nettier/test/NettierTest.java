package dev.implario.nettier.test;

import com.google.gson.Gson;
import dev.implario.nettier.NettierClient;
import dev.implario.nettier.NettierServer;
import dev.implario.nettier.Nettier;
import dev.implario.nettier.Talk;
import dev.implario.nettier.impl.client.NettierClientImpl;
import dev.implario.nettier.impl.server.NettierServerImpl;
import implario.LoggerUtils;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

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

    private static NettierClient client;

    @BeforeAll
    public static void prepare() throws InterruptedException {

        Gson gson = new Gson();

        NettierServer server = Nettier.createServer(gson, LoggerUtils.simpleLogger("Server"));
        client = Nettier.createClient(gson, LoggerUtils.simpleLogger("Client"));

        ((NettierServerImpl) server).setDebugReads(true);
        ((NettierServerImpl) server).setDebugWrites(true);

        server.addListener(InverseRequest.class, (talk, request) -> {
            if (request.getValue() == 0)
                talk.respond(new EvalError("Unable to inverse zero."));
            else
                talk.respond(1.0 / request.getValue());
        });

        server.start(49146).await();

        client.connect("127.0.0.1", 49146);

        ((NettierClientImpl) client).setDebugReads(true);
        ((NettierClientImpl) client).setDebugWrites(true);

        client.waitUntilReady();

    }

    @Test
    public void testSimpleCommunication() throws Exception {

        Double result = client.send(new InverseRequest(PI)).await(Double.class);

        assertEquals(1.0 / PI, result);

    }

    @Test
    public void testTranslatorErrors() throws Exception {

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



}
