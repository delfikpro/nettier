package dev.implario.nettier.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.implario.nettier.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
@RequiredArgsConstructor
@ChannelHandler.Sharable
@SuppressWarnings ({"rawtypes", "unchecked"})
public abstract class NettierNodeImpl implements NettierNode {

    protected final Gson gson;

    protected final ComplexPacketQualifier qualifier = new ComplexPacketQualifier();

    protected final List<PacketHandler> middleware = new ArrayList<>();

    protected final Logger logger;

    @Getter
    protected final Cache<Long, CompletableFuture> responseCache = CacheBuilder.newBuilder()
            .concurrencyLevel(3)
            .expireAfterWrite(50L, TimeUnit.SECONDS)
            .<Long, CompletableFuture>removalListener(notification -> {
                logger.warning("Removed " + notification.getKey() + " talk from responseCache");
                if (notification.getCause() == RemovalCause.EXPIRED) {
                    val callback = notification.getValue();
                    if (!callback.isDone()) {
                        callback.completeExceptionally(new TimeoutException("Packet " + notification.getKey() + " timed out"));
                    }
                }
            })
            .build();

    private final Multimap<Class<?>, PacketHandler<?>> listenerMap = HashMultimap.create();


    protected final AtomicLong packetCounter = new AtomicLong();

    @Setter
    protected boolean autoReconnect = true;

    @Setter
    private Consumer<NettierRemote> handshakeHandler;

    @Setter
    private Consumer<Object> foreignPacketHandler;

    @Setter
    private Consumer<Runnable> executor = Runnable::run;

    @Setter
    private boolean debugReads, debugWrites;

    @Override
    public <T> void addListener(@NonNull Class<T> clazz, @NonNull PacketHandler<T> handler) {
        listenerMap.put(clazz, handler);
    }

    @Override
    public <T> void removeListener(@NonNull Class<T> clazz, @NonNull PacketHandler<T> handler) {
        listenerMap.remove(clazz, handler);
    }

    public void write(Channel channel, Object packet, long talkId) {
        if (channel == null)
            throw new NettierException("Channel is not initialized yet!");
        Runnable command = () -> channel.writeAndFlush(toWebSocketFrame(toNettierFrame(packet, talkId)), channel.voidPromise());
        val eventLoop = channel.eventLoop();
        if (eventLoop.inEventLoop())
            command.run();
        else
            eventLoop.execute(command);
    }

    public void processWebSocketFrame(NettierRemote remote, TextWebSocketFrame webSocketFrame) {
        val text = webSocketFrame.text();
        if (debugReads) {
            logger.warning("IN » " + text);
        }

        val json = gson.fromJson(text, JsonObject.class);

        String type = json.getAsJsonPrimitive("type").getAsString();
        long talkId = json.getAsJsonPrimitive("talk").getAsLong();
        JsonElement packetJson = json.get("packet");

        Class<?> clazz = qualifier.getClassForType(type);

        if (clazz == null) {
            logger.warning("Unable to resolve class for '" + type + "' packet type");
            return;
        }

        Object packet;

        try {
            packet = gson.fromJson(packetJson, clazz);
        } catch (RuntimeException ex) {
            throw new NettierException("Error while instantiating packet " + type, ex);
        }

        handlePacket(remote, talkId, packet);
    }

    private void handlePacket(NettierRemote remote, long talkId, Object packet) {

        CompletableFuture callback = talkId == 0 ? null : responseCache.getIfPresent(talkId);

        logger.warning("Callback for talk " + talkId + " is " + callback);

        val listeners = listenerMap.get(packet.getClass());

        if (callback == null && listeners.isEmpty())
            return;

        Talk talk = new Talk(talkId, this, remote);

        executor.accept(() -> {

            for (PacketHandler handler : middleware) {
                handler.handle(talk, packet);
            }

            listeners.forEach(listener -> {
                try {
                    ((PacketHandler) listener).handle(talk, packet);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Exception while handling " + packet.getClass().getSimpleName(), ex);
                }
            });

            if (callback != null && !callback.isDone())
                callback.complete(packet);
        });

    }


    public NettierPacketFrame toNettierFrame(Object packet, long talkId) {

        String type = qualifier.getTypeForPacket(packet);
        if (type == null)
            throw new NettierException("Unable to qualify " + packet.getClass().getName());

        return new NettierPacketFrame(type, packet, talkId);
    }

    public WebSocketFrame toWebSocketFrame(NettierPacketFrame nettierFrame) {
        val json = gson.toJson(nettierFrame);
        if (debugWrites) {
            logger.warning("OUT » " + json);
        }
        return new TextWebSocketFrame(json);
    }


}
