package org.fedorahosted.freeu2f.u2f;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet implements Frameable {
    public enum Command {
        PING(-1),
        KEEPALIVE(-2),
        MSG(-3),
        ERROR(-4);

        private final byte value;

        Command(int value) {
            this.value = (byte) value;
        }

        public static Command valueOf(byte cmd)
                throws FrameableException {
            for (Command c : values()) {
                if (c.value == cmd)
                    return c;
            }

            throw new FrameableException(ErrorCode.INVALID_CMD);
        }
    }

    private byte sequence = 0;

    private Command command;
    private char length;
    private byte[] data;

    public Packet(Command command) {
        this.command = command;
        this.length = 0;
        this.data = new byte[0];
    }

    public Packet(Command command, byte[] data) {
        this.command = command;
        this.length = (char) data.length;
        this.data = data;
    }

    public Packet(byte[] msg) throws FrameableException {
        ByteBuffer bb = ByteBuffer.wrap(msg);
        bb.order(ByteOrder.BIG_ENDIAN);

        try {
            command = Command.valueOf(bb.get());
            length = bb.getChar();
            data = new byte[Math.min(msg.length - 3, length)];
            bb.get(data);
        } catch (BufferUnderflowException e) {
            throw new FrameableException(ErrorCode.INVALID_LEN);
        }
    }

    public Command getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isComplete() {
        return data.length == length;
    }

    public void put(byte[] msg) throws FrameableException {
        try {
            if (msg[0] != sequence++)
                throw new FrameableException(ErrorCode.INVALID_SEQ);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new FrameableException(ErrorCode.INVALID_LEN);
        }

        int rem = length - data.length;
        int len = Math.min(msg.length - 1, rem);

        ByteBuffer bb = ByteBuffer.allocate(data.length + len);
        bb.put(data);
        bb.put(msg, 1, len);
        data = bb.array();
    }

    public byte[] toBytes() {
        ByteBuffer bb = ByteBuffer.allocate(3 + data.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(command.value);
        bb.putChar(length);
        bb.put(data);
        return bb.array();
    }

    public byte[][] toFrames(int mtu) {
        int len = data.length + 3;

        int fcnt = (len + mtu - 3) / (mtu - 1);
        byte[][] out = new byte[fcnt][];

        ByteBuffer bb = ByteBuffer.allocate(Math.min(mtu, len));
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(command.value);
        bb.putChar(length);
        bb.put(data, 0, Math.min(mtu, len) - 3);
        out[0] = bb.array();

        for (byte i = 0; i < fcnt - 1; i++) {
            int off = mtu - 3 + i * (mtu - 1);
            int cnt = Math.min(mtu - 1, len - off);

            ByteBuffer b = ByteBuffer.allocate(len + 1);
            b.put(i);
            b.put(data, off, cnt);
            out[i + 1] = b.array();
        }

        return out;
    }
}
