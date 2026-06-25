package com.hari.androidtvremote.androidLib.util;

import java.math.BigInteger;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public class Utils {
    public static final long intBigEndianBytesToLong(byte[] input) {
        assert (input.length == 4);
        long ret = (long) (input[0]) & 0xff;
        ret <<= 8;
        ret |= (long) (input[1]) & 0xff;
        ret <<= 8;
        ret |= (long) (input[2]) & 0xff;
        ret <<= 8;
        ret |= (long) (input[3]) & 0xff;
        return ret;
    }

    public static final byte[] intToBigEndianIntBytes(int intVal) {
        byte[] outBuf = new byte[4];
        outBuf[0] = (byte) ((intVal >> 24) & 0xff);
        outBuf[1] = (byte) ((intVal >> 16) & 0xff);
        outBuf[2] = (byte) ((intVal >> 8) & 0xff);
        outBuf[3] = (byte) (intVal & 0xff);
        return outBuf;
    }

    public static Certificate getPeerCert(SSLSession session) {
        try {
            // Peer certificate
            Certificate[] certs = session.getPeerCertificates();
            if (certs == null || certs.length < 1) {
                System.out.println("No Certificate");
            }
            return certs[0];
        } catch (SSLPeerUnverifiedException e) {
            System.out.println("No Certificate");
        }
        return null;
    }

    public static Certificate getLocalCert(SSLSession session) {
        Certificate[] certs = session.getLocalCertificates();
        if (certs == null || certs.length < 1) {
            System.out.println("No Certificate");
            return null;
        }
        return certs[0];
    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        BigInteger bigint = new BigInteger(1, bytes);
        int formatLen = bytes.length * 2;
        return String.format("%0" + formatLen + "x", bigint);
    }

    /**
     * Converts a string of hex characters to a byte array.
     *
     * @param hexstr the string of hex characters
     * @return a byte array representation
     */
    public static byte[] hexStringToBytes(String hexstr) {
        byte[] result = null;
        try {

            if (hexstr == null || hexstr.length() == 0 || (hexstr.length() % 2) != 0) {
                throw new IllegalArgumentException("Bad input string.");
            }

            result = new byte[hexstr.length() / 2];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) Integer.parseInt(hexstr.substring(2 * i, 2 * (i + 1)),
                        16);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
