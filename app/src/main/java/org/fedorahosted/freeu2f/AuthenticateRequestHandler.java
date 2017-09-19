package org.fedorahosted.freeu2f;

import org.fedorahosted.freeu2f.u2f.APDUReply;
import org.fedorahosted.freeu2f.u2f.APDURequest;
import org.fedorahosted.freeu2f.u2f.FrameableException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyStore;
import java.security.Signature;

public class AuthenticateRequestHandler implements RequestHandler {
    private static final String KEY_STORE = "AndroidKeyStore";

    public enum Control {
        CHECK_ONLY(0x07),
        ENFORCE_USER_PRESENCE_AND_SIGN(0x03),
        DONT_ENFORCE_USER_PRESENCE_AND_SIGN(0x08);

        private final byte value;

        Control(int value) {
            this.value = (byte) value;
        }

        public static Control valueOf(byte cnt)
                throws FrameableException {
            for (Control c : values()) {
                if (c.value == cnt)
                    return c;
            }

            throw new FrameableException(APDUReply.StatusCode.UNKNOWN);
        }
    }

    @Override
    public APDUReply handle(APDURequest req) throws FrameableException {
        if (req.lc.length < 65)
            throw new FrameableException(APDUReply.StatusCode.WRONG_LENGTH);

        Control cnt = Control.valueOf(req.p1);

        StringBuilder sb = new StringBuilder();
        for (int i = 32; i < req.lc.length; i++)
            sb.append(String.format("%02X", req.lc[i]));

        try {
            // Load the key handle from the key store.
            KeyStore ks = KeyStore.getInstance(KEY_STORE);
            ks.load(null);
            KeyStore.Entry e = ks.getEntry(sb.toString(), null);

            // Handle the specified command.
            byte up = 0;
            switch (cnt) {
            case CHECK_ONLY:
                return new APDUReply(APDUReply.StatusCode.CONDITIONS_NOT_SATISFIED);

            case ENFORCE_USER_PRESENCE_AND_SIGN:
                up = 1;
                break;

            case DONT_ENFORCE_USER_PRESENCE_AND_SIGN:
                break;
            }

            // Create the signature payload.
            ByteBuffer pay = ByteBuffer.allocate(69);
            pay.order(ByteOrder.BIG_ENDIAN);
            pay.put(req.lc, 32, 32);
            pay.put(up);
            pay.putInt(0);
            pay.put(req.lc, 0, 32);

            // Sign the signature payload.
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(((KeyStore.PrivateKeyEntry) e).getPrivateKey());
            s.update(pay.array());
            byte[] sig = s.sign();

            // Create the reply message.
            ByteBuffer out = ByteBuffer.allocate(5 + sig.length);
            pay.put(up);
            pay.putInt(0);
            pay.put(sig);

            return new APDUReply(APDUReply.StatusCode.NO_ERROR, pay.array());
        } catch (Exception e) {
            e.printStackTrace();
            throw new FrameableException(APDUReply.StatusCode.WRONG_DATA);
        }
    }
}
