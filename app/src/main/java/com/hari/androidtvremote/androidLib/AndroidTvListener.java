package com.hari.androidtvremote.androidLib;

public interface AndroidTvListener {
    void onSessionCreated();

    void onSecretRequested();

    void onPaired();

    void onConnectingToRemote();

    void onConnected();

    void onDisconnect();

    void onImeShow(String text, int fieldCounter);

    void onError(String error);
    /**
     * TV opened its voice search popup and is ready to receive audio.
     * The ACK (RemoteVoiceBegin echo) has already been sent automatically.
     * Start recording and call AndroidRemoteTv.sendVoiceChunk(sessionId, pcm) in a loop,
     * then AndroidRemoteTv.endVoiceSession(sessionId) when done.
     *
     * @param sessionId  The session ID from the TV — pass this to sendVoiceChunk / endVoiceSession.
     */
    default void onVoiceSessionStarted(int sessionId) {}

}