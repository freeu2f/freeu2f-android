package org.fedorahosted.freeu2f.u2f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet implements Packetable {
    private final PacketCommand command;
    private final byte[] data;

    public Packet(PacketCommand command, byte[] data) {
        this.command = command;
        this.data = data;
    }

    public PacketCommand getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public Packet toPacket() {
        return this;
    }

    public byte[][] toFrames(int mtu) {
        int nframes = (data.length + 2 + mtu - 2) / (mtu - 1);
        byte[][] out = new byte[nframes][];

        ByteBuffer bb = ByteBuffer.allocate(Math.min(mtu, data.length + 3));
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(command.toByte());
        bb.putChar((char) data.length);
        bb.put(data, 0, Math.min(mtu - 3, data.length));
        out[0] = bb.array();

        int off = out[0].length - 3;
        for (byte i = 0; i < nframes - 1; i++) {
            int cnt = Math.min(mtu - 1, data.length - off);
            ByteBuffer b = ByteBuffer.allocate(cnt + 1);
            b.put(i);
            b.put(data, off, cnt);
            out[i + 1] = b.array();
            off += cnt;
        }

        return out;
    }
}
