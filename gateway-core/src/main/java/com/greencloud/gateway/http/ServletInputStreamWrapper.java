package com.greencloud.gateway.http;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;

/**
 * ServletInputStream wrapper to wrap a byte[] into a ServletInputStream
 *
 * @author leejianhao
 */
public class ServletInputStreamWrapper extends ServletInputStream {

    private byte[] data;
    private int idx = 0;
    private ReadListener readListener;

    /**
     * Creates a new <code>ServletInputStreamWrapper</code> instance.
     *
     * @param data a <code>byte[]</code> value
     */
    public ServletInputStreamWrapper(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        this.data = data;
    }

    @Override
    public int read() throws IOException {
        if (idx == data.length) {
            return -1;
        }
        // I have to AND the byte with 0xff in order to ensure that it is returned as an unsigned integer
        // the lack of this was causing a weird bug when manually unzipping gzipped request bodies
        return data[idx++] & 0xff;
    }

    @Override
    public boolean isFinished() {
        return (idx == data.length-1);
    }

    @Override
    public boolean isReady() {
        // This implementation will never block
        // We also never need to call the readListener from this method, as this method will never return false
        return isFinished();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        this.readListener = readListener;
        if (!isFinished()) {
            try {
                readListener.onDataAvailable();
            } catch (IOException e) {
                readListener.onError(e);
            }
        } else {
            try {
                readListener.onAllDataRead();
            } catch (IOException e) {
                readListener.onError(e);
            }
        }
    }
}