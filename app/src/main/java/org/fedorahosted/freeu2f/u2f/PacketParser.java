package org.fedorahosted.freeu2f.u2f;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketParser {
    private ByteArrayOutputStream stream;
    private PacketCommand command;
    private byte sequence = 0;
    private char length;

    public Packet update(byte[] bytes) throws PacketableException {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        int offset = 1;

        if (stream == null) {
            command = PacketCommand.valueOf(bb.get());
            stream = new ByteArrayOutputStream();
            length = bb.getChar();
            sequence = 0;
            offset = 3;
        } else if (bb.get() != sequence++)
            throw new PacketableException(ErrorCode.INVALID_SEQ);

        stream.write(bytes, offset, bytes.length - offset);
        if (stream.size() > length)
            throw new PacketableException(ErrorCode.INVALID_LEN);
        else if (stream.size() < length)
            return null;

        try {
            return new Packet(command, stream.toByteArray());
        } finally {
            stream = null;
        }
    }
}
