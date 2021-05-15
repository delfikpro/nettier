package dev.implario.nettier.test;

import dev.implario.nettier.NettierClient;
import dev.implario.nettier.NettierServer;
import dev.implario.nettier.Nettier;
import dev.implario.nettier.impl.client.NettierClientImpl;
import dev.implario.nettier.impl.server.NettierServerImpl;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    private static final NettierClient client = Nettier.createClient();

    @BeforeAll
    public static void prepare() throws InterruptedException {

        NettierServer server = Nettier.createServer();

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

        double testValue = Math.PI;

        Double result = client.send(new InverseRequest(testValue)).await(Double.class);

        assertEquals(1.0 / testValue, result);

    }

    @Test
    public void testTranslatorErrors() throws Exception {

        client.setPacketTranslator((packet, type) -> {
            System.out.println("Translating " + packet);
            if (packet instanceof EvalError) {
                System.out.println("throwing");
                throw new EvalException(((EvalError) packet).getMessage());
            }
            return packet;
        });

        assertThrows(EvalException.class, () ->
                client.send(new InverseRequest(0)).await(InverseResponse.class)
        );
    }



}
