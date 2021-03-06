package org.fedorahosted.freeu2f.u2f;

public enum ErrorCode implements Packetable {
    SUCCESS(0x00),
    INVALID_CMD(0x01),
    INVALID_PAR(0x02),
    INVALID_LEN(0x03),
    INVALID_SEQ(0x04),
    MSG_TIMEOUT(0x05),
    OTHER(0x7f);

    private final byte value;

    ErrorCode(int value) {
        this.value = (byte) value;
    }

    @Override
    public Packet toPacket() {
        byte[] body = new byte[] { value };
        return new Packet(PacketCommand.ERROR, body);
    }
}
