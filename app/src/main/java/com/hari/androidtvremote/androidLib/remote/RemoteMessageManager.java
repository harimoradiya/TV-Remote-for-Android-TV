package com.hari.androidtvremote.androidLib.remote;

import com.google.protobuf.CodedOutputStream;
import com.hari.androidtvremote.androidLib.wire.MessageManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RemoteMessageManager extends MessageManager {

    public byte[] createRemoteConfigure(int code, String model, String vendor, int unknown1, String unknown2) {
        Remotemessage.RemoteConfigure remoteConfigure = Remotemessage.RemoteConfigure.newBuilder()
                .setCode1(code)
                .setDeviceInfo(Remotemessage.RemoteDeviceInfo.newBuilder()
                        .setModel(model)
                        .setVendor(vendor)
                        .setUnknown1(unknown1)
                        .setUnknown2(unknown2)
                        .setPackageName("androidtv-remote")
                        .setAppVersion("1.0.0")
                        .build())
                .build();

        return createRemoteConfigure(remoteConfigure);
    }

    public byte[] createRemoteConfigure(Remotemessage.RemoteConfigure remoteConfigure) {

        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteConfigure(remoteConfigure)
                .build();
        byte[] pairingMessageByteArray = remoteMessage.toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }

    public byte[] createRemoteMessage(Remotemessage.RemoteMessage remoteMessage) {
        return addLengthAndCreate(remoteMessage.toByteArray());
    }

    public byte[] createRemoteActive(int code) {

        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteSetActive(Remotemessage.RemoteSetActive.newBuilder().setActive(code).build())
                .build();
        byte[] pairingMessageByteArray = remoteMessage.toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }

    public byte[] createPingResponse(int val1) {
        Remotemessage.RemotePingResponse remotePingResponse = Remotemessage.RemotePingResponse.newBuilder()
                .setVal1(val1).build();
        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemotePingResponse(remotePingResponse).build();
        byte[] pairingMessageByteArray = remoteMessage.toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }

    public byte[] createPower() {
        Remotemessage.RemoteKeyInject remoteKeyInject = Remotemessage.RemoteKeyInject.newBuilder()
                .setDirection(Remotemessage.RemoteDirection.SHORT)
                .setKeyCode(Remotemessage.RemoteKeyCode.KEYCODE_POWER)
                .build();
        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteKeyInject(remoteKeyInject).build();
        return addLengthAndCreate(remoteMessage.toByteArray());
    }

    public byte[] createVolumeLevel(int volume) {
        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteAdjustVolumeLevel(Remotemessage.RemoteAdjustVolumeLevel.newBuilder().build())
                .build();
        return addLengthAndCreate(remoteMessage.toByteArray());
    }

    public byte[] createKeyCommand(Remotemessage.RemoteKeyCode keyCode,
                                   Remotemessage.RemoteDirection remoteDirection) {
        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteKeyInject(Remotemessage.RemoteKeyInject.newBuilder()
                        .setKeyCode(keyCode)
                        .setDirection(remoteDirection)
                        .build())
                .build();
        return addLengthAndCreate(remoteMessage.toByteArray());
    }

    public byte[] createAppCommand(String appLink) {
        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteAppLinkLaunchRequest(Remotemessage.RemoteAppLinkLaunchRequest.newBuilder()
                        .setAppLink(appLink)
                        .build())
                .build();

        return addLengthAndCreate(remoteMessage.toByteArray());
    }

    public byte[] createVoiceAssistCommand() {
        Remotemessage.RemoteKeyInject remoteKeyInject = Remotemessage.RemoteKeyInject.newBuilder()
                .setDirection(Remotemessage.RemoteDirection.SHORT)
                .setKeyCode(Remotemessage.RemoteKeyCode.KEYCODE_VOICE_ASSIST)
                .build();

        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteKeyInject(remoteKeyInject)
                .build();

        byte[] messageBytes = remoteMessage.toByteArray();
        return addLengthAndCreate(messageBytes);
    }

    private Remotemessage.RemoteTextFieldStatus createImeTextFieldStatus(
            String text,
            int fieldCounter,
            int int5,
            String label) {
        int cursor = text.length();
        Remotemessage.RemoteTextFieldStatus.Builder statusBuilder = Remotemessage.RemoteTextFieldStatus
                .newBuilder()
                .setCounterField(fieldCounter)
                .setValue(text)
                .setStart(cursor)
                .setEnd(cursor);

        if (int5 >= 0) {
            statusBuilder.setInt5(int5);
        }
        if (label != null && !label.isEmpty()) {
            statusBuilder.setLabel(label);
        }
        return statusBuilder.build();
    }

    private Remotemessage.RemoteImeBatchEdit createImeBatchEdit(int imeCounter, int fieldCounter) {
        Remotemessage.RemoteEditInfo editInfo = Remotemessage.RemoteEditInfo.newBuilder()
                .setInsert(1)
                .build();
        return Remotemessage.RemoteImeBatchEdit.newBuilder()
                .setImeCounter(imeCounter)
                .setFieldCounter(fieldCounter)
                .addEditInfo(editInfo)
                .build();
    }

    public byte[] createImeText(String text, int imeCounter, int fieldCounter) {
        return createImeText(text, imeCounter, fieldCounter, -1, null, null, true);
    }

    public byte[] createImeText(
            String text,
            int imeCounter,
            int fieldCounter,
            int int5,
            String label,
            Remotemessage.RemoteAppInfo appInfo,
            boolean includeBatchEdit) {
        if (text == null) {
            text = "";
        }

        Remotemessage.RemoteTextFieldStatus textFieldStatus = createImeTextFieldStatus(
                text,
                fieldCounter,
                int5,
                label);

        Remotemessage.RemoteImeKeyInject.Builder imeKeyInjectBuilder = Remotemessage.RemoteImeKeyInject
                .newBuilder()
                .setTextFieldStatus(textFieldStatus);

        if (appInfo != null) {
            imeKeyInjectBuilder.setAppInfo(appInfo);
        }

        Remotemessage.RemoteMessage.Builder remoteMessageBuilder = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteImeKeyInject(imeKeyInjectBuilder.build());

        if (includeBatchEdit) {
            remoteMessageBuilder.setRemoteImeBatchEdit(createImeBatchEdit(imeCounter, fieldCounter));
        }

        return addLengthAndCreate(remoteMessageBuilder.build().toByteArray());
    }

    public byte[] createImeBatchEditCompatV2(String text, int imeCounter, int fieldCounter) {
        final String safeText = text == null ? "" : text;
        int cursor = safeText.length();

        try {
            byte[] imeObject = writeMessage(cos -> {
                cos.writeInt32(1, cursor);
                cos.writeInt32(2, cursor);
                cos.writeString(3, safeText);
            });

            byte[] editInfo = writeMessage(cos -> {
                cos.writeInt32(1, 1);
                cos.writeByteArray(2, imeObject);
            });

            byte[] batchEdit = writeMessage(cos -> {
                cos.writeInt32(1, imeCounter);
                cos.writeInt32(2, fieldCounter);
                cos.writeByteArray(3, editInfo);
            });

            byte[] remoteMessage = writeMessage(cos -> cos.writeByteArray(21, batchEdit));
            return addLengthAndCreate(remoteMessage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode IME batch edit payload", e);
        }
    }

    private interface ProtoWriter {
        void write(CodedOutputStream cos) throws IOException;
    }

    private byte[] writeMessage(ProtoWriter writer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        writer.write(cos);
        cos.flush();
        return baos.toByteArray();
    }

    public byte[] createImeEnter() {
        Remotemessage.RemoteKeyInject remoteKeyInject = Remotemessage.RemoteKeyInject.newBuilder()
                .setDirection(Remotemessage.RemoteDirection.SHORT)
                .setKeyCode(Remotemessage.RemoteKeyCode.KEYCODE_ENTER)
                .build();
        Remotemessage.RemoteMessage remoteMessage = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteKeyInject(remoteKeyInject)
                .build();
        return addLengthAndCreate(remoteMessage.toByteArray());
    }

// ─────────────────────────────────────────────────────────────────────────────
// ADD THESE THREE METHODS TO RemoteMessageManager.java
// ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sends RemoteVoiceBegin — tells the TV to open a voice recognition session.
     *
     * @param sessionId  any positive integer; must be the same across Begin / Payload / End
     */
    public byte[] createVoiceBegin(int sessionId) {
        Remotemessage.RemoteVoiceBegin voiceBegin = Remotemessage.RemoteVoiceBegin.newBuilder()
                .setSessionId(sessionId)
                // package_name is optional when sending audio directly (per proto comment)
                .build();

        Remotemessage.RemoteMessage message = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteVoiceBegin(voiceBegin)
                .build();

        return addLengthAndCreate(message.toByteArray());
    }

    /**
     * Wraps a raw PCM chunk in a RemoteVoicePayload message.
     * Audio must be 16-bit PCM, mono, 8 kHz — matching VoiceManager's output.
     *
     * @param sessionId  must match the id used in createVoiceBegin
     * @param pcmChunk   raw PCM bytes (up to ~20 KB per call)
     */
    public byte[] createVoicePayload(int sessionId, byte[] pcmChunk) {
        Remotemessage.RemoteVoicePayload payload = Remotemessage.RemoteVoicePayload.newBuilder()
                .setSessionId(sessionId)
                .setSamples(com.google.protobuf.ByteString.copyFrom(pcmChunk))
                .build();

        Remotemessage.RemoteMessage message = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteVoicePayload(payload)
                .build();

        return addLengthAndCreate(message.toByteArray());
    }

    /**
     * Sends RemoteVoiceEnd — signals the TV that audio streaming is complete
     * and it should process / recognise the speech.
     *
     * @param sessionId  must match the id used in createVoiceBegin
     */
    public byte[] createVoiceEnd(int sessionId) {
        Remotemessage.RemoteVoiceEnd voiceEnd = Remotemessage.RemoteVoiceEnd.newBuilder()
                .setSessionId(sessionId)
                .build();

        Remotemessage.RemoteMessage message = Remotemessage.RemoteMessage.newBuilder()
                .setRemoteVoiceEnd(voiceEnd)
                .build();

        return addLengthAndCreate(message.toByteArray());
    }

}