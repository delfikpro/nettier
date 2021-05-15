package dev.implario.nettier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface NettierNode {

    void setHandshakeHandler(Consumer<NettierRemote> handler);

//    void setDisconnectHandler(Consumer<NettierRemote> handler);

    ComplexPacketQualifier getQualifier();

    void setExecutor(Consumer<Runnable> executor);

    void setForeignPacketHandler(Consumer<Object> handler);

    <T> void addListener(Class<T> clazz, PacketHandler<T> handler);

    <T> void removeListener(Class<T> clazz, PacketHandler<T> handler);

    boolean isActive();

    void close();

}
