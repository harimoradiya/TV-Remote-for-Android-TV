package com.hari.androidtvremote.androidLib.wire;

import java.io.IOException;
import java.io.InputStream;

public abstract class PacketParser extends Thread {
    private final InputStream mInputStream;

    private boolean isAbort = false;

    public PacketParser(InputStream inputStream) {
        mInputStream = inputStream;
    }

    /**
     * Read a varint-encoded length from the stream.
     * Returns the decoded length, or -1 if the stream ended.
     * Matches the varint encoding in MessageManager.addLengthAndCreate().
     */
    private int readVarint() throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            int b = mInputStream.read();
            if (b < 0) return -1;  // Stream closed
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;  // MSB=0 → last byte
            shift += 7;
            if (shift >= 35) return -1;  // Malformed varint (>5 bytes)
        }
    }

    @Override
    public void run() {
        while (!isAbort) {
            try {
                int available = readVarint();

                if (available < 0) {
                    // Stream closed
                    isAbort = true;
                    break;
                }
                if (available == 0) continue;

                byte[] buf = new byte[available];
                int bytesRead = 0;
                while (bytesRead < available) {
                    int read = mInputStream.read(buf, bytesRead, available - bytesRead);
                    if (read < 0) {
                        isAbort = true;
                        break;
                    }
                    bytesRead += read;
                }

                if (!isAbort) {
                    messageBufferReceived(buf);
                }
            } catch (IOException e) {
                isAbort = true;
                e.printStackTrace();
            }
        }
    }

    public void abort() {
        isAbort = true;
    }

    public abstract void messageBufferReceived(byte[] buf);
}
