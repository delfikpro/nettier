package dev.implario.nettier;

@FunctionalInterface
public interface PacketHandler<T> {

    void handle(Talk talk, T packet);

}
