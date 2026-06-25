package com.hari.androidtvremote.androidLib.pairing;


import com.google.protobuf.ByteString;
import com.hari.androidtvremote.androidLib.wire.MessageManager;

public class PairingMessageManager extends MessageManager {
    public byte[] createPairingMessage(String clientName, String serviceName) {
        Pairingmessage.PairingMessage.Builder pairingMessage = Pairingmessage.PairingMessage.newBuilder()
                .setPairingRequest(Pairingmessage.PairingRequest.newBuilder()
                        .setClientName(clientName)
                        .setServiceName(serviceName))
                .setStatus(Pairingmessage.PairingMessage.Status.STATUS_OK)
                .setProtocolVersion(2);
        byte[] pairingMessageByteArray = pairingMessage.build().toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }

    public byte[] createPairingOption() {
        Pairingmessage.PairingEncoding pairingEncoding = Pairingmessage.PairingEncoding.newBuilder()
                .setType(Pairingmessage.PairingEncoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
                .setSymbolLength(6).build();

        Pairingmessage.PairingMessage.Builder pairingOption = Pairingmessage.PairingMessage.newBuilder()
                .setPairingOption(Pairingmessage.PairingOption.newBuilder()
                        .setPreferredRole(Pairingmessage.RoleType.ROLE_TYPE_INPUT)
                        .addInputEncodings(pairingEncoding))
                .setStatus(Pairingmessage.PairingMessage.Status.STATUS_OK)
                .setProtocolVersion(2);

        byte[] pairingMessageByteArray = pairingOption.build().toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }


    public byte[] createConfigMessage() {
        Pairingmessage.PairingEncoding pairingEncoding = Pairingmessage.PairingEncoding.newBuilder()
                .setType(Pairingmessage.PairingEncoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
                .setSymbolLength(6).build();

        Pairingmessage.PairingMessage.Builder pairingConfig = Pairingmessage.PairingMessage.newBuilder()
                .setPairingConfiguration(Pairingmessage.PairingConfiguration.newBuilder()
                        .setClientRole(Pairingmessage.RoleType.ROLE_TYPE_INPUT)
                        .setEncoding(pairingEncoding).build())
                .setStatus(Pairingmessage.PairingMessage.Status.STATUS_OK)
                .setProtocolVersion(2);

        byte[] pairingMessageByteArray = pairingConfig.build().toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }

    public byte[] createSecretMessage(Pairingmessage.PairingMessage pairingSecretMessage) {
        byte[] pairingMessageByteArray = pairingSecretMessage.toByteArray();
        return addLengthAndCreate(pairingMessageByteArray);
    }

    public Pairingmessage.PairingMessage createSecretMessageProto(byte[] secret) {

        return Pairingmessage.PairingMessage.newBuilder()
                .setPairingSecret(Pairingmessage.PairingSecret.newBuilder().setSecret(ByteString.copyFrom(secret)).build())
                .setStatus(Pairingmessage.PairingMessage.Status.STATUS_OK)
                .setProtocolVersion(2).build();

    }


}
