package io.github.baifangkual.jlib.vfs.smb;

import com.hierynomus.smbj.share.File;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 委托 smbj inputStream
 *
 * @author baifangkual
 * @since 2024/8/29
 */
@RequiredArgsConstructor
class SMBJInputStreamDelegator extends InputStream {

    private final File smbFileCloseableRef;
    private final InputStream delegate;

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return delegate.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return delegate.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return delegate.readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        delegate.skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return delegate.transferTo(out);
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
