package org.fedorahosted.freeu2f.u2f;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class APDURequest {
    public enum Class {
        U2F(0);

        private final byte value;

        Class(int value) {
            this.value = (byte) value;
        }

        public static Class valueOf(byte cmd)
                throws PacketableException {
            for (Class c : values()) {
                if (c.value == cmd)
                    return c;
            }

            throw new PacketableException(APDUReply.StatusCode.CLA_NOT_SUPPORTED);
        }
    }

    public enum Instruction {
        REGISTER(0x01),
        AUTHENTICATE(0x02),
        VERSION(0x03);

        private final byte value;

        Instruction(int value) {
            this.value = (byte) value;
        }

        public static Instruction valueOf(byte cmd)
                throws PacketableException {
            for (Instruction i : values()) {
                if (i.value == cmd)
                    return i;
            }

            throw new PacketableException(APDUReply.StatusCode.INS_NOT_SUPPORTED);
        }
    }

    public Class cla;
    public Instruction ins;
    public byte p1 = 0;
    public byte p2 = 0;
    public byte[] lc = new byte[] {};
    public char le = 0;

    public APDURequest(byte[] msg) throws PacketableException {
        ByteBuffer bb = ByteBuffer.wrap(msg);
        bb.order(ByteOrder.BIG_ENDIAN);

        try {
            cla = Class.valueOf(bb.get());
            ins = Instruction.valueOf(bb.get());
            p1 = bb.get();
            p2 = bb.get();

            if (bb.remaining() > 0) {
                char len = (char) (bb.get() & 0xff);
                if (len != 0) {
                    if (bb.remaining() == len || bb.remaining() == len + 1) {
                        lc = new byte[len];
                        bb.get(lc);

                        if (bb.remaining() == 1)
                            le = (char) (bb.get() & 0xff);
                    } else if (bb.remaining() == 0) {
                        le = len;
                    } else {
                        throw new BufferUnderflowException();
                    }
                } else {
                    len = bb.getChar();
                    if (bb.remaining() == len || bb.remaining() == len + 2) {
                        lc = new byte[len];
                        bb.get(lc);

                        if (bb.remaining() == 2)
                            le = bb.getChar();
                    } else if (bb.remaining() == 0) {
                        le = len;
                    } else {
                        throw new BufferUnderflowException();
                    }
                }
            }
        } catch (BufferUnderflowException e) {
            throw new PacketableException(ErrorCode.INVALID_PAR);
        }
    }
}
