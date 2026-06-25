package com.hari.androidtvremote.androidLib.pairing;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hari.androidtvremote.androidLib.wire.PacketParser;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

public class PairingPacketParser extends PacketParser {

    private BlockingQueue<Pairingmessage.PairingMessage> mMessagesQueue;

    public PairingPacketParser(InputStream inputStream, BlockingQueue<Pairingmessage.PairingMessage> messagesQueue) {
        super(inputStream);
        mMessagesQueue = messagesQueue;
    }

    @Override
    public void messageBufferReceived(byte[] buf) {
        try {
            Pairingmessage.PairingMessage pairingMessage = Pairingmessage.PairingMessage.parseFrom(buf);
            // Queue all messages, including errors, so they can be properly handled
            mMessagesQueue.put(pairingMessage);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

}
