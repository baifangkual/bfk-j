package io.github.baifangkual.jlib.vfs.smb;

import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;

/**
 * 连接包，包装为Closeable
 *
 * @author baifangkual
 * @since 2024/8/29
 */
@Slf4j
@Getter
@RequiredArgsConstructor
class ConnPack implements Closeable {

    private final Connection connection;
    private final Session session;
    private final DiskShare share;

    @Override
    public void close() throws IOException {
        try {
            share.close();
        } catch (IOException e) {
            throw new VFSIOException(e.getMessage(), e);
        } finally {
            try {
                session.close();
            } finally {
                connection.close();
            }
        }
    }

    void quietClose() {
        try {
            close();
        } catch (IOException e) {
            log.warn("closing session error", e);
        }
    }
}
