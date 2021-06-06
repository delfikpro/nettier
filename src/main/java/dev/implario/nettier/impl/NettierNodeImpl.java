package dev.implario.nettier.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.implario.nettier.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
@RequiredArgsConstructor
@ChannelHandler.Sharable
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class NettierNodeImpl implements NettierNode {

    protected final Gson gson;

    protected final ComplexPacketQualifier qualifier = new ComplexPacketQualifier();

    protected final List<PacketHandler> middleware = new ArrayList<>();

    protected final Logger logger;

    private final Multimap<Class<?>, PacketHandler<?>> listenerMap = HashMultimap.create();

    @Setter
    protected boolean autoReconnect = true;

    @Setter
    private Consumer<NettierRemote> handshakeHandler;

    @Setter
    private PacketTranslator packetTranslator;

    @Setter
    private Consumer<Runnable> executor = r -> getEventLoopGroup().execute(r);

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

    public abstract EventLoopGroup getEventLoopGroup();

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

    @SneakyThrows
    private void handlePacket(NettierRemote remote, long talkId, Object packet) {

        Talk talk = remote.provideTalk(talkId);

        val listeners = listenerMap.get(packet.getClass());

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

            talk.receive(packet);

        });

    }


    public NettierPacketFrame toNettierFrame(Object packet, long talkId) {

        String type = qualifier.getTypeForPacket(packet);
        if (type == null)
            throw new NettierException("Unable to qualify " + packet.getClass().getName());

        return new NettierPacketFrame(talkId, type, packet);
    }

    public WebSocketFrame toWebSocketFrame(NettierPacketFrame nettierFrame) {
        val json = gson.toJson(nettierFrame);
        if (debugWrites) {
            logger.warning("OUT » " + json);
        }
        return new TextWebSocketFrame(json);
    }



}
