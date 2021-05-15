package dev.implario.nettier;

public interface NettierRemote {

    Talk send(Object packet);

    void write(Object packet, long talkId);

}
