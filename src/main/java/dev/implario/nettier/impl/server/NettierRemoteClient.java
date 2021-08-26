package dev.implario.nettier.impl.server;

import dev.implario.nettier.NettierRemote;
import dev.implario.nettier.Talk;
import dev.implario.nettier.impl.NettierNodeImpl;
import dev.implario.nettier.impl.NettyAdapter;
import dev.implario.nettier.impl.TalkProvider;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.net.SocketAddress;

public class NettierRemoteClient implements NettierRemote {

    @Getter
    private final NettierNodeImpl node;

    @Getter
    private final Channel channel;

    @Getter
    private final NettyAdapter adapter;

    @Delegate
    private final TalkProvider talkProvider = new TalkProvider(this);

    @Setter
    private Runnable disconnectHandler;

    public NettierRemoteClient(NettierServerImpl node, Channel channel) {
        this.node = node;
        this.channel = channel;
        this.adapter = new NettyAdapter(node, this,
                () -> node.getClients().add(this),
                () -> {
                    node.getClients().remove(this);
                    disconnectHandler.run();
                }
        );
    }

    @Override
    public SocketAddress getAddress() {
        return channel.remoteAddress();
    }

    @Override
    public Talk send(Object packet) {
        return provideTalk(talkProvider.getPacketCounter().decrementAndGet()).respond(packet);
    }

    @Override
    public void write(Object packet, long talkId) {
        node.write(channel, packet, talkId);
    }

}
