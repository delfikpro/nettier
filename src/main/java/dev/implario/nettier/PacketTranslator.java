package dev.implario.nettier;

@FunctionalInterface
public interface PacketTranslator {

    Object translate(Object packet, Class<?> expectedType);

}
