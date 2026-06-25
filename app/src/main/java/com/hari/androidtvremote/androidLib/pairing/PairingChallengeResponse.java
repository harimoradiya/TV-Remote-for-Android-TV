package com.hari.androidtvremote.androidLib.pairing;



import com.hari.androidtvremote.androidLib.exception.PairingException;
import com.hari.androidtvremote.androidLib.util.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Class to represent the out-of-band secret transmitted during pairing.
 */
public class PairingChallengeResponse {

    /**
     * Hash algorithm to generate secret.
     */
    private static final String HASH_ALGORITHM = "SHA-256";


    /**
     * Certificate of the local peer in the protocol.
     */
    private Certificate mClientCertificate;

    /**
     * Certificate of the remote peer in the protocol.
     */
    private Certificate mServerCertificate;

    public PairingChallengeResponse(Certificate clientCert, Certificate serverCert) {
        mClientCertificate = clientCert;
        mServerCertificate = serverCert;
    }

    public byte[] getAlpha(byte[] nonce) throws PairingException {
        PublicKey clientPubKey = mClientCertificate.getPublicKey();
        PublicKey serverPubKey = mServerCertificate.getPublicKey();

        logDebug("getAlpha, nonce=" + Utils.bytesToHexString(nonce));

        if (!(clientPubKey instanceof RSAPublicKey) ||
                !(serverPubKey instanceof RSAPublicKey)) {
            throw new PairingException("Only supports RSA public keys");
        }

        RSAPublicKey clientPubRsa = (RSAPublicKey) clientPubKey;
        RSAPublicKey serverPubRsa = (RSAPublicKey) serverPubKey;

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new PairingException("Could not get digest algorithm", e);
        }

        byte[] digestBytes;
        byte[] clientModulus = clientPubRsa.getModulus().abs().toByteArray();
        byte[] clientExponent =
                clientPubRsa.getPublicExponent().abs().toByteArray();
        byte[] serverModulus = serverPubRsa.getModulus().abs().toByteArray();
        byte[] serverExponent =
                serverPubRsa.getPublicExponent().abs().toByteArray();

        // Per "Polo Implementation Overview", section 6.1, leading null bytes must
        // be removed prior to hashing the key material.
        clientModulus = removeLeadingNullBytes(clientModulus);
        clientExponent = removeLeadingNullBytes(clientExponent);
        serverModulus = removeLeadingNullBytes(serverModulus);
        serverExponent = removeLeadingNullBytes(serverExponent);

        logVerbose("Hash inputs, in order: ");
        logVerbose("   client modulus: " + Utils.bytesToHexString(clientModulus));
        logVerbose("  client exponent: " + Utils.bytesToHexString(clientExponent));
        logVerbose("   server modulus: " + Utils.bytesToHexString(serverModulus));
        logVerbose("  server exponent: " + Utils.bytesToHexString(serverExponent));
        logVerbose("            nonce: " + Utils.bytesToHexString(nonce));

        // Per "Polo Implementation Overview", section 6.1, client key material is
        // hashed first, followed by the server key material, followed by the
        // nonce.
        digest.update(clientModulus);
        digest.update(clientExponent);
        digest.update(serverModulus);
        digest.update(serverExponent);
        digest.update(nonce);

        digestBytes = digest.digest();
        logDebug("Generated hash: " + Utils.bytesToHexString(digestBytes));
        return digestBytes;
    }

    public byte[] getGamma(byte[] nonce) throws PairingException {
        byte[] alphaBytes = getAlpha(nonce);
        assert(alphaBytes.length >= nonce.length);

        byte[] result = new byte[nonce.length * 2];

        System.arraycopy(alphaBytes, 0, result, 0, nonce.length);
        System.arraycopy(nonce, 0, result, nonce.length, nonce.length);

        return result;
    }

    public byte[] extractNonce(byte[] gamma) {
        if ((gamma.length < 2) || (gamma.length % 2 != 0)) {
            throw new IllegalArgumentException();
        }
        int nonceLength = gamma.length / 2;
        byte[] nonce = new byte[nonceLength];
        System.arraycopy(gamma, nonceLength, nonce, 0, nonceLength);
        return nonce;
    }

    public boolean checkGamma(byte[] gamma) throws PairingException {

        byte[] nonce;
        try {
            nonce = extractNonce(gamma);
        } catch (IllegalArgumentException e) {
            logDebug("Illegal nonce value.");
            return false;
        }
        logDebug("Nonce is: " + Utils.bytesToHexString(nonce));
        logDebug("User gamma is: " + Utils.bytesToHexString(gamma));
        logDebug("Generated gamma is: " + Utils.bytesToHexString(getGamma(nonce)));
        return Arrays.equals(gamma, getGamma(nonce));
    }

    /**
     * Strips leading null bytes from a byte array, returning a new copy.
     * <p>
     * As a special case, if the input array consists entirely of null bytes,
     * then an array with a single null element will be returned.
     */
    private byte[] removeLeadingNullBytes(byte[] inArray) {
        int offset = 0;
        while (offset < inArray.length & inArray[offset] == 0) {
            offset += 1;
        }
        byte[] result = new byte[inArray.length - offset];
        for (int i=offset; i < inArray.length; i++) {
            result[i - offset] = inArray[i];
        }
        return result;
    }

    private void logDebug(String message) {
        System.out.println(message);
    }

    private void logVerbose(String message) {
        System.out.println(message);
    }

    public static interface DebugLogger {
        /**
         * Logs debugging information from challenge-response generation.
         */
        public void debug(String message);

        /**
         * Logs verbose debugging information from challenge-response generation.
         */
        public void verbose(String message);

    }

}

