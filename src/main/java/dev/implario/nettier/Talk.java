package dev.implario.nettier;

import dev.implario.nettier.impl.NettierNodeImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
@RequiredArgsConstructor
public class Talk {

    private final long id;
    private final NettierNodeImpl node;
    private final NettierRemote remote;

    private Object lastReceivedPacket;
    private CompletableFuture<Object> future;

    public Talk respond(Object response) {
        remote.write(response, this.id);
        return this;
    }

    public void receive(Object packet) {
        this.lastReceivedPacket = packet;
        if (future != null) future.complete(packet);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> CompletableFuture<T> awaitFuture(Class<T> type) {
        CompletableFuture future = new CompletableFuture<>();
        this.future = future;

        node.getTalkCache().put(this.id, this);
        return future.thenApply(response -> {

            this.lastReceivedPacket = null;

            PacketTranslator translator = node.getPacketTranslator();
            if (translator != null) response = translator.translate(response, type);

            if (!type.isInstance(response)) {
                throw new NettierException("Packet not translated: " + response.getClass().getName() + " instead of " + type.getName());
            }

            return type.cast(response);
        });
    }

    @SneakyThrows
    public <T> T await(Class<T> type) throws NettierException {

        CompletableFuture<T> future = awaitFuture(type);
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new NettierException("Talk " + id + " timed out while waiting for " + type.getSimpleName(), ex);
        } catch (InterruptedException ex) {
            throw new NettierException("Unknown error while waiting for " + type.getSimpleName() + " in talk " + id, ex);
        } catch (ExecutionException ex) {
            throw ex.getCause() == null ? new NettierException("Unknown error in talk " + id, ex) : ex.getCause();
        }

    }


}


