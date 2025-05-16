package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端借出代理, 可被比较， impl hash & eq
 *
 * @author baifangkual
 * @since 2024/9/18 v0.0.5
 */
@Slf4j
class BorrowableCliDelegation implements Closeable {

    private static final AtomicLong loopIdGenerator = new AtomicLong(0);

    private final Long loopId;
    private final FTPClient cli;
    private final AtomicBoolean borrowed = new AtomicBoolean(false);

    BorrowableCliDelegation(FTPClient cliRef) {
        this.loopId = loopIdGenerator.incrementAndGet();
        this.cli = cliRef;
        if (!borrowed.compareAndSet(false, true)) {
            throw new IllegalStateException("原子状态异常");
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BorrowableCliDelegation that)) return false;

        return loopId.equals(that.loopId);
    }

    @Override
    public int hashCode() {
        return loopId.hashCode();
    }

    static BorrowableCliDelegation wrap(FTPClient cliRef) {
        return new BorrowableCliDelegation(cliRef);
    }

    public InputStream retrieveFileStream(String remote) throws IOException {
        return cli.retrieveFileStream(remote);
    }

    public boolean storeFile(String remote, InputStream local) throws IOException {
        return cli.storeFile(remote, local);
    }

    boolean completePendingCommand() throws IOException {
        return cli.completePendingCommand();
    }

    boolean casSetUnBorrowed() {
        return borrowed.compareAndSet(true, false);
    }

    boolean casSetBorrowed() {
        return borrowed.compareAndSet(false, true);
    }

    boolean isBorrowed() {
        return borrowed.get();
    }

    @Override
    public void close() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Closing Transform Data FTP client...");
        }
        try {
            this.cli.logout();
        } finally {
            this.cli.disconnect();
            if (log.isDebugEnabled()) {
                log.debug("Closed Transform Data FTP client");
            }
        }
    }

    void sneakyClose() {
        try {
            close();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("error on FTP Client sneaky close", e);
            }
        }
    }
}
