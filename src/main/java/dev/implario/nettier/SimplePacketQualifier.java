package dev.implario.nettier;

public class SimplePacketQualifier implements PacketQualifier {

    @Override
    public String getTypeForPacket(Object packet) {
        return packet.getClass().getName();
    }

    @Override
    public Class<?> getClassForType(String type) throws ClassNotFoundException {
        return Class.forName(type);
    }

}
