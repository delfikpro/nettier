package dev.implario.nettier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class ComplexPacketQualifier implements PacketQualifier {

    @Getter
    private final List<PacketQualifier> qualifiers = new ArrayList<>();

    {
        qualifiers.add(new SimplePacketQualifier());
    }

    @Override
    public String getTypeForPacket(Object packet) {
        for (PacketQualifier qualifier : qualifiers) {
            String type = qualifier.getTypeForPacket(packet);
            if (type != null) return type;
        }
        return null;
    }

    @Override
    public Class<?> getClassForType(String type) {
        for (PacketQualifier qualifier : qualifiers) {
            try {
                return qualifier.getClassForType(type);
            } catch (ClassNotFoundException ignored) { }
        }
        return null;
    }
}
