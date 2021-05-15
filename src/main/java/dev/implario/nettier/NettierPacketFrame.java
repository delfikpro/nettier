package dev.implario.nettier;

import lombok.Data;

@Data
public class NettierPacketFrame {

    private final long talk;
    private final String type;
    private final Object packet;

}
