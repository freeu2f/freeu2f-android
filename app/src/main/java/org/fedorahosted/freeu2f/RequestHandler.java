package org.fedorahosted.freeu2f;

import org.fedorahosted.freeu2f.u2f.APDUReply;
import org.fedorahosted.freeu2f.u2f.APDURequest;
import org.fedorahosted.freeu2f.u2f.PacketableException;

public interface RequestHandler {
    APDUReply handle(APDURequest req) throws PacketableException;
}
