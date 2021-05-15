package dev.implario.nettier.impl.server;

import dev.implario.nettier.NettierRemote;
import dev.implario.nettier.Talk;
import dev.implario.nettier.impl.NettierNodeImpl;
import dev.implario.nettier.impl.NettyAdapter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Getter;

public class NettierRemoteClient implements NettierRemote {

    @Getter
    private final NettierNodeImpl node;

    @Getter
    private final Channel channel;

    @Getter
    private final NettyAdapter adapter;

    public NettierRemoteClient(NettierServerImpl node, Channel channel) {
        this.node = node;
        this.channel = channel;
        this.adapter = new NettyAdapter(node, this,
                () -> node.getClients().add(this),
                () -> node.getClients().remove(this)
        );
    }

    @Override
    public Talk send(Object packet) {
        return new Talk(node.getPacketCounter().decrementAndGet(), node, this)
                .send(packet);
    }

    @Override
    public void write(Object packet, long talkId) {
        node.write(channel, packet, talkId);
    }

}
