package dev.implario.nettier;

import io.netty.channel.ChannelFuture;

public interface NettierServer extends NettierNode {

    void broadcast(Object packet);

    ChannelFuture start(int port);

}
