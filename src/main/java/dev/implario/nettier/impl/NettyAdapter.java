package dev.implario.nettier.impl;


import dev.implario.nettier.NettierRemote;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent;
import lombok.RequiredArgsConstructor;

import java.util.logging.Level;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class NettyAdapter extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final NettierNodeImpl node;
    private final NettierRemote remote;
    private final Runnable onReady;
    private final Runnable onClose;


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt == ClientHandshakeStateEvent.HANDSHAKE_COMPLETE || evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            onReady.run();
            if (node.getHandshakeHandler() != null) {
                node.getExecutor().execute(() -> node.getHandshakeHandler().accept(remote));
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
