package dev.implario.nettier.impl;


import dev.implario.nettier.NettierRemote;
import dev.implario.nettier.impl.NettierNodeImpl;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import lombok.RequiredArgsConstructor;

import java.util.logging.Level;

import static io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE;

@RequiredArgsConstructor
public class NettyAdapter extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final NettierNodeImpl node;
    private final NettierRemote remote;
    private final Runnable onReady;
    private final Runnable onClose;


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt == HANDSHAKE_COMPLETE) {
            onReady.run();
            if (node.getHandshakeHandler() != null) {
                node.getExecutor().accept(() -> node.getHandshakeHandler().accept(remote));
            }
        }
    }

    @Override
    public boolean acceptInboundMessage(Object msg) {
        return msg instanceof TextWebSocketFrame;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
        if (msg instanceof TextWebSocketFrame) {
            node.processWebSocketFrame(remote, (TextWebSocketFrame) msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        onClose.run();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        node.getLogger().log(Level.SEVERE, "Nettier exception", cause);
    }

}
