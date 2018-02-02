package org.fedorahosted.freeu2f.u2f;

public class PacketableException extends java.lang.Exception implements Packetable {
    private final Packet packet;

    public PacketableException(Packetable packetable) {
        packet = packetable.toPacket();
    }

    @Override
    public Packet toPacket() {
        return packet;
    }
}
