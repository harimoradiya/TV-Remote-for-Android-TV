package com.amazon.whisperlink.port.android.transport;

import android.bluetooth.BluetoothSocket;
import com.amazon.whisperlink.jmdns.impl.constants.DNSConstants;
import com.amazon.whisperlink.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransportException;

/* loaded from: WhisperPlay.jar:com/amazon/whisperlink/port/android/transport/TBtSocket.class */
public class TBtSocket extends TIOStreamTransport {
    private static final String TAG = "TBtSocket";
    protected static final int READ_BUFFER_SIZE = 1024;
    protected static final int WRITE_BUFFER_SIZE = 1024;
    protected BluetoothSocket socket;

    public TBtSocket(BluetoothSocket socket) throws TTransportException {
        this.socket = socket;
        initStreams();
    }

    protected void initStreams() throws TTransportException {
    }

    @Override // org.apache.thrift.transport.TIOStreamTransport, org.apache.thrift.transport.TTransport
    public void open() throws TTransportException {
        if (this.socket.isConnected()) {
            return;
        }
        try {
            this.socket.connect();
            this.inputStream_ = new BufferedInputStream(this.socket.getInputStream(), DNSConstants.FLAGS_AA);
            this.outputStream_ = new BufferedOutputStream(this.socket.getOutputStream(), DNSConstants.FLAGS_AA);
        } catch (IOException iox) {
            close();
            throw new TTransportException(1, iox);
        }
    }

    @Override // org.apache.thrift.transport.TIOStreamTransport, org.apache.thrift.transport.TTransport
    public int read(byte[] buf, int off, int len) throws TTransportException {
        if (this.inputStream_ == null) {
            throw new TTransportException(1, "Cannot read from null inputStream");
        }
        try {
            return this.inputStream_.read(buf, off, len);
        } catch (IOException iox) {
            throw new TTransportException(4, iox);
        } catch (NullPointerException npe) {
            Log.error(TAG, "BluetoothSocket is closed, and input stream is null", npe);
            throw new TTransportException(4);
        }
    }

    @Override // org.apache.thrift.transport.TIOStreamTransport, org.apache.thrift.transport.TTransport
    public void write(byte[] buf, int off, int len) throws TTransportException {
        if (this.outputStream_ == null) {
            throw new TTransportException(1, "Cannot write to null outputStream");
        }
        try {
            this.outputStream_.write(buf, off, len);
        } catch (IOException iox) {
            throw new TTransportException(1, iox);
        } catch (NullPointerException npe) {
            Log.error(TAG, "BluetoothSocket is closed, and output stream is null", npe);
            throw new TTransportException(1);
        }
    }

    @Override // org.apache.thrift.transport.TIOStreamTransport, org.apache.thrift.transport.TTransport
    public boolean isOpen() {
        return this.socket.isConnected();
    }

    @Override // org.apache.thrift.transport.TIOStreamTransport, org.apache.thrift.transport.TTransport
    public void close() {
        super.close();
        try {
            this.socket.close();
        } catch (IOException e) {
            Log.error(TAG, "Exception when closing BluetoothSocket", e);
        }
    }
}