package dev.implario.nettier.impl.server;

import com.google.gson.Gson;
import dev.implario.nettier.NettierRemote;
import dev.implario.nettier.NettierServer;
import dev.implario.nettier.impl.NettierNodeImpl;
import dev.implario.nettier.impl.NettyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class NettierServerImpl extends NettierNodeImpl implements NettierServer {

    @Getter
    private final List<NettierRemote> clients = new ArrayList<>();
    private ChannelFuture context;

    public NettierServerImpl(Gson gson, Logger logger) {
        super(gson, logger);
    }

    @Override
    public void broadcast(Object packet) {

        for (NettierRemote client : clients) {
            client.write(packet, 0);
        }

    }

    @Override
    public ChannelFuture start(int port) {

        return context = new ServerBootstrap()
                .group(NettyUtil.SERVER_GROUP)
                .channel(NettyUtil.SERVER_CHANNEL_CLASS)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        NettierRemoteClient remote = new NettierRemoteClient(NettierServerImpl.this, channel);
                        channel.pipeline().addLast(
                                new HttpServerCodec(),
                                new HttpObjectAggregator(Integer.MAX_VALUE),
//                                new HttpResponseEncoder(),
                                new WebSocketServerProtocolHandler("/"),
                                remote.getAdapter()
                        );
                    }
                })
                .localAddress(port)
                .bind();

    }

    @Override
    public boolean isActive() {
        return context != null;
    }

    @Override
    public void setHandshakeHandler(Consumer<NettierRemote> handshakeHandler) {
        super.setHandshakeHandler(remote -> {
            clients.add(remote);
            if (handshakeHandler != null) handshakeHandler.accept(remote);
        });
    }

    @Override
    public void close() {

        if (isActive()) {
            context.channel().close();
        }


    }
}
