package com.hari.androidtvremote.androidLib.wire;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public abstract class MessageManager {

    private final Logger logger = LoggerFactory.getLogger(MessageManager.class);
    public ByteBuffer mPacketBuffer = ByteBuffer.allocate(65539);

    /**
     * Encode a length as a varint (little-endian, 7 bits per byte, MSB=1 means "more bytes follow").
     * This matches the Android TV Remote Protocol framing spec.
     *
     * Single-byte length was the root cause of the voice "Connection reset" bug:
     * a voice payload is ~649 bytes. (byte)649 wraps to 137, so the TV received a
     * 137-byte frame followed by 512 bytes of garbage → connection reset.
     */
    private static void writeVarint(ByteBuffer buf, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                // Last (or only) byte — MSB=0
                buf.put((byte) value);
                return;
            } else {
                // More bytes to come — write 7 bits with MSB=1
                buf.put((byte) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    public byte[] addLengthAndCreate(byte[] message) {
        int length = message.length;
        Log.d("MessageManager", String.valueOf(length));
        writeVarint(mPacketBuffer, length);
        mPacketBuffer.put(message);
        byte[] buf = new byte[mPacketBuffer.position()];
        System.arraycopy(mPacketBuffer.array(), mPacketBuffer.arrayOffset(), buf, 0, mPacketBuffer.position());
        mPacketBuffer.clear();
        return buf;
    }
}
