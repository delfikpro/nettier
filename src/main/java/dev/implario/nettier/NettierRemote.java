package dev.implario.nettier;

import java.net.SocketAddress;

public interface NettierRemote {

    Talk send(Object packet);

    void write(Object packet, long talkId);

    SocketAddress getAddress();

}
