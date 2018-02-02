package org.fedorahosted.freeu2f.u2f;

public enum PacketCommand {
    PING(0x81),
    KEEPALIVE(0x82),
    MSG(0x83),
    ERROR(0xBF);

    private final byte value;

    PacketCommand(int value) {
        this.value = (byte) value;
    }

    public static PacketCommand valueOf(byte cmd) throws PacketableException {
        for (PacketCommand c : values()) {
            if (c.value == cmd)
                return c;
        }

        throw new PacketableException(ErrorCode.INVALID_CMD);
    }

    public byte toByte() {
        return value;
    }
}
