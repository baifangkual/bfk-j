package io.github.baifangkual.jlib.vfs.smb;

import com.hierynomus.smbj.share.File;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 委托 smbj outputStream
 *
 * @author baifangkual
 * @since 2024/8/29
 */
@RequiredArgsConstructor
class SMBJOutputStreamDelegator extends OutputStream {

    private final File smbFileCloseableRef;
    private final OutputStream delegate;

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            smbFileCloseableRef.closeSilently();
        }
    }


}
