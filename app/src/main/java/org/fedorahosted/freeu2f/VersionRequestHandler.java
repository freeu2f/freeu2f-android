package org.fedorahosted.freeu2f;

import org.fedorahosted.freeu2f.u2f.APDUReply;
import org.fedorahosted.freeu2f.u2f.APDURequest;
import org.fedorahosted.freeu2f.u2f.PacketableException;

import java.nio.charset.Charset;

public class VersionRequestHandler implements RequestHandler {
    private static final Charset CHARSET = Charset.forName("US-ASCII");
    private static final String STRING = "U2F_V2";
    private static final byte[] BYTES = STRING.getBytes(CHARSET);

    @Override
    public APDUReply handle(APDURequest req) throws PacketableException {
        return new APDUReply(APDUReply.StatusCode.NO_ERROR, BYTES);
    }
}
