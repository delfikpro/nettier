package dev.implario.nettier;

public interface PacketQualifier {

    String getTypeForPacket(Object packet);

    Class<?> getClassForType(String type) throws ClassNotFoundException;

}
