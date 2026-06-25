package com.hari.androidtvremote.androidLib.remote;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures microphone audio as 16-bit PCM, mono, 8 kHz and streams
 * it in ~20 KB chunks — matching the format expected by the Android TV
 * RemoteVoicePayload protocol.
 *
 * Usage:
 *   VoiceManager vm = new VoiceManager(chunk -> session.sendVoiceChunk(chunk));
 *   vm.startRecording();
 *   // ... user speaks ...
 *   vm.stopRecording();
 */
public class VoiceManager {

    private static final String TAG = "VoiceManager";

    // Audio format required by the Android TV voice protocol
    public static final int SAMPLE_RATE    = 8000;              // 8 kHz
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT   = AudioFormat.ENCODING_PCM_16BIT;

    // Target chunk size: ~20 KB, matching the Python library's default
    private static final int CHUNK_SIZE_BYTES = 20 * 1024;

    private final ChunkCallback mChunkCallback;
    private final AtomicBoolean mRecording = new AtomicBoolean(false);

    private AudioRecord mAudioRecord;
    private Thread mRecordThread;

    /**
     * @param chunkCallback called on a background thread each time a chunk
     *                      of raw PCM bytes is ready to be sent to the TV.
     */
    public VoiceManager(ChunkCallback chunkCallback) {
        this.mChunkCallback = chunkCallback;
    }

    /**
     * Start capturing from the microphone and streaming chunks.
     * Returns false if AudioRecord could not be initialised (e.g. no
     * RECORD_AUDIO permission).
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public boolean startRecording() {
        if (mRecording.get()) {
            Log.w(TAG, "startRecording called while already recording");
            return false;
        }

        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBufSize == AudioRecord.ERROR || minBufSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord.getMinBufferSize() failed: " + minBufSize);
            return false;
        }

        // Use at least CHUNK_SIZE_BYTES as the internal buffer so the OS
        // never blocks our read loop.
        int bufferSize = Math.max(minBufSize, CHUNK_SIZE_BYTES);

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialise");
            mAudioRecord.release();
            mAudioRecord = null;
            return false;
        }

        mRecording.set(true);
        mAudioRecord.startRecording();

        mRecordThread = new Thread(this::recordLoop, "VoiceManager-Record");
        mRecordThread.start();

        Log.i(TAG, "Recording started (8 kHz, mono, PCM-16)");
        return true;
    }

    /**
     * Stop capturing and release the microphone.
     * Safe to call from any thread.
     */
    public void stopRecording() {
        mRecording.set(false);

        if (mAudioRecord != null) {
            try {
                mAudioRecord.stop();
            } catch (IllegalStateException ignored) {}
            mAudioRecord.release();
            mAudioRecord = null;
        }

        if (mRecordThread != null) {
            mRecordThread.interrupt();
            mRecordThread = null;
        }

        Log.i(TAG, "Recording stopped");
    }

    public boolean isRecording() {
        return mRecording.get();
    }

    // ── internal ──────────────────────────────────────────────────────────────

    private void recordLoop() {
        byte[] buffer = new byte[CHUNK_SIZE_BYTES];

        while (mRecording.get() && !Thread.currentThread().isInterrupted()) {
            int bytesRead = mAudioRecord.read(buffer, 0, buffer.length);

            if (bytesRead > 0) {
                // Copy to a fresh array so the callback owns its data
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                mChunkCallback.onChunk(chunk);
            } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION
                    || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "AudioRecord.read() error: " + bytesRead);
                break;
            }
        }

        Log.i(TAG, "Record loop exited");
    }

    // ── callback interface ────────────────────────────────────────────────────

    public interface ChunkCallback {
        /**
         * Called on the recording thread with raw 16-bit PCM, 8 kHz, mono bytes.
         * Implementations must be non-blocking (hand off to a queue if needed).
         */
        void onChunk(byte[] pcmChunk);
    }
}