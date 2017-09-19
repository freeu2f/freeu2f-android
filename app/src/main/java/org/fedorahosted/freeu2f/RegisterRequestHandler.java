package org.fedorahosted.freeu2f;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Pair;

import org.fedorahosted.freeu2f.u2f.APDUReply;
import org.fedorahosted.freeu2f.u2f.APDURequest;
import org.fedorahosted.freeu2f.u2f.FrameableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;

public class RegisterRequestHandler implements RequestHandler {
    private static final String KEY_STORE = "AndroidKeyStore";

    private Pair<KeyPair, Certificate> generateKey(String id)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, KeyStoreException,
            IOException, CertificateException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec
                .Builder(id, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationValidityDurationSeconds(60)
                .setUserAuthenticationValidWhileOnBody(true)
                .setInvalidatedByBiometricEnrollment(true)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(new byte[] {})
                .setUserAuthenticationRequired(false)
                //.setCertificateSubject(null)
                .build();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", KEY_STORE);
        kpg.initialize(spec);
        KeyPair kp =  kpg.generateKeyPair();

        KeyStore ks = KeyStore.getInstance(KEY_STORE);
        ks.load(null);
        Certificate crt = ks.getCertificate(id);

        return new Pair<>(kp, crt);
    }

    @Override
    public APDUReply handle(APDURequest req) throws FrameableException {
        if (req.lc.length != 64)
            throw new FrameableException(APDUReply.StatusCode.WRONG_LENGTH);

        try {
            // Generate the new key pair handle.
            SecureRandom sr = new SecureRandom();
            byte[] hnd = new byte[32];
            sr.nextBytes(hnd);

            // Convert the key pair handle and application param to hex ID.
            StringBuilder sb = new StringBuilder();
            for (int i = 32; i < 64; i++)
                sb.append(String.format("%02X", req.lc[i]));
            sb.append(String.format("%02X", (byte) hnd.length));
            for (byte b : hnd)
                sb.append(String.format("%02X", b));
            String id = sb.toString();

            // Generate the new signing key.
            Pair<KeyPair, Certificate> key = generateKey(id);

            // Create the signature payload
            ByteArrayOutputStream pay = new ByteArrayOutputStream();
            pay.write(new byte[] { 0 });
            pay.write(req.lc, 32, 32);
            pay.write(req.lc, 0, 32);
            pay.write(hnd);
            pay.write(key.first.getPublic().getEncoded());

            // Sign the payload
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(key.first.getPrivate(), sr);
            s.update(pay.toByteArray());

            // Create the output structure
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[] { 0x05 });
            out.write(key.first.getPublic().getEncoded());
            out.write((byte) hnd.length);
            out.write(hnd);
            out.write(key.second.getEncoded());
            out.write(s.sign());

            return new APDUReply(
                APDUReply.StatusCode.NO_ERROR,
                out.toByteArray()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new FrameableException(APDUReply.StatusCode.UNKNOWN);
        }
    }
}
