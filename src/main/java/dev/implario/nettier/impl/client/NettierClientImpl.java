package dev.implario.nettier.impl.client;

import com.google.gson.Gson;
import dev.implario.nettier.NettierClient;
import dev.implario.nettier.NettierNode;
import dev.implario.nettier.Talk;
import dev.implario.nettier.impl.NettierNodeImpl;
import dev.implario.nettier.impl.NettyAdapter;
import dev.implario.nettier.impl.TalkProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.val;

import java.net.SocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.implario.nettier.impl.NettyUtil.*;

@Getter
public class NettierClientImpl extends NettierNodeImpl implements NettierClient {

    private final Object[] readyLock = {};
    private boolean ready = false;

    private final NettyAdapter adapter = new NettyAdapter(this, this,
            () -> {
                ready = true;
                synchronized (readyLock) {
                    readyLock.notifyAll();
                }
            }, this::close);

    private String hostname;
    private int port;

    @Delegate
    private final TalkProvider talkProvider = new TalkProvider(this);

    @Getter
    @Setter
    private Channel channel;

    public NettierClientImpl(Gson gson, Logger logger) {
        super(gson, logger);
    }

    @Override
    public SocketAddress getAddress() {
        return channel.remoteAddress();
    }

    @Override
    public NettierNode getNode() {
        return this;
    }

    public void waitUntilReady() {
        synchronized (readyLock) {
            while (!ready) {
                try {
                    readyLock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public ChannelFuture connect(String hostname, int port) {

        if (isActive())
            throw new IllegalStateException("Already connected");

        this.hostname = hostname;
        this.port = port;

        return connect();

    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return CLIENT_GROUP;
    }

    public ChannelFuture connect() {

        return new Bootstrap()
                .channel(CLIENT_CHANNEL_CLASS)
                .group(getEventLoopGroup())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        val config = ch.config();
                        config.setOption(ChannelOption.IP_TOS, 24);
                        config.setAllocator(PooledByteBufAllocator.DEFAULT);
                        config.setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                        config.setOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                        ch.pipeline()
                                .addLast("codec", new HttpClientCodec())
                                .addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE))
                                .addLast("protocol_handler", new WebSocketClientProtocolHandler(
                                        WebSocketClientHandshakerFactory.newHandshaker(
                                                URI.create("ws://" + getHostname() + ":" + getPort() + "/"),
                                                WebSocketVersion.V13,
                                                null,
                                                false,
                                                new DefaultHttpHeaders(),
                                                Integer.MAX_VALUE
                                        ),
                                        true
                                ))
                                .addLast("handler_boss", adapter);
                    }
                })
                .remoteAddress(getHostname(), getPort())
                .connect().addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        getLogger().info("Connection succeeded, bound to: " + (channel = future.channel()));
                    } else {
                        getLogger().log(Level.SEVERE, "Connection failed", future.cause());
                        processAutoReconnect();
                    }
                });
    }


    @Override
    public void write(Object packet, long talkId) {
        write(channel, packet, talkId);
    }

    @Override
    public Talk send(Object packet) {
        return provideTalk(talkProvider.getPacketCounter().incrementAndGet()).respond(packet);
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isOpen();
    }


    public void processAutoReconnect() {
        if (autoReconnect) {
            logger.warning("Automatically reconnecting in next 1.5 seconds");
            schedule(this::connect, 1500L, TimeUnit.MILLISECONDS);
        }
    }

    public void close() {
        if (channel == null) return;
        logger.info("Client shutdown");
        channel.close();
        channel = null;
        ready = false;
        talkProvider.getTalkCache().invalidateAll();
        processAutoReconnect();
    }

}
