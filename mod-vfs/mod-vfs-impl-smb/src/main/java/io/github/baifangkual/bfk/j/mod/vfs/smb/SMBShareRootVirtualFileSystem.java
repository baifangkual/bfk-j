package io.github.baifangkual.bfk.j.mod.vfs.smb;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileDirectoryQueryableInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msfscc.fileinformation.FileStandardInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.connection.NegotiatedProtocol;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.vfs.*;
import io.github.baifangkual.bfk.j.mod.vfs.exception.IllegalVFSBuildParamsException;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import io.github.baifangkual.bfk.j.mod.vfs.impl.AbstractVirtualFileSystem;
import io.github.baifangkual.bfk.j.mod.vfs.impl.DefaultSliceAbsolutePath;
import io.github.baifangkual.bfk.j.mod.vfs.impl.DefaultVFile;
import io.github.baifangkual.bfk.j.mod.vfs.smb.conf.SMBCfgOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.baifangkual.bfk.j.mod.vfs.smb.conf.SMBCfgOptions.*;


/**
 * SMB虚拟文件系统实现，该实现线程安全性未知，确定其需要查看源码或是看官方文档，该实现的线程安全依靠{@link SMBClient}的线程安全性，
 * 目前该依赖库smbj的文档和使用者较少，且未有官方文档对此说明等，查看源码发现有一些同步锁，可能该实现为线程安全的实现<br>
 * 该实现的构造函数所需的配置类配置在 {@link SMBCfgOptions}, 可参阅其<br>
 * <h5>已知问题和部分说明</h5>
 * <ul>
 *     <li>该实现仅支持smb协议v2~3，不支持v1协议，考虑到v1协议较为古老且速度并不高，遂后续也未有对v1协议支持的打算</li>
 *     <li>可用认证登录 user 和 passwd 或 guest访客 或 anonymous </li>
 * </ul>
 *
 * @author baifangkual
 * @since 2024/8/26 v0.0.5
 */
public class SMBShareRootVirtualFileSystem extends AbstractVirtualFileSystem implements VFS {

    private final static char[] NONE_PWD = new char[0];

    private final String host;
    private final int port;
    private final String share;
    private final String refRoot;
    private final AuthenticationContext ac;
    private final SMBClient smbClient;
    private final ConnPack singleConnPack;
    private final VPath vfsRoot;
    private final AtomicBoolean isClose = new AtomicBoolean(true);

    /**
     * 该构造应为所有子类型统一构造的入口，在该构造内，子类型有权校验及变更给定的配置,
     * 可能会影响给定的参数
     *
     * @param config 外界传递的vfs连接参数
     * @throws VFSBuildingFailException 当构造过程发生异常时，应由该包装或抛出
     */
    public SMBShareRootVirtualFileSystem(Cfg config) throws VFSBuildingFailException {
        super(config);
        final Cfg immutableConfig = readonlyCfg();
        this.host = immutableConfig.get(SMBCfgOptions.host);
        this.port = immutableConfig.getOrDefault(SMBCfgOptions.port);
        this.share = immutableConfig.get(SMBCfgOptions.share);
        this.refRoot = VFSDefaultConst.PATH_SEPARATOR + this.share;
        this.ac = createAc();
        // mid ref client
        SMBClient smbClient = null;
        boolean fail = false;
        try {
            smbClient = new SMBClient(createClientConf());
            ConnPack connPack = createSessionPack(smbClient, this.ac, this.host, this.port, this.share);
            /* conn 对象无需持有，因为上层的 smbClient有其host port 缓冲引用，下层session也有其引用 */
            Connection connect = connPack.getConnection();
            NegotiatedProtocol negotiatedProtocol = connect.getNegotiatedProtocol();
            Err.realIf((!negotiatedProtocol.getDialect().isSmb3x())
                       && immutableConfig.getOrDefault(withVersion3),
                    VFSBuildingFailException::new, "该SMB服务器不支持 smb v3 协议");
            Session session = connPack.getSession();
            this.smbClient = smbClient;
            this.singleConnPack = connPack;
            this.vfsRoot = new DefaultSliceAbsolutePath(this, VFSDefaultConst.PATH_SEPARATOR);
            this.isClose.compareAndSet(true, false);
        } catch (Exception e) {
            fail = true;
            if (e instanceof VFSBuildingFailException ve) {
                throw ve;
            } else {
                throw new VFSBuildingFailException(e);
            }
        } finally {
            if (fail && smbClient != null) {
                smbClient.close();
            }
        }

    }

    @Override
    protected void beforeCfgBind(Cfg config) {
        /*
        20241115 fix 当 share以 /或 "\" 结尾时，消除其后的那个符号
         */
        Optional<String> shareOpt = config.tryGet(SMBCfgOptions.share);
        if (shareOpt.isPresent()) {
            String share = shareOpt.get();
            if (share.endsWith("/") || share.endsWith("\\")) {
                share = share.substring(0, share.length() - 1);
                config.reset(SMBCfgOptions.share, share);
            }
        }
    }

    /**
     * 创建一个smb连接包，内部状态转化，smbClient会持有已创建的conn引用缓存，而conn会持有session引用缓存，
     * session会持有share引用缓存，遂在第一次创建之后，后续的该方法调用（若连接未关闭等）则是都从缓存中获取相同的对象,
     * 该方法的内部状态转换成功需要前提条件： host port smbClient ac share 参数均有且参数正确
     *
     * @return 连接包 包含 父子关系的三级 conn session share
     * @throws IOException 当创建过程中发生异常
     */
    private ConnPack createSessionPack(SMBClient smbClient,
                                       AuthenticationContext ac,
                                       String host, int port, String share) throws IOException {
        Connection connect = smbClient.connect(host, port);
        Session session = connect.authenticate(ac);
        DiskShare dShare = (DiskShare) session.connectShare(share);
        return new ConnPack(connect, session, dShare);
    }

    @Override
    protected void afterReadonlyCfgBind(Cfg immutableConfig) throws IllegalVFSBuildParamsException {
        Optional<String> shareOpt = immutableConfig.tryGet(SMBCfgOptions.share);
        if (shareOpt.isEmpty() || shareOpt.get().startsWith("/"))
            throw new IllegalVFSBuildParamsException("非法参数share");
    }

    /**
     * 部分，构造smbConnect所需的ac
     */
    private AuthenticationContext createAc() {
        final Cfg immutableConfig = readonlyCfg();
        String user;
        String passwd = null;
        String domain = null;
        if (immutableConfig.getOrDefault(acWithAnonymous)) { //匿名任何
            user = "";
        } else if (immutableConfig.getOrDefault(acWithGuest)) { //访客
            user = "guest";
        } else {
            user = immutableConfig.get(SMBCfgOptions.user);
            // 密码及域可能为空正常
            passwd = immutableConfig.tryGet(SMBCfgOptions.passwd).orElse(null);
            domain = immutableConfig.tryGet(SMBCfgOptions.domain).orElse(null);
        }
        return new AuthenticationContext(user, passwd == null ? NONE_PWD : passwd.toCharArray(), domain);
    }

    /**
     * 部分，构造创建smbClient所需的SmbConfig，部分参数设置其
     */
    private SmbConfig createClientConf() {
        final Cfg immutableConfig = readonlyCfg();
        SmbConfig.Builder builder = SmbConfig.builder();
        if (immutableConfig.getOrDefault(withNegotiatedBufSize)) {
            builder.withNegotiatedBufferSize();
        } else {
            builder.withBufferSize(immutableConfig.getOrDefault(bufSize));
        }
        return builder.build();
    }

    @Override
    public VFSType type() {
        return VFSType.smb;
    }


    @Override
    public VPath root() throws VFSIOException {
        return vfsRoot;
    }

    @Override
    public boolean isClosed() {
        return isClose.get();
    }


    @Override
    public List<VPath> lsDir(VPath path) throws VFSIOException {
        DiskShare ds = this.singleConnPack.getShare();
        String sp = path.simplePath();
        Err.realIf(!ds.folderExists(sp), VFSIOException::new, "Not a directory: '{}'", sp);
        List<FileIdBothDirectoryInformation> list = ds.list(sp);
        return list.stream().map(FileDirectoryQueryableInformation::getFileName)
                .filter(n -> !(n.equals(VFSDefaultConst.CURR_PATH) || n.equals(VFSDefaultConst.PARENT_PATH)))
                .map(path::join)
                .toList();
    }

    @Override
    public List<VFile> lsDir(VFile file) throws VFSIOException {
        Err.realIf(!file.isDirectory(), VFSIOException::new, "Not a directory: {}", file.toPath());
        VPath fPath = file.toPath();
        DiskShare ds = this.singleConnPack.getShare();
        List<FileIdBothDirectoryInformation> fL = ds.list(fPath.simplePath());
        return fL.stream().filter(f -> !f.getFileName().equals(VFSDefaultConst.CURR_PATH)
                                       && !f.getFileName().equals(VFSDefaultConst.PARENT_PATH))
                .map(f -> {
                    String fn = f.getFileName();
                    boolean dir = FileAttrTranslate.isDir(f.getFileAttributes());
                    /* 隐藏文件显示否？*/
                    return (VFile) (new DefaultVFile(this, fPath.join(fn),
                            dir ? VFileType.directory : VFileType.simpleFile,
                            dir ? 0L : f.getAllocationSize()));
                }).toList();
    }

    @Override
    public VFile mkdir(VPath path) throws VFSIOException {
        /*
        20241107 fix 如果为root，即已经存在，又因为该方法的策略为确保结果一致性，
        遂当self为root时，跳过创建目录的过程即可
        20250517 该方法已取消结果一致性保证，方法语义更严格，遂若为root，抛出异常，因为root一定已经存在了
         */
        Objects.requireNonNull(path, "given path is null");
        if (path.isVfsRoot()) {
            throw new VFSIOException("directory already exists");
        }
        DiskShare ds = this.singleConnPack.getShare();
        ds.mkdir(path.simplePath());
        // 勉强 可能逻辑重复冗余, 或直接创建VFile更好？
        return getFile(path).orElseThrow(() -> new VFSIOException(STF.f("\"{}\" not exists", path)));
    }

    @Override
    public void rmFile(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        DiskShare ds = this.singleConnPack.getShare();
        // 20250517 该方法表意已修改，没有结果一致性语义，即若位置本来无一物，
        // 则抛出异常，但该SMB实现尚不清楚（可能测试过，但我忘了）该的rm是何种含义，遂这里先不添加tryGetFile的冗余
        // 20250517 无需纠正，该方法已修改，没有结果一致性语义,结果一致性与否，下发给下级原生行为
        ds.rm(path.simplePath());
    }

    @Override
    public void rmdir(VPath path, boolean recursive) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        DiskShare ds = this.singleConnPack.getShare();
        // 20250517 该方法表意已修改，没有结果一致性语义，即若位置本来无一物，
        // 则抛出异常，但该SMB实现尚不清楚（可能测试过，但我忘了）该的rm是何种含义，遂这里先不添加tryGetFile的冗余
        // 20250517 无需纠正，该方法已修改，没有结果一致性语义,结果一致性与否，下发给下级原生行为
        ds.rmdir(path.simplePath(), recursive);
    }

    @Override
    public boolean exists(VPath path) throws VFSIOException {
        if (path.isVfsRoot()) {
            return true;
        }
        DiskShare ds = singleConnPack.getShare();
        String sp = path.simplePath();
        return ds.folderExists(sp) || ds.fileExists(sp);
    }

    @Override
    public Optional<VFile> getFile(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        if (path.isVfsRoot()) {
            return Optional.of(new DefaultVFile(this, vfsRoot, VFileType.directory, 0));
        }
        if (exists(path)) {
            DiskShare ds = singleConnPack.getShare();
            String sp = path.simplePath();
            FileStandardInformation fI = ds.getFileInformation(sp, FileStandardInformation.class);

            return Optional.of(new DefaultVFile(this, path,
                    fI.isDirectory() ? VFileType.directory : VFileType.simpleFile,
                    fI.isDirectory() ? 0L : fI.getAllocationSize()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public InputStream getFileInputStream(VFile file) throws VFSIOException {
        if (file.isDirectory()) throw new VFSIOException(STF.f("\"{}\" is a directory", file));
        else if (file.isSimpleFile()) {
            DiskShare ds = this.singleConnPack.getShare();
            File openFile = ds.openFile(file.toPath().simplePath(),
                    Set.of(AccessMask.FILE_READ_DATA),
                    null,
                    Set.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null
            );
            InputStream inputStream = openFile.getInputStream();
            return new SMBJInputStreamDelegator(openFile, inputStream);
        } else {
            throw new VFSIOException(STF.f("Not a simple file or directory: '{}'", file));
        }
    }

    private OutputStream createFile(VPath path) throws VFSIOException {
        Err.realIf(path.toFile().isPresent(), VFSIOException::new, "\"{}\" 已存在，无法创建", path);
        DiskShare ds = singleConnPack.getShare();
        File openFile = ds.openFile(path.simplePath(),
                Set.of(AccessMask.FILE_WRITE_DATA),
                null,
                Set.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_CREATE,
                Set.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH)
        );
        OutputStream outputStream = openFile.getOutputStream();
        return new SMBJOutputStreamDelegator(openFile, outputStream);
    }

    @Override
    public VFile mkFile(VPath path, InputStream newFileData) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        Objects.requireNonNull(newFileData, "given input stream is null");
        if (getFile(path).isPresent()) {
            throw new VFSIOException(STF.f("{}:\"{}\" already exists", this, path));
        }
        try (OutputStream out = createFile(path)) {
            newFileData.transferTo(out);
        } catch (IOException e) {
            throw new VFSIOException(e);
        }
        return path.toFile().orElseThrow(() -> new VFSIOException("not found file on path: " + path));
    }

    @Override
    public void close() {
        if (isClose.compareAndSet(false, true)) {
            this.singleConnPack.quietClose();
            this.smbClient.close();
        }
    }

}
