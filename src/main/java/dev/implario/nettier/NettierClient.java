package dev.implario.nettier;

import io.netty.channel.ChannelFuture;

public interface NettierClient extends NettierNode, NettierRemote {

    ChannelFuture connect(String hostname, int port);

    void waitUntilReady();

}
