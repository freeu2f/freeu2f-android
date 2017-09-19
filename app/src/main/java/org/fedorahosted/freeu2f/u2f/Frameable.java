package org.fedorahosted.freeu2f.u2f;

public interface Frameable {
    byte[][] toFrames(int mtu);
}
