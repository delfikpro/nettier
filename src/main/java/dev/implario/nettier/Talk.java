package dev.implario.nettier;

import dev.implario.nettier.impl.NettierNodeImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public Talk send(Object packet) {
        remote.write(packet, this.id);
        return this;
    }

    public <T> CompletableFuture<T> awaitFuture(Class<T> type) {
        return awaitFuture();
    }

    public <T> CompletableFuture<T> awaitFuture() {
        CompletableFuture<T> future = new CompletableFuture<>();
        node.getResponseCache().put(this.id, future);
        return future;
    }

    public <T> T await(Class<T> type) throws NettierException {

        CompletableFuture<T> future = awaitFuture(type);

        try {
            Object response = future.get(1, TimeUnit.SECONDS);

            if (!type.isInstance(response)) {
                node.getForeignPacketHandler().accept(response);
                throw new NettierException("Foreign packet type: " + response.getClass().getName() + " instead of " + type.getName());
            }
            return type.cast(response);
        } catch (TimeoutException ex) {
            throw new NettierException("Talk " + id + " timed out while waiting for " + type.getSimpleName(), ex);
        } catch (InterruptedException | ExecutionException ex) {
            throw new NettierException("Unknown error while waiting for " + type.getSimpleName() + " in talk " + id, ex);
        }
    }

}


