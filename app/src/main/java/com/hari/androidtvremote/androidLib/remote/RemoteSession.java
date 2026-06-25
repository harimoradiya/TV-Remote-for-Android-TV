package com.hari.androidtvremote.androidLib.remote;

import android.util.Log;

import com.hari.androidtvremote.androidLib.exception.PairingException;
import com.hari.androidtvremote.androidLib.ssl.DummyTrustManager;
import com.hari.androidtvremote.androidLib.ssl.KeyStoreManager;
import com.hari.androidtvremote.androidLib.wire.PacketParser;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class RemoteSession {

    private static final String TAG = "RemoteSession";


    private static final class SynchronizedOutputStream extends OutputStream {
        private final OutputStream delegate;
        SynchronizedOutputStream(OutputStream delegate) { this.delegate = delegate; }

        @Override public synchronized void write(int b) throws IOException { delegate.write(b); }
        @Override public synchronized void write(byte[] b) throws IOException { delegate.write(b); }
        @Override public synchronized void write(byte[] b, int off, int len) throws IOException { delegate.write(b, off, len); }
        @Override public synchronized void flush() throws IOException { delegate.flush(); }
        @Override public synchronized void close() throws IOException { delegate.close(); }
    }

    private final BlockingQueue<Remotemessage.RemoteMessage> mMessageQueue;
    private static RemoteMessageManager mMessageManager;

    private final String mHost;
    private final int mPort;
    private final RemoteSessionListener mRemoteSessionListener;

    // BUG FIX 3: Use AtomicBoolean so flag is thread-safe and can be reset cleanly
    // between sessions without a stale `true` blocking the next VoiceBegin.
    private final AtomicBoolean voiceSessionActive = new AtomicBoolean(false);
    private volatile int syncedVoiceSessionId = -1;

    // Central socket health flag.
    private final AtomicBoolean socketAlive = new AtomicBoolean(false);

    int retry;
    private OutputStream outputStream;  // always a SynchronizedOutputStream after connect()
    private PacketParser packetParser;

    private volatile int syncedImeCounter = -1;
    private volatile int syncedFieldCounter = -1;
    private volatile int syncedTextInt5 = -1;
    private volatile String syncedTextLabel = "";
    private volatile Remotemessage.RemoteAppInfo syncedAppInfo = null;

    public RemoteSession(String host, int port, RemoteSessionListener remoteSessionListener) {
        mMessageQueue = new LinkedBlockingQueue<>();
        mMessageManager = new RemoteMessageManager();
        mHost = host;
        mPort = port;
        mRemoteSessionListener = remoteSessionListener;
    }

    public void connect() throws GeneralSecurityException, IOException, InterruptedException, PairingException {
        try {
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(
                    new KeyStoreManager().getKeyManagers(),
                    new TrustManager[]{new DummyTrustManager()},
                    new SecureRandom()
            );
            SSLSocketFactory sslSocketFactory = sSLContext.getSocketFactory();
            SSLSocket sSLSocket = (SSLSocket) sslSocketFactory.createSocket(mHost, mPort);
            sSLSocket.setNeedClientAuth(true);
            sSLSocket.setUseClientMode(true);
            sSLSocket.setKeepAlive(true);
            sSLSocket.setTcpNoDelay(true);
            sSLSocket.startHandshake();

            outputStream = new SynchronizedOutputStream(sSLSocket.getOutputStream());

            packetParser = new RemotePacketParser(
                    sSLSocket.getInputStream(),
                    outputStream,
                    mMessageQueue,
                    new RemoteListener() {
                        @Override public void onConnected() { mRemoteSessionListener.onConnected(); }
                        @Override public void onDisconnected() {}
                        @Override public void onVolume() {}
                        @Override public void onPerformInputDeviceRole() throws PairingException {}
                        @Override public void onPerformOutputDeviceRole(byte[] gamma) throws PairingException {}
                        @Override public void onSessionEnded() {}
                        @Override public void onError(String message) {}
                        @Override public void onImeShow(Remotemessage.RemoteTextFieldStatus status) {
                            updateSyncedTextFieldStatus(status);
                        }
                        @Override public void onLog(String message) {}
                        @Override public void sSLException() {}
                    }
            );

            packetParser.start();

            waitForMessage();

            outputStream.write(mMessageManager.createRemoteConfigure(
                    622, "ROG Strix G531GT_G531GT", "ASUSTeK COMPUTER INC.", 1, "1"));
            outputStream.flush();

            waitForMessage();

            outputStream.write(mMessageManager.createRemoteActive(622));
            outputStream.flush();

            socketAlive.set(true);

            startMessageReader();



        } catch (SSLException sslException) {
            mRemoteSessionListener.onSslError();
        } catch (Exception e) {
            e.printStackTrace();
            mRemoteSessionListener.onError(e.getMessage());
        }
    }

    private void startMessageReader() {
        new Thread(() -> {
            try {
                while (socketAlive.get() && !packetParser.isInterrupted()) {
                    Remotemessage.RemoteMessage message = waitForMessage();
                    if (message == null) break;

                    if (message.hasRemoteImeShowRequest()) {
                        Remotemessage.RemoteImeShowRequest request = message.getRemoteImeShowRequest();
                        if (request.hasRemoteTextFieldStatus()) {
                            updateSyncedTextFieldStatus(request.getRemoteTextFieldStatus());
                        }
                    }

                    if (message.hasRemoteImeKeyInject()) {
                        Remotemessage.RemoteImeKeyInject imeKeyInject = message.getRemoteImeKeyInject();
                        if (imeKeyInject.hasAppInfo()) syncedAppInfo = imeKeyInject.getAppInfo();
                        if (imeKeyInject.hasTextFieldStatus()) {
                            updateSyncedTextFieldStatus(imeKeyInject.getTextFieldStatus());
                        }
                    }

                    if (message.hasRemoteImeBatchEdit()) {
                        syncedImeCounter = message.getRemoteImeBatchEdit().getImeCounter();
                        syncedFieldCounter = message.getRemoteImeBatchEdit().getFieldCounter();
                    }
                    if (message.hasRemoteSetVolumeLevel()) {
                        Remotemessage.RemoteSetVolumeLevel vol = message.getRemoteSetVolumeLevel();
                        int level    = (int) vol.getVolumeLevel();
                        int maxLevel = (int) vol.getVolumeMax();
                        boolean muted = vol.getVolumeMuted();
                        Log.i(TAG, "Volume: " + level + "/" + maxLevel + " muted=" + muted);
                        if (mVolumeListener != null) {
                            mVolumeListener.onVolumeChanged(level, maxLevel, muted);
                        }
                    }



                }
            } catch (Exception e) {
                Log.e(TAG, "Message reader error: " + e.getMessage());
            }
            Log.i(TAG, "Message reader thread exiting");
        }, "RemoteSession-Reader").start();
    }

    private void updateSyncedTextFieldStatus(Remotemessage.RemoteTextFieldStatus status) {
        if (status == null) return;
        syncedFieldCounter = status.getCounterField();
        syncedTextInt5 = status.getInt5();
        syncedTextLabel = status.getLabel();
        mRemoteSessionListener.onImeShow(status.getValue(), status.getCounterField());
    }

    Remotemessage.RemoteMessage waitForMessage() throws InterruptedException, PairingException {
        return mMessageQueue.take();
    }

    public void attemptToReconnect() {
        retry++;
        try {
            connect();
        } catch (GeneralSecurityException | IOException | InterruptedException | PairingException e) {
            mRemoteSessionListener.onError(e.getMessage());
            e.printStackTrace();
        }
    }

    public void abort() {
        socketAlive.set(false);
        voiceSessionActive.set(false);
        syncedVoiceSessionId = -1;
        if (packetParser != null) packetParser.abort();
    }

    private boolean writeBytes(byte[] data, String callerTag) {
        if (!socketAlive.get()) {
            Log.w(TAG, callerTag + " skipped: socket not alive");
            return false;
        }
        try {
            outputStream.write(data);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, callerTag + " failed: " + e.getMessage());
            socketAlive.set(false);
            return false;
        }
    }

    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        boolean ok = writeBytes(mMessageManager.createKeyCommand(remoteKeyCode, remoteDirection), "sendCommand");
        if (ok) Log.i(TAG, "sendCommand: " + remoteKeyCode + " dir=" + remoteDirection);
    }

    public void sendText(String text, int imeCounter, int fieldCounter) {
        if (text == null) text = "";
        int resolvedIme = syncedImeCounter >= 0 ? syncedImeCounter : Math.max(imeCounter, 0);
        int resolvedField = syncedFieldCounter >= 0 ? syncedFieldCounter : Math.max(fieldCounter, 0);
        if (!socketAlive.get()) { Log.w(TAG, "sendText skipped: socket not alive"); return; }
        try {
            outputStream.write(mMessageManager.createImeText(
                    text, resolvedIme, resolvedField, syncedTextInt5, syncedTextLabel, syncedAppInfo, false));
            outputStream.write(mMessageManager.createImeBatchEditCompatV2(text, resolvedIme, resolvedField));
            outputStream.flush();
            syncedImeCounter = resolvedIme;
            syncedFieldCounter = resolvedField;
        } catch (IOException e) {
            Log.e(TAG, "sendText failed: " + e.getMessage());
            socketAlive.set(false);
        }
    }

    public void sendAppCommand(String appLink) {
        writeBytes(mMessageManager.createAppCommand(appLink), "sendAppCommand");
    }

    public void sendImeEnter() {
        writeBytes(mMessageManager.createImeEnter(), "sendImeEnter");
    }

    public void sendMessage(Remotemessage.RemoteMessage message) {
        if (message == null) return;
        writeBytes(mMessageManager.createRemoteMessage(message), "sendMessage");
    }



    // ─────────────────────────────────────────────────────────────────────────────
// ADD THESE METHODS TO RemoteSession.java
//
// Also add this field near the existing voiceSessionActive / syncedVoiceSessionId
// fields (they are already declared in your file):
//
//   private final java.util.concurrent.atomic.AtomicInteger voiceSessionCounter
//       = new java.util.concurrent.atomic.AtomicInteger(0);
// ─────────────────────────────────────────────────────────────────────────────

    // Counter used to generate unique, incrementing session IDs.
    // Declared alongside the other voice fields near the top of RemoteSession.
    private final java.util.concurrent.atomic.AtomicInteger voiceSessionCounter =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /**
     * Opens a voice session on the TV (sends RemoteVoiceBegin).
     *
     * Returns the session ID that was assigned, or -1 if the socket is not alive
     * or a voice session is already active.
     *
     * Call sendVoiceChunk() repeatedly with PCM data, then stopVoice() when done.
     */
    public int startVoice() {
        if (!socketAlive.get()) {
            Log.w(TAG, "startVoice: socket not alive");
            return -1;
        }
        if (voiceSessionActive.get()) {
            Log.w(TAG, "startVoice: voice session already active (id=" + syncedVoiceSessionId + ")");
            return syncedVoiceSessionId;
        }

        // Assign a new session ID (1-based, wraps safely)
        int sessionId = voiceSessionCounter.incrementAndGet();
        syncedVoiceSessionId = sessionId;

        boolean ok = writeBytes(mMessageManager.createVoiceBegin(sessionId), "startVoice");
        if (ok) {
            voiceSessionActive.set(true);
            Log.i(TAG, "startVoice: RemoteVoiceBegin sent (sessionId=" + sessionId + ")");
        } else {
            syncedVoiceSessionId = -1;
            return -1;
        }

        return sessionId;
    }

    /**
     * Streams a chunk of raw 16-bit PCM, mono, 8 kHz audio to the TV.
     *
     * Must be called after startVoice() and before stopVoice().
     * Intended to be called from VoiceManager's ChunkCallback.
     *
     * @param pcmChunk  raw PCM bytes (~20 KB recommended, matching VoiceManager.CHUNK_SIZE_BYTES)
     */
    public void sendVoiceChunk(byte[] pcmChunk) {
        if (!voiceSessionActive.get()) {
            Log.w(TAG, "sendVoiceChunk: no active voice session — call startVoice() first");
            return;
        }
        if (pcmChunk == null || pcmChunk.length == 0) return;

        writeBytes(
                mMessageManager.createVoicePayload(syncedVoiceSessionId, pcmChunk),
                "sendVoiceChunk"
        );
    }

    /**
     * Ends the voice session (sends RemoteVoiceEnd).
     * The TV will process all received audio and perform speech recognition.
     *
     * Safe to call even if no session is active (no-op).
     */
    public void stopVoice() {
        if (!voiceSessionActive.get()) {
            Log.w(TAG, "stopVoice: no active voice session");
            return;
        }

        int sessionId = syncedVoiceSessionId;

        // Clear state before writing so abort() can't race us
        voiceSessionActive.set(false);
        syncedVoiceSessionId = -1;

        writeBytes(mMessageManager.createVoiceEnd(sessionId), "stopVoice");
        Log.i(TAG, "stopVoice: RemoteVoiceEnd sent (sessionId=" + sessionId + ")");
    }

    /**
     * Convenience: returns true if a voice session is currently streaming.
     */
    public boolean isVoiceActive() {
        return voiceSessionActive.get();
    }


    public boolean isSocketAlive() {
        return socketAlive.get();
    }

    public interface RemoteSessionListener {
        void onConnected();
        void onSslError() throws GeneralSecurityException, IOException, InterruptedException, PairingException;
        void onDisconnected();
        void onImeShow(String text, int fieldCounter);
        void onError(String message);

    }
    public interface VolumeListener {
        void onVolumeChanged(int level, int maxLevel, boolean muted);
    }

// ── Add this field near the other volatile fields ─────────────────────────────

    private volatile VolumeListener mVolumeListener;

    public void setVolumeListener(VolumeListener listener) {
        mVolumeListener = listener;
    }


}