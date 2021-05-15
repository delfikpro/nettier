package dev.implario.nettier.test;

import dev.implario.nettier.NettierClient;
import dev.implario.nettier.NettierServer;
import dev.implario.nettier.Nettier;
import lombok.Data;
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

    @Test
    public void testNettier() throws Exception {

        NettierServer server = Nettier.createServer();

        server.addListener(InverseRequest.class, (talk, request) -> {
            if (request.getValue() == 0)
                talk.send(new EvalError("Unable to inverse zero."));
            else
                talk.send(1.0 / request.getValue());
        });

        server.start(49146).await();


        NettierClient client = Nettier.createClient();

        client.setForeignPacketHandler(packet -> {
            if (packet instanceof EvalError)
                throw new EvalException(((EvalError) packet).getMessage());
        });

        client.connect("127.0.0.1", 49146);

        client.waitUntilReady();

        double testValue = Math.PI;

        Double result = client.send(new InverseRequest(testValue)).await(Double.class);

        assertEquals(1.0 / testValue, result);

        assertThrows(EvalException.class, () ->
                client.send(new InverseRequest(0)).await(InverseResponse.class)
        );
    }

}
