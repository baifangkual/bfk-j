package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.vfs.*;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import io.github.baifangkual.bfk.j.mod.vfs.ftp.conf.FTPCfgOptions;
import io.github.baifangkual.bfk.j.mod.vfs.impl.AbstractVirtualFileSystem;
import io.github.baifangkual.bfk.j.mod.vfs.impl.DefaultSliceAbsolutePath;
import io.github.baifangkual.bfk.j.mod.vfs.impl.DefaultVFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.io.CopyStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/**
 * ftp协议的虚拟文件系统实现，该实现线程安全<p>
 * 该实现的构造函数所需的配置类配置在 {@link FTPCfgOptions}, 可参阅其<br>
 * <b>ftp相关注意及该实现行为</b>
 * <ul>
 *     <li>该实现对链接（符号链接、软链接等）不可见，遂也不能操作其</li>
 *     <li>默认启动utf8编码自动检测，服务端也最好使用utf-8编码，若非，则可设置指定其</li>
 *     <li>默认使用ftp的被动模式，因客户端通常都启用了防火墙</li>
 *     <li>vsftpd 2.3.5开始，用户锁定的chroot目录必须是不可写的，若要进行写操作，请确保服务端配置正确的写权限</li>
 *     <li>当服务端类似vsftpd的chroot_local_user=YES启动，用户可访问目录被限制，此时无法得知真实的root地址</li>
 *     <li>不支持FTP的ASCII模式，即使服务器启动ASCII模式支持，因其ASCII模式下 SIZE /big/file 可进行DOS攻击且可能对部分字节字符有更改 </li>
 *     <li>当ftp客户端空闲时间超过给定时间段，ftp服务器可能会选择提前关闭连接，该设置了控制空闲发送NOOP重置空闲定时器，默认值为{@link FTPCfgOptions#controlKeepAliveTimeoutSec}</li>
 *     <li>参数{@link FTPCfgOptions#transformQueueMaxSize}控制了可同时读+写的流数量，当超过该值的其他线程要读/写文件流时，将会阻塞，直到另一个线程读/写流完成并关闭流</li>
 *     <li>该实现中{@link #getFile(VPath)}方法优先使用FTP的MLST命令，当连接的FTP服务器不支持该命令时，将会降级使用LIST命令，相对MLST命令，该命令较为低效</li>
 *     <li>该实现中许多方法未进行前置的{@link #ifClosedThrowVFSRtIOE()}校验，遂当该VFS关闭后，部分行为会抛出不明确的异常，依赖底层实现</li>
 *     <li>在该实现连接FTP服务器时将会检查FTP服务器支持的命令，当驱动该VFS的基础命令不支持时，该VFS将会构造失败，抛出异常</li>
 * </ul>
 *
 * @author baifangkual
 * @since 2024/9/9 v0.0.5
 */
@Slf4j
public class FTPVirtualFileSystem extends AbstractVirtualFileSystem implements VFS {

    private final AtomicBoolean closed = new AtomicBoolean(true);
    private final FTPClient mainControlCli;
    private final String workingDirectory;
    private final VPath root;
    // FTP服务器 支持的命令列表
    private final List<String> supportCMDs;
    private final boolean supportMLST;
    private final int transformQueueMaxSize;
    private final BlockingQueue<BorrowableCliDelegation> freeQueue;
    private final BlockingQueue<BorrowableCliDelegation> busyQueue;
    private final Lock mainControlLock = new ReentrantLock();
    private final Lock transformQueueLock = new ReentrantLock();
    private final Condition emptyFree = transformQueueLock.newCondition();


    /**
     * 该构造应为所有子类型统一构造的入口，在该构造内，子类型有权校验及变更给定的配置,
     * 可能会影响给定的参数
     *
     * @param cfg 外界传递的vfs连接参数
     * @throws VFSBuildingFailException 当构造过程发生异常时，应由该包装或抛出
     */
    public FTPVirtualFileSystem(Cfg cfg) throws VFSBuildingFailException {
        super(cfg);
        try {
            this.mainControlCli = buildingCli(this.readonlyCfg());
            this.supportCMDs = analysisSupportHelps();
            boolean basicSupport = FTPSupport.isBasicSupport(this.supportCMDs);
            if (!basicSupport) {
                throw new VFSBuildingFailException(STF
                        .f("连接的FTP服务器不支持要求的基本的命令，无法构建FTPVirtualFileSystem，当前连接的FTP服务器支持的命令: {}",
                                this.supportCMDs));
            }
            this.supportMLST = FTPSupport.supportMLST(this.supportCMDs);
            /*
            获取登陆后分配的workingDirectory，
            经测试，类似 vsftpd 服务打开 chroot 后 该路径值固定为 “/”，该路径值已无法反应实际路径，
            但关闭 chroot 后 该值便能正确反映服务器路径的实际 作业路径了，查看FTPClient该方法doc可知
            当该值无法获取时，将返回空，尚不知空值如何处理，遂在此处判定，当给定返回空值时，抛出异常
             */
            this.workingDirectory = cleanGetPrintWorkingDirectory();
            this.transformQueueMaxSize = this.readonlyCfg().getOrDefault(FTPCfgOptions.transformQueueMaxSize);
            this.freeQueue = new ArrayBlockingQueue<>(transformQueueMaxSize);
            // 因为可能被借用的顺序和回收的顺序不同，这里应使用链表防止类似数组的多次位移开销
            this.busyQueue = new LinkedBlockingQueue<>(transformQueueMaxSize);
            this.root = new DefaultSliceAbsolutePath(this, VFSDefaultConst.PATH_SEPARATOR);
            this.closed.compareAndSet(true, false);
        } catch (IOException e) {
            throw new VFSBuildingFailException(e.getMessage(), e);
        }

    }

    private List<String> analysisSupportHelps() throws IOException {
        mainControlLock.lock();
        try {
            String helps = this.mainControlCli.listHelp();
            return FTPSupport.analysisSupportCMDList(helps);
        } finally {
            mainControlLock.unlock();
        }
    }

    /**
     * 对workingDirectory值做清理，不允许出现null值，当为null时，抛出异常
     */
    private String cleanGetPrintWorkingDirectory() throws IOException {
        mainControlLock.lock();
        try {
            String workingDir = this.mainControlCli.printWorkingDirectory();
            if (workingDir == null) {
                throw new VFSBuildingFailException("无法获取ftp的workingDirectory");
            }
            // 对可能的win路径构造进行处理
            if (workingDir.contains("\\")) {
                workingDir = workingDir.replace("\\", VFSDefaultConst.PATH_SEPARATOR);
            }
            return workingDir;
        } finally {
            mainControlLock.unlock();
        }
    }

    private FTPClient buildingCli(Cfg cfg) throws IOException {

        // 第一次创建成功后，这里的后续创建仍会从cfg中寻找，可优化其在创建成功后保存其，后续可不从cfg中寻找
        String host = cfg.get(FTPCfgOptions.host);
        int port = cfg.getOrDefault(FTPCfgOptions.port);
        String user = cfg.getOrDefault(FTPCfgOptions.user);
        String passwd = cfg.getOrDefault(FTPCfgOptions.passwd);
        int bufSize = cfg.getOrDefault(FTPCfgOptions.bufSize);
        boolean passiveMode = cfg.getOrDefault(FTPCfgOptions.usePassiveMode);
        String tz = cfg.getOrDefault(FTPCfgOptions.serverTimeZone);
        int connectTimeOut = cfg.getOrDefault(FTPCfgOptions.connectTimeoutMs);
        String encoding = cfg.getOrDefault(FTPCfgOptions.encoding);
        boolean autodetectUtf8 = cfg.getOrDefault(FTPCfgOptions.autodetectUTF8);
        boolean showHiddenFile = true; /* 20240913 不允许隐藏隐藏的文件，在服务器不支持MLST的情况下会导致行为异常 */
        Duration keepAliveHeartbeat = Duration.ofSeconds(cfg.getOrDefault(FTPCfgOptions.controlKeepAliveTimeoutSec));

        //client config
        FTPClientConfig cliConf = new FTPClientConfig(); // default FTPClientConfig.SYST_UNIX
        cliConf.setServerTimeZoneId(tz);
        cliConf.setUnparseableEntries(false);

        //building
        FTPClient cli = new FTPClient();
        cli.setConnectTimeout(connectTimeOut);
        cli.configure(cliConf);
        cli.setControlEncoding(encoding);
        cli.setAutodetectUTF8(autodetectUtf8); /* 是否启用服务器自动编码检测(仅支持UTF8) 连接前设置 */
        cli.setRemoteVerificationEnabled(false); /* 禁用主机验证 提高效率 */
        cli.setControlKeepAliveTimeout(keepAliveHeartbeat); /* 保活信号频率 */
        cli.connect(host, port);

        if (!FTPReply.isPositiveCompletion(cli.getReplyCode())) { /* 查看服务器响应码 是否连接成功 */
            cli.disconnect();
            throw new IOException("ftpClient connect to " + host + ":" + port + "fail");
        }

        boolean loginFlag = cli.login(user, passwd);
        if (!loginFlag) {
            throw new VFSBuildingFailException("FTP登录的用户名或密码错误");
        }
        cli.setBufferSize(bufSize); /* 设置传输缓冲区大小 */
        /*
        当不显示隐藏文件时，目录中 如 "." 和 ".." 都是不显示的，当然隐藏文件及文件夹也不显示，
        经过尝试，ftp想要查看文件或文件夹是否存在，尤其查看文件夹是否存在，最好使用 listFiles(...)
        然后看其返回的数组中0下标的"."文件是否有，或者说数组是否为空数组，当不隐藏隐藏文件显示时，若文件夹实际存在，
        则文件夹listFiles显示的结果中肯定不为空数组，并且 当 服务器没有提供 MLST 支持时，也没有较为简单的方法查看自身
        遂这里最好或一定不可隐藏隐藏文件的显示，当ftp服务器支持 MLST 时，或可不需要使用隐藏文件表示存在与否，
        如果用户明确要求要隐藏隐藏文件的显示，则该vfs认为服务器的行为支持该vfs的功能正常
         */
        cli.setListHiddenFiles(showHiddenFile); /* 尽量或一定要显示隐藏文件，该的部分行为依赖隐藏文件 */

        if (!cli.setFileType(FTP.BINARY_FILE_TYPE)) { /* 连接后设置文件传输类型 重新连接将重置该设置 */
            log.warn("设置FTP服务器文件传输格式失败 将使用服务器默认值");
        }
        if (passiveMode) {  //tod 最好为 被动模式 由客户端主动联系服务端 服务端的数据端口使用被动模式 方式客户端防火墙拦截
            cli.enterLocalPassiveMode();
        }
        return cli;

    }

    @Override
    public VFSType type() {
        return VFSType.ftp;
    }

    @Override
    public VPath root() {
        return root;
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public List<VPath> lsDir(VPath path) throws VFSIOException {
        mainControlLock.lock();
        try {
            // to do 必须其可ls 已知 ftpCli在 path 不存在时不会抛出异常，该情况应该暴露
            // 后续或可将该的行为变更，要知道 tryGetFile在行为降级（不支持MLST时）走的是LIST，会查询两次，完全没必要
            Optional<VFile> fOpt = getFile(path);
            if (fOpt.isEmpty() || !fOpt.get().isDirectory()) {
                throw new VFSIOException(STF.f("\"{}\" not a directory or not exists", path));
            }
            /*
            已知在部分ftp客户端上，使用 / 为开头，当非chroot时，可能指代了以文件系统为根的绝对路径
            遂最良好的方式是使用其相对路径，以 ./ 或直接路径名称为好
            行为二选一之下，遂这里选择使用 Point . + / 表示当前
             */
            String qP = pathTranslate(path);
            FTPFile[] ftpF = FTPSupport.sneakyRun(() ->
                    this.mainControlCli.listFiles(qP, FTPSupport.NOT_NULL_AND_NOT_MAGIC_AND_NOT_LINK));
            return convertF2(path, ftpF, this::convertF2P);
        } finally {
            mainControlLock.unlock();
        }
    }

    /**
     * 候选方法<p>
     * 给定一个路径，返回该路径表示的实体文件（不是文件夹）是否存在，
     * 因FTP MDTM 命令的行为，该方法无法确定文件夹是否存在
     */
    private boolean simpleFIleExistsUseMDTMCMD(VPath path) {
        mainControlLock.lock();
        try {
            if (path.isVfsRoot()) return false;
            String modificationTime = FTPSupport.sneakyRun(() ->
                    this.mainControlCli.getModificationTime(pathTranslate(path)));
            return modificationTime != null;
        } finally {
            mainControlLock.unlock();
        }
    }

    /**
     * 候选方法<p>
     * 与 {@link #simpleFIleExistsUseMDTMCMD(VPath)} 行为相似，也仅能获取给定的路径是否存在
     * 文件（非文件夹），该方法遂也无法检测文件夹是否存在，该方法使用FTP的 SIZE 命令
     */
    private boolean simpleFileExistsUseSIZECMD(VPath path) {
        mainControlLock.lock();
        try {
            if (path.isVfsRoot()) return false;
            String size = FTPSupport.sneakyRun(() ->
                    this.mainControlCli.getSize(pathTranslate(path)));
            return size != null;
        } finally {
            mainControlLock.unlock();
        }
    }

    /**
     * 将路径转为 ftpCli.listFiles使用的正确的路径，
     * 在 FTP LIST 命令中，LIST 命令 不追加目录表示本身，否则表示追加的目录，遂当为root时，为null即可
     */
    private String pathTranslate(VPath path) {
        if (path.isVfsRoot()) {
            return null;
        }
        return VFSDefaultConst.CURR_PATH + path.simplePath();
    }

    @Override
    public List<VFile> lsDir(VFile file) throws VFSIOException {
        mainControlLock.lock();
        try {
            if (file.isDirectory()) {
                VPath path = file.toPath();
                String qP = pathTranslate(path);
                FTPFile[] ftpF = FTPSupport.sneakyRun(() ->
                        this.mainControlCli.listFiles(qP, FTPSupport.NOT_NULL_AND_NOT_MAGIC_AND_NOT_LINK));
                return convertF2(path, ftpF, this::convertF2F);
            } else {
                throw new VFSIOException(STF.f("{} not a directory", file.toPath()));
            }
        } finally {
            mainControlLock.unlock();
        }
    }

    private <R> List<R> convertF2(VPath father, FTPFile[] ftpFiles, BiFunction<VPath, FTPFile, ? extends R> converter) {
        if (ftpFiles == null || ftpFiles.length == 0) {
            return Collections.emptyList();
        }
        List<R> rs = new ArrayList<>(ftpFiles.length);
        for (FTPFile ftpFile : ftpFiles) {
            rs.add(converter.apply(father, ftpFile));
        }
        return rs;
    }

    private VPath convertF2P(VPath father, FTPFile ftpFile) {
        return father.join(ftpFile.getName());
    }

    private VFile convertF2F(VPath father, FTPFile ftpFile) {
        String name = ftpFile.getName();
        VFileType ft;
        long f_size = 0;
        if (ftpFile.isDirectory()) {
            ft = VFileType.directory;
        } else if (ftpFile.isFile()) {
            ft = VFileType.simpleFile;
        } else if (ftpFile.isSymbolicLink()) {
            ft = VFileType.link;
        } else {
            throw new VFSIOException(STF.f("ftp file : {} type undefined, type: {}", ftpFile.getName(),
                    ftpFile.getType()));
        }
        if (ft == VFileType.simpleFile) {
            f_size = ftpFile.getSize();
        }
        return new DefaultVFile(this, father.join(name), ft, f_size);
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
        mainControlLock.lock();
        try {
        /*
        已测试FTPCli的makeDirectory行为，发现有：
        当给定位置无实体，目录创建成功，布尔值返回true，
        当给定位置无实体，但因多级目录等，目录创建失败，布尔值返回false，
        当给定位置已有实体，目录创建失败，布尔值返回false，
        因为该vfs的mkdir设定为结果的一致性，即目录已存在与目录成功创建行为等价，
        遂上述FTPCli的“当给定位置已有实体，目录创建失败，布尔值返回false”行为要被纠正
        20250517 该方法已取消结果一致性保证，方法语义更严格, 上述纠正已被纠正
         */
            Boolean mkr = FTPSupport.sneakyRun(() ->
                    this.mainControlCli.makeDirectory(pathTranslate(path)));
            // 勉强 可能逻辑重复冗余, 或直接创建VFile更好？
            VFile fo = getFile(path)
                    .orElseThrow(() -> new VFSIOException(STF
                            .f("\"{}\" not found directory, make directory unknown error", path)));
            if (!mkr) {
                if (!fo.isDirectory()) {
                    throw new VFSIOException(STF.f("\"{}\" already exists a simple file", path));
                } else {
                    throw new VFSIOException(STF.f("\"{}\" already exists a directory", path));
                }
            }
            return fo;
        } finally {
            mainControlLock.unlock();
        }
    }

    /**
     * 借用一个 {@link BorrowableCliDelegation} 对象，当有空闲的该对象时，将空闲的该对象从空闲队列移至忙链表
     * 并返回之，当无空闲对象且总持有的该对象数量小于{@link #transformQueueMaxSize}值，则创建一个该对象，将之移动至
     * 忙链表并返回之，当无空闲对象且总持有的对象数量等于{@link #transformQueueMaxSize}值时，间接调用该方法的线程将阻塞至此，
     * 直到有可用的对象被回收，即其他线程调用了{@link #awaitRecyclingOneBusy(BorrowableCliDelegation)}
     *
     * @throws BorrowThreadInterruptedException 当阻塞线程收到外界发送的中断信号时
     */
    BorrowableCliDelegation awaitBorrowOneFree() throws BorrowThreadInterruptedException {
        transformQueueLock.lock();
        try {
            while (busyQueue.size() == this.transformQueueMaxSize && freeQueue.isEmpty()) {
                // 情况1，所有已借出，并且正在忙，该情况下，要求当前线程等待，防止伪唤醒
                emptyFree.await();
                /*
                在设计的FTP VFS close 时，将会唤醒所有等待的线程，其醒来后要检查 VFS 是否已关闭，
                若已关闭，则使这些线程异常中断即可
                 */
                ifClosedThrowVFSRtIOE();
            }
            BorrowableCliDelegation refCli;
            // 情况2或阻塞完成，线程抢到锁，有可用的，直接借用其
            if (!freeQueue.isEmpty()) {
                refCli = freeQueue.take();
                /*
                唤醒在 满 状态等待的单个线程 使其可 回收，放入free队列
                这里不需要 fullFree.signal();
                 */
                /*
                需明确所有读写行为都会先借用再回收，也因此
                这里无需有第二个类似fullFree的Condition，因为所有借用的线程，若可借用对象不足，会阻塞在该方法内，
                后续有对象被回收才会被唤醒，而且freeQueue的容量最大值为transformQueueMaxSize值，不会导致队列满而
                部分线程等待队列有空位的情况
                 */
                if (refCli.isBorrowed()) {
                    throw new IllegalBorrowStateException("处于空闲队列的可借用对象已为已借用状态");
                }
                // 将其状态转换为已借出，并放入忙队列
                if (!refCli.casSetBorrowed()) {
                    throw new IllegalBorrowStateException("BorrowableCliDelegation 原子状态异常");
                }
            } else {
                // 情况3 空闲队列为空，借不了，而且忙队列总数 小于 最大数量，即可创建新的
                if (busyQueue.size() < this.transformQueueMaxSize) {
                    FTPClient cli = buildingCli(this.readonlyCfg());
                    // 新构建的默认为已借出状态
                    refCli = BorrowableCliDelegation.wrap(cli);
                } else {
                    throw new IllegalBorrowStateException("空闲队列为空, 忙队列已满，非法状态");
                }
            }
            busyQueue.put(refCli);
            if (log.isDebugEnabled()) {
                log.debug("borrow one Cli: {}", refCli);
            }
            return refCli;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断标志
            throw new BorrowThreadInterruptedException(e);
        } catch (IOException e) {
            throw new VFSIOException(e.getMessage(), e);
        } finally {
            transformQueueLock.unlock();
        }
    }

    /**
     * 归还一个{@link BorrowableCliDelegation}, 即将该类型对象从忙链表移至空闲队列，并唤醒一个{@link #awaitBorrowOneFree()}
     * 且无空闲对象的阻塞线程
     *
     * @param cli ftpCli delegation
     * @throws BorrowThreadInterruptedException 当阻塞线程收到外界发送的中断信号时
     */
    void awaitRecyclingOneBusy(BorrowableCliDelegation cli) throws BorrowThreadInterruptedException {
        transformQueueLock.lock();
        try {
            if (!cli.isBorrowed()) {
                throw new IllegalBorrowStateException("被借用对象在回收前不在借用状态");
            }
            if (!cli.casSetUnBorrowed()) {
                throw new IllegalBorrowStateException("BorrowableCliDelegation 原子状态异常");
            }
            if (!busyQueue.remove(cli)) {
                throw new IllegalBorrowStateException("被回收对象不在忙队列中");
            }
            if (log.isDebugEnabled()) {
                log.debug("recycling one Cli: {}", cli);
            }
            // 在此处，cli已经被置为未借用状态，并且从忙队列中删除了, 先归还其至空闲队列并唤醒任意一个等待借用的线程
            freeQueue.put(cli);
            // 单唤醒，因为此处是一个一个还，所以一个一个醒即可，无需多个争夺锁
            emptyFree.signal();
        } catch (InterruptedException e) {
            throw new BorrowThreadInterruptedException(e);
        } finally {
            transformQueueLock.unlock();
        }
    }

    @Override
    public void rmFile(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        mainControlLock.lock();
        try {
            /*
            已确定使用 FTP DELE 命令 删除 文件，经测试，该命令行为：
            仅能删除普通文件，文件夹无法删除，当文件被删除，cli.deleteFIle返回 true,
            当给定目录所在无文件，则返回false，这里需纠正这种给定位置无文件但返回false的情况
            20250517 无需纠正，该方法已修改，没有结果一致性语义,结果一致性与否，下发给下级原生行为
             */
            Boolean rmr = FTPSupport.sneakyRun(() ->
                    this.mainControlCli.deleteFile(pathTranslate(path)));
            if (!rmr) {
                Optional<VFile> fo = getFile(path);
                if (fo.isPresent()) {
                    if (fo.get().isDirectory()) {
                        throw new VFSIOException(STF
                                .f("\"{}\" already exists a directory, can't use rmFile to remove directory", path));
                    } else if (!fo.get().isDirectory()) {
                        throw new VFSIOException(STF.f("\"{}\" already exists, delete file unknown error", path));
                    }
                } else {
                    // 即不存在，结合上述的“当给定目录所在无文件，则返回false”，即这种情况表示该位置本来无一物，应抛出异常
                    throw new VFSIOException(STF.f("\"{}\" not found", path));
                }
            }
        } finally {
            mainControlLock.unlock();
        }
    }

    @Override
    public void rmdir(VPath path, boolean recursive) throws VFSIOException {
        /*
        使用 FTP RMD 命令执行删除文件夹操作，经测试ftpCli.removeDirectory行为，有如下：
        当给定位置为空文件夹时，可成功删除，返回true，
        当给定位置为非空文件夹时，不可删除成功，返回false，
        当给定位置为非文件夹时，不可删除成功，返回false，
        当给定位置不存在实体时，不可删除成功，返回false，
        因为vfs的rmdir设定为结果一致性，遂 给定位置不存在实体和删除成功挂等号。
        经测试，发现，符号链接，可通过删除普通文件的方式删除
        20250517 该方法表意已修改，没有结果一致性语义，即若位置本来无一物，则抛出异常，上述挂等号行为取消
        20250517 无需纠正，该方法已修改，没有结果一致性语义,结果一致性与否，下发给下级原生行为
         */
        mainControlLock.lock();
        try {
            if (recursive) {
                FTPFile[] ff = FTPSupport.sneakyRun(() ->
                        this.mainControlCli.listFiles(pathTranslate(path), FTPSupport.NOT_NULL_AND_NOT_MAGIC));
                for (FTPFile f : ff) {
                    if (!f.isDirectory()) {
                        // 发现 符号链接也可被当成普通文件删除，遂该处认为删除普通文件
                        this.rmFile(path.join(f.getName()));
                    } else {
                        this.rmdir(path.join(f.getName()), true);
                    }
                }
                this.rmdir(path, false);
            } else {
                Boolean rmr = FTPSupport.sneakyRun(() ->
                        this.mainControlCli.removeDirectory(pathTranslate(path)));
                if (!rmr) {
                    Optional<VFile> fo = getFile(path);
                    if (fo.isPresent()) {
                        if (fo.get().isDirectory()) {
                            throw new VFSIOException(STF.f("\"{}\" already exists, directory is not empty", path));
                        } else if (!fo.get().isDirectory()) {
                            throw new VFSIOException(STF.f("\"{}\" already exists, not a directory", path));
                        }
                    } else {
                        // 20250517 该方法表意已修改，没有结果一致性语义，即若位置本来无一物，则抛出异常，上述挂等号行为取消
                        // 这里即不存在，但删除标志位rmr又失败的情况
                        throw new VFSIOException(STF.f("\"{}\" not found", path));
                    }
                }
            }
        } finally {
            mainControlLock.unlock();
        }
    }

    @Override
    public Optional<VFile> getFile(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        /*
        cli.mlistFile()方法将使用ftp MLST 命令，已知vsftpd 3.0.5及以下版本不支持该，遂应另谋他法
        https://zh.wikipedia.org/wiki/FTP%E5%91%BD%E4%BB%A4%E5%88%97%E8%A1%A8
        https://stackoverflow.com/questions/52032468/does-vsftpd-supports-mlsd-command
        https://stackoverflow.com/questions/10482204/checking-file-existence-on-ftp-server
         */
        if (path.isVfsRoot()) {
            return Optional.of(new DefaultVFile(this, root, VFileType.directory, 0));
        } else {
            /*
            连接时查看受支持的命令列表，当FTP服务器不支持 MLST 命令时，将降级行为使用 LIST 命令行为，
            要知道 MLST 性能肯定好于 LIST，已知的不支持MLST命令的FTP服务程序至少有vsftpd 3.0.5及以下版本
            实际这里将使用 FTP LIST 命令行为，并且要求不得隐藏隐藏文件的显示，否则该方法的行为将出现异常，
            MLST命令行为简单与该方法表意相同，在此不赘述，当行为为LIST时，即服务器不支持MLST时，该方法的行为即：
            当给定目录处为文件时，返回的数组中将只有一个元素，且该元素名称与path.lastname相同，
            当给定目录处为文件夹时，返回的数组中至少有两个元素，为"."和“.."（该行为仅在显示隐藏的文件的情况下正常）
            当给定的目录处无文件或文件夹时，返回的数组为空数组，
            以上，便可确定三分行为：存在普通文件、存在文件夹、不存在文件或文件夹
             */
            mainControlLock.lock();
            try {
                if (supportMLST) {
                    FTPFile ff = FTPSupport.sneakyRun(() -> this.mainControlCli.mlistFile(pathTranslate(path)));
                    if (ff != null) {
                        return Optional.of(convertF2F(path.back(), ff));
                    } else {
                        return Optional.empty();
                    }
                } else {
                    FTPFile[] ffs = FTPSupport.sneakyRun(() ->
                            this.mainControlCli.listFiles(pathTranslate(path), FTPSupport.NOT_NULL_AND_NOT_LINK));
                    if (ffs.length == 0) {
                        return Optional.empty();
                    } else if (ffs.length == 1) {
                        // 为文件
                        return Optional.of(new DefaultVFile(this, path, VFileType.simpleFile, ffs[0].getSize()));
                    } else {
                        // 因为 显示了隐藏文件，遂文件夹的 "." 和 ".." 包含在内（FTPSupport.NOT_NULL_AND_NOT_LINK不过滤该）
                        // 遂 已知文件夹行为：ffs一定大于等于2， 且一定有 ”." 和 ".."
                        return Optional.of(new DefaultVFile(this, path, VFileType.directory, 0));
                    }
                }
            } finally {
                mainControlLock.unlock();
            }
        }
    }

    @Override
    public InputStream getFileInputStream(VFile file) throws VFSIOException {
        if (file.isSimpleFile()) { // 只有 普通文件可有流，符号链接？ 不要符号链接追实际文件
            BorrowableCliDelegation borCli = awaitBorrowOneFree();
            InputStream in;
            try {
                in = borCli.retrieveFileStream(pathTranslate(file.toPath()));
            } catch (IOException e) {
                // 当到达这里异常时，将之返回空闲并抛出异常
                awaitRecyclingOneBusy(borCli);
                throw new VFSIOException(e.getMessage(), e);
            }
            if (in == null) {
                // 当实体无inputStream，归还该，并抛出异常
                // 这里尚不清楚是否需要调用 completePendingCommand
                // 可能需要更多观察或测试才可确定
                awaitRecyclingOneBusy(borCli);
                throw new VFSIOException(STF.f("\"{}\" not found or not have inputStream", file));
            }
            /*
            经查勘，Apache commons net 的 FTPClient 继承自 FTP类，
            FTP类继承自SocketClient类，FTP类中有定义端口 control port 和 data port
            遂未深入查看其源码，但从该两个端口推断，其从socket引的流，会造成读和写不能同时，遂
            为确保基本的读写同时需求，每个被创建的InputStream将根据FTPClient要求，将其的Complete过程和其流（Socket）
            引用的cli都独立出来，为妥协该过程，每个inputStream将都创建一个FTPClient
            20240918认知更新：在一个FTPClient连接里，FTP标准规定，一个命令请求一个响应，传递文件的STOR命令同样如此，
            遂当通过FTPClient打开一个文件流IO时，还处于相应命令的响应阶段，遂IO流未关闭时，其他命令不会得到响应，
            在FileZilla Server测试后发现，FTP Server会将后续请求的命令暂存至队列，并有日志显示：
            “A command is already being processed. Queuing the new one until the current one is finished.”
            如上，即可证明，Apache commons net 提供的FTPClient并未有问题，其可能实现的是标准FTP协议。
            经测试发现，FileZilla 的 FTP Client 可同时在文件传输时进行其他作业，并可同时传输多个文件，从FileZilla的Server日志和Session连接
            记录中发现其有一个主控Session，每个文件传输都打开了一个新的Session（Server日志中有新Session登录等步骤的日志），遂，
            该VFS打算效仿该行为实现：
            即连接一个主控，其他每次涉及文件传输都会打开另一个FTPClient，若涉及到同时传输，则相应的启动多个Cli。
            经测试，xftp客户端使用的方式同理，同样为一个主控Session，多个传输Session，但与FileZilla有些区别：
            FileZilla的主控为 TYPE I, 传输为 TYPE I, xftp的主控为 TYPE A， 传输为 TYPE I
            TYPE A ASCII模式无法进行非ASCII集的字符编码转化，优先使用TYPE I
             */
            return InputCompleteDelegation.accept(in, borCli, this);
        } else {
            throw new VFSIOException(STF.f("\"{}\" is a directory", file));
        }
    }

    @Override
    public VFile mkFile(VPath path, InputStream newFileData) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        Objects.requireNonNull(newFileData, "given input stream is null");
        if (getFile(path).isPresent()) {
            throw new VFSIOException(STF.f("{}:\"{}\" already exists", this, path));
        }
        BorrowableCliDelegation borCli = null;
        try {
            borCli = awaitBorrowOneFree();
            /*
            该方法 storeFile 完成后 无需 completePendingCommand 因其内部已进行了该操作
            已测试过，若多次进行该 completePendingCommand 操作，则线程会阻塞，似乎等待返回，
            遂应当仅进行一次
            */
            boolean success = borCli.storeFile(pathTranslate(path), newFileData);
            /* fix 20240913 storeFile 方法无需调用 completePendingCommand 因为该方法末尾已自己调用了
             * 若再在外界调用 completePendingCommand，则线程会一直阻塞在此 */
            if (!success) {
                throw new VFSIOException(STF.f("store file not success, unknown error"));
            }
            return getFile(path).orElseThrow(() -> new VFSIOException(STF.f("\"{}\" not found", path)));
        } catch (CopyStreamException ce) {
            log.warn("传输文件时发生IO错误, err msg: {}", ce.getMessage());
            throw new VFSIOException(ce);
        } catch (IOException e) {
            throw new VFSIOException(e.getMessage(), e);
        } finally {
            if (borCli != null) {
                awaitRecyclingOneBusy(borCli);
            }
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                freeQueue.forEach(BorrowableCliDelegation::sneakyClose);
                busyQueue.forEach(BorrowableCliDelegation::sneakyClose);
                this.mainControlCli.logout();
                this.mainControlCli.disconnect();
            } catch (IOException e) {
                // ignore... just log
                log.warn("{} close fail, error msg: {}", this, e.getMessage());
            } finally {
                transformQueueLock.lock();
                try {
                    // 唤醒所有正在等待的，已经结束咧~
                    emptyFree.signalAll();
                } finally {
                    transformQueueLock.unlock();
                }
            }
        }
    }
}
