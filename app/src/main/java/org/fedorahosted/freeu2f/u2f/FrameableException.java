package org.fedorahosted.freeu2f.u2f;

public class FrameableException
        extends Exception
        implements Frameable {
    private Frameable frameable;

    public FrameableException(Frameable frameable) {
        this.frameable = frameable;
    }

    @Override
    public byte[][] toFrames(int mtu) {
        return frameable.toFrames(mtu);
    }
}
