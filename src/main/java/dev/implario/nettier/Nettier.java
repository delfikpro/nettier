package dev.implario.nettier;

import com.google.gson.Gson;
import dev.implario.nettier.impl.client.NettierClientImpl;
import dev.implario.nettier.impl.server.NettierServerImpl;
import lombok.experimental.UtilityClass;

import java.util.logging.Logger;

@UtilityClass
public class Nettier {

    public static NettierClient createClient() {
        return createClient(new Gson(), Logger.getLogger("Nettier"));
    }

    public static NettierClient createClient(Gson gson, Logger logger) {
        return new NettierClientImpl(gson, logger);
    }

    public static NettierServer createServer() {
        return createServer(new Gson(), Logger.getLogger("Nettier"));
    }

    public static NettierServer createServer(Gson gson, Logger logger) {
        return new NettierServerImpl(gson, logger);
    }


}
