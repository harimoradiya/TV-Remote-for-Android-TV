package com.hari.androidtvremote.androidLib;

import android.Manifest;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.hari.androidtvremote.androidLib.exception.PairingException;
import com.hari.androidtvremote.androidLib.pairing.PairingListener;
import com.hari.androidtvremote.androidLib.pairing.PairingSession;
import com.hari.androidtvremote.androidLib.remote.RemoteSession;
import com.hari.androidtvremote.androidLib.remote.Remotemessage;
import com.hari.androidtvremote.androidLib.remote.VoiceManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class AndroidRemoteTv extends BaseAndroidRemoteTv {

    private PairingSession mPairingSession;

    private RemoteSession mRemoteSession;

    private final List<AndroidTvListener> mListeners = new ArrayList<>();

    public void addListener(AndroidTvListener listener) {
        synchronized (mListeners) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    public void removeListener(AndroidTvListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    private void notifyOnImeShow(String text, int fieldCounter) {
        synchronized (mListeners) {
            for (AndroidTvListener listener : mListeners) {
                listener.onImeShow(text, fieldCounter);
            }
        }
    }

    private void notifyOnConnected() {
        synchronized (mListeners) {
            for (AndroidTvListener listener : mListeners) {
                listener.onConnected();
            }
        }
    }

    private void notifyOnDisconnected() {
        synchronized (mListeners) {
            for (AndroidTvListener listener : mListeners) {
                listener.onDisconnect();
            }
        }
    }

    private void notifyOnError(String message) {
        synchronized (mListeners) {
            for (AndroidTvListener listener : mListeners) {
                listener.onError(message);
            }
        }
    }



    public void connect(String host, AndroidTvListener androidTvListener)
            throws GeneralSecurityException, IOException, InterruptedException, PairingException {

        if (androidTvListener != null) {
            addListener(androidTvListener);
        }

        mRemoteSession = new RemoteSession(host, 6466, new RemoteSession.RemoteSessionListener() {
            @Override
            public void onConnected() {
                notifyOnConnected();
            }

            @Override
            public void onSslError() {

            }

            @Override
            public void onDisconnected() {
                notifyOnDisconnected();
            }

            @Override
            public void onImeShow(String text, int fieldCounter) {
                notifyOnImeShow(text, fieldCounter);
            }

            @Override
            public void onError(String message) {
                notifyOnError(message);
            }

        });

        mPairingSession = new PairingSession();
        mPairingSession.pair(host, 6467, new PairingListener() {

            @Override
            public void onSessionCreated() {

            }

            @Override
            public void onPerformInputDeviceRole() throws PairingException {

            }

            @Override
            public void onPerformOutputDeviceRole(byte[] gamma) throws PairingException {

            }

            @Override
            public void onSecretRequested() {
                androidTvListener.onSecretRequested();
            }

            @Override
            public void onSessionEnded() {

            }

            @Override
            public void onError(String message) {
                androidTvListener.onError(message);
            }

            @Override
            public void onPaired() {
                try {
                    mRemoteSession.connect();
                } catch (GeneralSecurityException | IOException | InterruptedException | PairingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onLog(String message) {

            }
        });
    }

    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        Log.i("AndroidRemoteTv", "sendCommand : " + remoteKeyCode + "  " + remoteDirection);
        mRemoteSession.sendCommand(remoteKeyCode, remoteDirection);
    }

    public void sendAppLink(String appLink) {
        Log.i("AndroidRemoteTv", "appLink : " + appLink);
        mRemoteSession.sendAppCommand(appLink);
    }

    public void sendMessage(Remotemessage.RemoteMessage message) {
        if (mRemoteSession == null || message == null) {
            return;
        }
        mRemoteSession.sendMessage(message);
    }

    public void sendText(String text, int imeCounter, int fieldCounter) {
        Log.i("AndroidRemoteTv", "sendText: \"" + text + "\"");
        mRemoteSession.sendText(text, imeCounter, fieldCounter);
    }

    public void sendImeEnter() {
        mRemoteSession.sendImeEnter();
    }

    public void sendSecret(String code) {
        mPairingSession.provideSecret(code);
    }

    public void abort() {
        stopVoice();
        if (mRemoteSession != null)
            mRemoteSession.abort();
        if (mPairingSession != null)
            mPairingSession.abort();
    }

    public void reconnect(String host, AndroidTvListener androidTvListener)
            throws GeneralSecurityException, IOException, InterruptedException, PairingException {
        mRemoteSession = new RemoteSession(host, 6466, new RemoteSession.RemoteSessionListener() {
            @Override
            public void onConnected() {
                androidTvListener.onConnected();
            }

            @Override
            public void onSslError() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onImeShow(String text, int fieldCounter) {
                notifyOnImeShow(text, fieldCounter);
                if (androidTvListener != null) {
                    androidTvListener.onImeShow(text, fieldCounter);
                }
            }

            @Override
            public void onError(String message) {

            }

        });

        mRemoteSession.connect();
    }

    public void disconnect(String host, AndroidTvListener androidTvListener)
            throws GeneralSecurityException, IOException, InterruptedException, PairingException {
        mRemoteSession = new RemoteSession(host, 6466, new RemoteSession.RemoteSessionListener() {
            @Override
            public void onConnected() {

            }

            @Override
            public void onSslError() {

            }

            @Override
            public void onDisconnected() {
                androidTvListener.onDisconnect();
            }

            @Override
            public void onImeShow(String text, int fieldCounter) {
                // no-op for disconnect flow
            }

            @Override
            public void onError(String message) {

            }

        });
    }
// ─────────────────────────────────────────────────────────────────────────────
// ADD THESE MEMBERS TO AndroidRemoteTv.java
// ─────────────────────────────────────────────────────────────────────────────

    // Field — declare near the top alongside mRemoteSession / mPairingSession
    private VoiceManager mVoiceManager;

    /**
     * Start a push-to-talk voice session.
     *
     * Flow:
     *   1. Sends RemoteVoiceBegin to the TV.
     *   2. Opens the microphone and begins streaming 16-bit PCM, mono, 8 kHz chunks.
     *
     * Requires android.permission.RECORD_AUDIO.
     *
     * @return true if the session started successfully.
     */
// In AndroidRemoteTv.java — update startVoice()
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public boolean startVoice() {
        if (mRemoteSession == null || !mRemoteSession.isSocketAlive()) return false;

        // Step 1: Wake up the TV voice UI
        sendCommand(
                Remotemessage.RemoteKeyCode.KEYCODE_SEARCH,
                Remotemessage.RemoteDirection.SHORT
        );

        // Step 2: Small delay so the TV opens the search/voice overlay
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        // Step 3: Now begin the voice session
        int sessionId = mRemoteSession.startVoice();
        if (sessionId < 0) return false;

        mVoiceManager = new VoiceManager(pcmChunk ->
                mRemoteSession.sendVoiceChunk(pcmChunk)
        );

        boolean micStarted = mVoiceManager.startRecording();
        if (!micStarted) {
            mRemoteSession.stopVoice();
            mVoiceManager = null;
            return false;
        }

        return true;
    }

    /**
     * Stop the push-to-talk session.
     *
     * Flow:
     *   1. Stops microphone capture.
     *   2. Sends RemoteVoiceEnd to the TV so it processes the audio.
     */
    public void stopVoice() {
        if (mVoiceManager != null) {
            mVoiceManager.stopRecording();
            mVoiceManager = null;
        }
        if (mRemoteSession != null) {
            mRemoteSession.stopVoice();
        }
        Log.i("AndroidRemoteTv", "Voice session stopped");
    }

    /**
     * @return true while a voice session is active and streaming.
     */
    public boolean isVoiceActive() {
        return mRemoteSession != null && mRemoteSession.isVoiceActive();
    }


// ─────────────────────────────────────────────────────────────────────────────
// ALSO update abort() to clean up voice state:
//
//   public void abort() {
//       stopVoice();                  // ← add this line
//       if (mRemoteSession != null)
//           mRemoteSession.abort();
//       if (mPairingSession != null)
//           mPairingSession.abort();
//   }
// ─────────────────────────────────────────────────────────────────────────────


// ═════════════════════════════════════════════════════════════════════════════
// USAGE EXAMPLE  (e.g. in your Activity / ViewModel)
// ═════════════════════════════════════════════════════════════════════════════
//
// AndroidRemoteTv remote = ...;   // already connected
//
// // Push-to-talk button — press & hold
// voiceButton.setOnTouchListener((v, event) -> {
//     switch (event.getAction()) {
//         case MotionEvent.ACTION_DOWN:
//             if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//                     == PackageManager.PERMISSION_GRANTED) {
//                 boolean started = remote.startVoice();
//                 voiceButton.setText(started ? "🎙 Listening…" : "🎙 Error");
//             } else {
//                 ActivityCompat.requestPermissions(this,
//                         new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
//             }
//             return true;
//
//         case MotionEvent.ACTION_UP:
//         case MotionEvent.ACTION_CANCEL:
//             remote.stopVoice();
//             voiceButton.setText("🎙 Hold to speak");
//             return true;
//     }
//     return false;
// });
//
// // AndroidManifest.xml — add before <application>:
// //   <uses-permission android:name="android.permission.RECORD_AUDIO" />



    private volatile int mVolumeLevel  = -1;
    private volatile int mVolumeMax    = 15;
    private volatile boolean mVolumeMuted = false;
    private volatile VolumeChangedCallback mVolumeChangedCallback;

    public interface VolumeChangedCallback {
        void onVolumeChanged(int level, int maxLevel, boolean muted);
    }

    public void setVolumeChangedCallback(VolumeChangedCallback callback) {
        mVolumeChangedCallback = callback;

        // Wire into the session if already connected
        if (mRemoteSession != null) {
            mRemoteSession.setVolumeListener((level, maxLevel, muted) -> {
                mVolumeLevel  = level;
                mVolumeMax    = maxLevel;
                mVolumeMuted  = muted;
                if (mVolumeChangedCallback != null) {
                    mVolumeChangedCallback.onVolumeChanged(level, maxLevel, muted);
                }
            });
        }
    }

    // Getters for one-shot reads
    public int  getCurrentVolume()  { return mVolumeLevel; }
    public int  getMaxVolume()      { return mVolumeMax;   }

}
