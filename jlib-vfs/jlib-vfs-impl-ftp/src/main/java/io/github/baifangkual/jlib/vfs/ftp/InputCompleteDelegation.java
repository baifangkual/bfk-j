package io.github.baifangkual.jlib.vfs.ftp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FTP CLi Socket 引申出的 inputStream 部分行为的委托代理
 * 在Close时，会先关闭父Socket套接字，然后通过completePendingCommand，收尾传输行为命令
 * 最后将Cli归还到VFS中
 *
 * @author baifangkual
 * @since 2024/9/18 v0.0.5
 */
@Slf4j
@RequiredArgsConstructor
class InputCompleteDelegation extends InputStream {

    private final InputStream in;
    private final BorrowableCliDelegation cli;
    private final FTPVirtualFileSystem vfs;

    static InputCompleteDelegation accept(InputStream in,
                                          BorrowableCliDelegation cli,
                                          FTPVirtualFileSystem vfs) {
        return new InputCompleteDelegation(in, cli, vfs);
    }

    @Override
    public void close() throws IOException {
        boolean fComp = false;
        boolean sCs = innerClose();
        try {
            if (!cli.completePendingCommand()) {
                fComp = true;
            }
        } catch (IOException e) {
            log.warn("FTP Client on completePendingCommand IOException, msg:{}", e.getMessage());
            if (!sCs) {
                throw e;
            }
        } finally {
            // 当流发生异常并且 complete 也异常时
            if (fComp && !sCs) {
                log.error("FTP Client completePendingCommand false and inputStream close fail");
            }
            // 归还 引用, 无论如何都要归还
            vfs.awaitRecyclingOneBusy(cli);
        }

    }

    private boolean innerClose() {
        try {
            in.close();
            return true;
        } catch (IOException e) {
            log.warn("FTP inputStream close error， msg:{}", e.getMessage());
            return false;
        }
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return in.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return in.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return in.readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        in.skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return in.transferTo(out);
    }


}
