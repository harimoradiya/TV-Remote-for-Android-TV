package com.hari.androidtvremote.androidLib.remote;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hari.androidtvremote.androidLib.wire.PacketParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class RemotePacketParser extends PacketParser {

    BlockingQueue<Remotemessage.RemoteMessage> mMessageQueue;
    private final OutputStream mOutputStream;
    private final RemoteMessageManager remoteMessageManager;
    private final RemoteListener mRemoteListener;
    private boolean isConnected = false;

    public RemotePacketParser(InputStream inputStream, OutputStream outputStream,
                              BlockingQueue<Remotemessage.RemoteMessage> messageQueue,
                              RemoteListener remoteListener) {
        super(inputStream);
        mOutputStream = outputStream;  // this is already a SynchronizedOutputStream from RemoteSession
        remoteMessageManager = new RemoteMessageManager();
        mRemoteListener = remoteListener;
        mMessageQueue = messageQueue;
    }

    @Override
    public void messageBufferReceived(byte[] buf) {
        System.out.println(Arrays.toString(buf));
        Remotemessage.RemoteMessage remoteMessage = null;
        try {
            remoteMessage = Remotemessage.RemoteMessage.parseFrom(buf);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        if (remoteMessage != null) {
            if (remoteMessage.hasRemotePingRequest()) {
                try {
                    // FIX: write AND flush atomically. The SynchronizedOutputStream ensures
                    // this write+flush cannot interleave with voice chunks or key commands
                    // being written from other threads at the same time.
                    synchronized (mOutputStream) {
                        mOutputStream.write(remoteMessageManager.createPingResponse(
                                remoteMessage.getRemotePingRequest().getVal1()));
                        mOutputStream.flush();  // FIX: was missing — caused partial bytes to
                        // be flushed later mixed with voice chunk data
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (remoteMessage.hasRemoteStart()) {
                if (!isConnected) mRemoteListener.onConnected();
                isConnected = true;
            } else {
                try {
                    mMessageQueue.put(remoteMessage);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface RemotePacketParserListener {
        void onConnected();
    }
}



//public class RemotePacketParser extends PacketParser {
//
//    BlockingQueue<Remotemessage.RemoteMessage> mMessageQueue;
//    private final OutputStream mOutputStream;
//    private final RemoteMessageManager remoteMessageManager;
//
//    private final RemoteListener mRemoteListener;
//
//    private boolean isConnected = false;
//
//    public RemotePacketParser(InputStream inputStream, OutputStream outputStream, BlockingQueue<Remotemessage.RemoteMessage> messageQueue, RemoteListener remoteListener) {
//        super(inputStream);
//        mOutputStream = outputStream;
//        remoteMessageManager = new RemoteMessageManager();
//        mRemoteListener = remoteListener;
//        mMessageQueue = messageQueue;
//    }
//
//    @Override
//    public void messageBufferReceived(byte[] buf) {
//        System.out.println(Arrays.toString(buf));
//        Remotemessage.RemoteMessage remoteMessage = null;
//        try {
//            remoteMessage = Remotemessage.RemoteMessage.parseFrom(buf);
//        } catch (InvalidProtocolBufferException e) {
//            e.printStackTrace();
//        }
//        //Send Ping Response
//        if (remoteMessage != null) {
//            if (remoteMessage.hasRemotePingRequest()) {
//                try {
//                    mOutputStream.write(remoteMessageManager.createPingResponse(remoteMessage.getRemotePingRequest().getVal1()));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else if (remoteMessage.hasRemoteStart()) {
//                if (!isConnected)
//                    mRemoteListener.onConnected();
//                isConnected = true;
//            } else {
//                try {
//                    mMessageQueue.put(remoteMessage);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
////            System.out.println(remoteMessage);
//        }
//    }
//
//
//    public interface RemotePacketParserListener {
//        void onConnected();
//    }
//
//}
