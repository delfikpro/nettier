package dev.implario.nettier;

import lombok.Data;

@Data
public class NettierPacketFrame {

    private final String type;
    private final Object packet;
    private final long talk;

}
