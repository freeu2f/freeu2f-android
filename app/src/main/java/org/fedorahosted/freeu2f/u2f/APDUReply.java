package org.fedorahosted.freeu2f.u2f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class APDUReply implements Packetable {
    public enum StatusCode implements Packetable {
        NO_ERROR(0x9000),
        CONDITIONS_NOT_SATISFIED(0x6985),
        WRONG_DATA(0x6a80),
        WRONG_LENGTH(0x6700),
        CLA_NOT_SUPPORTED(0x6e00),
        INS_NOT_SUPPORTED(0x6d00),
        UNKNOWN(0x6f00);

        private final char value;

        StatusCode(int value) {
            this.value = (char) value;
        }

        @Override
        public Packet toPacket() {
            return new APDUReply(this).toPacket();
        }
    }

    private final StatusCode code;
    private final byte[] data;

    public APDUReply(StatusCode code) {
        this.code = code;
        this.data = new byte[0];
    }

    public APDUReply(StatusCode code, byte[] data) {
        this.code = code;
        this.data = data;
    }

    public Packet toPacket() {
        ByteBuffer bb = ByteBuffer.allocate(data.length + 2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(data);
        bb.putChar(code.value);
        return new Packet(PacketCommand.MSG, bb.array());
    }
}
