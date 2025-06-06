package io.github.baifangkual.jlib.vfs.minio;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.vfs.*;
import io.github.baifangkual.jlib.vfs.exception.IllegalVFSBuildParamsException;
import io.github.baifangkual.jlib.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;
import io.github.baifangkual.jlib.vfs.impl.AbstractVirtualFileSystem;
import io.github.baifangkual.jlib.vfs.impl.DefaultSliceAbsolutePath;
import io.github.baifangkual.jlib.vfs.impl.DefaultVFile;
import io.github.baifangkual.jlib.vfs.minio.action.FileFlagDirectoryAction;
import io.github.baifangkual.jlib.vfs.minio.action.MinioAPIDirectoryAction;
import io.github.baifangkual.jlib.vfs.minio.action.NativeDirectoryAction;
import io.github.baifangkual.jlib.vfs.minio.conf.MinioCfgOptions;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static io.github.baifangkual.jlib.vfs.minio.conf.MinioCfgOptions.*;

/**
 * minio 虚拟文件系统实现，该实现线程安全，该线程安全性依赖{@link MinioClient},
 * <a href="https://github.com/minio/minio-java/issues/975">依据</a><br>
 * 该实现的构造函数所需的配置类配置在 {@link MinioCfgOptions}, 可参阅其<br>
 * <b>minio的已知特性</b>
 * <ul>
 *     <li>bucket名不得包含大写字母，下划线等，中划线可包括，应该也不可中文</li>
 *     <li>minio有用户概念，sdk api 的访问认证可使用 user 和 passwd，也可使用 acc key & sec key 形式</li>
 *     <li>一个user 可创建多个 acc key & sec key 密钥对</li>
 *     <li>若bucket的Access Policy为public，则可不提供认证</li>
 *     <li>minio无所谓“文件夹”概念，“多级”以“前缀”表示，遂minio不会存在“空文件夹”的情况</li>
 *     <li>putObj时，未知大小的流，使用partSize范围必须在5MiB~5GiB之间</li>
 *     <li>已测试可以向一个桶的同级创建同名文件与文件夹，但会导致minio的一些怪异表现，后创建的同名文件夹再minioUI不可见，但其内部的目录数据结构却有其内容，
 *     表现出来似乎将先创建的同名文件覆盖了，但能从其UI下载到同名文件字节，遂同名文件也存在</li>
 * </ul>
 * <p>设定该VFS不允许同目录出现同名文件与文件夹（一是因为minio这种情况的行为怪异，不确定其健壮，二是因为大部分文件系统都不允许该情况存在）<br>
 * <b>因minio无文件夹概念，遂该虚拟文件系统以某种形式模拟文件夹，可有如下方法释义</b><br>
 * <b>方式一 使用原生minio/s3的文件夹概念</b>
 * <ul>
 *     <li>文件夹行为实现类为 {@link NativeDirectoryAction}</li>
 *     <li>没有显式的“文件夹”概念，需要通过 检查该前缀下是否有对象 来判断“文件夹”是否存在</li>
 *     <li>若检查某前缀下有对象，则表示该“前缀”所在位置有一个“文件夹”</li>
 * </ul>
 * <p>方式一不会有冗余，且为原生minio设定，但可能对部分VFS行为造成难以理解的影响<br>
 * <p>方式一的实现{@link NativeDirectoryAction}已经重写了完全遵照minio/s3的模拟设定，
 * 为防止出现使用{@link #mkdir(VPath)}后再使用{@link #exists(VPath)}发现“文件夹”即创建了又不存在的问题，
 * 该Action内缓存了“被创建的minio空文件夹”。虽该minio实现之前业务有使用过并优化过，
 * 但自从这次重写{@link NativeDirectoryAction}后还未深度使用过，
 * 遂后续深度使用可能会发现部分问题导致VFS的API的约束与保证无法实现等，那时应将现象记录并修复bug<br>
 * <b>方式二 使用冗余文件标识目录（未实现且已废弃）</b>
 * <ul>
 *     <li>文件夹行为实现类为 {@link FileFlagDirectoryAction}</li>
 *     <li>创建文件夹时，将创建一个带有固定文件名的文件在文件夹下，作为标记，也为冗余，该冗余文件名及其内容固定</li>
 *     <li>删除文件夹时，将删除该冗余文件，根据minio的默认行为，该文件夹便被”删除“了</li>
 *     <li>判断文件夹是否存在等时，将list文件夹并找固定的冗余文件，若无冗余文件且文件夹内实际已有其他元素，则创建该冗余文件</li>
 *     <li>随使用，桶将会越发冗余，所有实例行为将往创建冗余文件表示文件夹的形式靠拢</li>
 * </ul>
 * <p>方式二将创建冗余文件表示文件夹，对minio的使用方式偏离minio设定，但更靠近fileSystem设想。并且，这种行为的理想使用条件是整个桶均由该vfs管理才可<br>
 * <b>方式三 使用Minio提供的API的默认文件夹行为（未实现）</b>
 * <ul>
 *     <li>文件夹行为实现类为 {@link MinioAPIDirectoryAction}</li>
 *     <li>经测试，minio提供的API的默认的文件夹行为会在使用创建文件夹（IO0字节情况，prefix以斜杠结尾）时，创建一个文件夹，该被创建的文件夹
 *     下有一个与文件夹同名的实体，该实体行为较怪异，但假设将这种情况设定为文件夹实际存在，则其行为也是清晰的，该方式与方式二类似，同样的，缺点也同样：
 *     这个桶必须要由VFS管理</li>
 * </ul>
 * 默认文件夹行为配置在 {@link MinioCfgOptions#dirActionStrategy}<p>
 *
 * @author baifangkual
 * @since 2024/9/2 v0.0.5
 */
@Slf4j
public class MinioBucketRootVirtualFileSystem extends AbstractVirtualFileSystem implements VFS {

    private static final long MINIO_API_MIN_BYTE_BUF_SIZE = 5 * 1024 * 1024;

    private final String host;
    private final int port;
    private final MinioClient cli;
    private final String bucket;
    private final VPath root;
    /**
     * A valid part size is between 5MiB to 5GiB (both limits inclusive)，必须在该区间内
     */
    private final long putObjectBufSize;
    private final MinioDirectoryAction directoryAction;
    private final AtomicBoolean isClosed = new AtomicBoolean(true);


    /**
     * 该构造应为所有子类型统一构造的入口，在该构造内，子类型有权校验及变更给定的配置,
     * 可能会影响给定的参数
     *
     * @param cfg 外界传递的vfs连接参数
     * @throws VFSBuildingFailException 当构造过程发生异常时，应由该包装或抛出
     */
    public MinioBucketRootVirtualFileSystem(Cfg cfg) throws VFSBuildingFailException {
        super(cfg);
        final Cfg readonlyCfg = readonlyCfg();
        String accKey = readonlyCfg.tryGet(accessKey).orElse(null);
        String secKey = readonlyCfg.tryGet(secretKey).orElse(null);
        String host = readonlyCfg.get(MinioCfgOptions.host);
        int port = readonlyCfg.getOrDefault(MinioCfgOptions.port);
        boolean useHttps = readonlyCfg.getOrDefault(useHttpsSecure);
        MinioClient.Builder CliBuilder = MinioClient.builder()
                .endpoint(host, port, useHttps);
        if (accKey != null && secKey != null) {
            CliBuilder.credentials(accKey, secKey);
        }
        final MinioClient oCli = CliBuilder.build();
        String buckN = readonlyCfg.get(MinioCfgOptions.bucket);
        boolean err = false;
        try {
            boolean bkE = MinioPro.checkBucketExists(oCli, buckN);
            if (bkE) {
                // process after...
                this.host = host;
                this.port = port;
                this.bucket = buckN;
                this.directoryAction = buildingDirAction(readonlyCfg.getOrDefault(dirActionStrategy), oCli, buckN);
                this.putObjectBufSize = safeBufRange(readonlyCfg.getOrDefault(bufSize));
                this.root = new DefaultSliceAbsolutePath(this, VFSDefaults.PATH_SEPARATOR);
                this.cli = oCli;
                // 目录行为策略
                this.isClosed.compareAndSet(true, false);
            } else {
                throw new IllegalVFSBuildParamsException(Stf.f("bucket \"{}\" not exists", buckN));
            }
        } catch (Exception e) {
            err = true;
            log.warn("bucket \"{}\" not exists, minio client closing...", buckN, e);
            throw new VFSBuildingFailException(e);
        } finally {
            if (err) {
                try {
                    oCli.close();
                    log.warn("minio client closed");
                } catch (Exception e) {
                    log.error("error closing MinioClient", e);
                }
            }
        }

    }

    private long safeBufRange(long unsafeBufSize) {
        if (unsafeBufSize < MINIO_API_MIN_BYTE_BUF_SIZE) {
            if (log.isDebugEnabled()) {
                log.debug("minio API 要求bufSize不能小于5MiB，当前配置值实际小于该值，将使用最小值5MiB");
            }
            return MINIO_API_MIN_BYTE_BUF_SIZE;
        } else {
            return unsafeBufSize;
        }
    }

    private MinioDirectoryAction buildingDirAction(MinioDirectoryActionStrategy strategy, MinioClient cli, String bucket) {
        return switch (strategy) {
            case MINIO_NATIVE -> new NativeDirectoryAction(cli, bucket);
            case MINIO_API, FILE_FLAG ->
                    throw new UnsupportedOperationException("不支持的操作，尚未实现该类型的目录行为策略");
        };
    }


    @Override
    public boolean exists(VPath path) throws VFSIOException {
        if (path.isRoot()) {
             /*
             to do 如果 path 为 root 则应该使用 bucketExists
             不能为root做操作，遂直接true
             return MinioPro.checkBucketExists(cli, bucket);
              */
            return true;
        } else {
            Optional<StatObjectResponse> statObjectResponse = objStat(path);
            if (statObjectResponse.isPresent()) {
                return true;
            } else {
                return directoryAction.directoryExists(path);
            }
        }
    }

    /**
     * 给定一个路径，返回该路径所表示的文件的相关信息，当给定目录所指定的位置不存在文件时，该返回{@link Optional#empty()}，
     * 即 option[some] 表示文件存在 ，option[none] 表示文件不存在，当抛出异常，表示其他情况，该方法仅可查询文件状态，文件夹不可用
     */
    private Optional<StatObjectResponse> objStat(VPath path) {
        try {
            StatObjectResponse stat = MinioPro.sneakyRun(() -> cli.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(MinioPro.leftCleanPathSeparator(path.simplePath()))
                    .build()));
            return Optional.of(stat);
        } catch (ErrorResponseRuntimeException e) {
            String errMsg = e.getErrorResponse().message();
            if (!"Object does not exist".equals(errMsg)) {
                throw e;
            }
        }
        return Optional.empty();
    }


    @Override
    public VFSType type() {
        return VFSType.minio;
    }

    @Override
    public VPath root() {
        return root;
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public List<VPath> lsDir(VPath path) throws VFSIOException {
        Optional<VFile> file = file(path);
        if (file.isPresent()) {
            VFile vf = file.get();
            if (vf.isDirectory()) {
                return directoryAction.lsDir(path);
            } else {
                throw new VFSIOException(Stf.f("Not a directory: {}", path));
            }
        } else {
            throw new VFSIOException(Stf.f("\"{}\" not exists", path));
        }
    }

    @Override
    public VFile mkdir(VPath path) throws VFSIOException {
        /*
        20241107 fix 如果为root，即已经存在，又因为该方法的策略为确保结果一致性，
        遂当self为root时，跳过创建目录的过程即可
        20250517 该方法已取消结果一致性保证，方法语义更严格，遂若为root，抛出异常，因为root一定已经存在了
         */
        Objects.requireNonNull(path, "given path is null");
        if (path.isRoot()) {
            throw new VFSIOException("directory already exists");
        }
        // 实体存在
        Optional<StatObjectResponse> statObjectResponse = objStat(path);
        if (statObjectResponse.isPresent()) {
            throw new VFSIOException(Stf.f("位置已存在同名文件, bucket: '{}', path: '{}'", this.bucket, path.simplePath()));
        }
        // 文件夹存在
        if (directoryAction.directoryExists(path)) {
            throw new VFSIOException(Stf.f("Directory already exists, bucket: '{}', path: '{}'", this.bucket, path.simplePath()));
        }
        // 创建其
        directoryAction.mkdir(path);
        // 勉强 可能逻辑重复冗余, 或直接创建VFile更好？
        return file(path).orElseThrow(() -> new VFSIOException(Stf.f("\"{}\" not exists", path)));
    }

    @Override
    public void rmFile(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        Optional<VFile> f = path.toFile();
        if (f.isPresent()) {
            if (f.get().isDirectory()) {
                throw new VFSIOException(Stf.f("\"{}\" is a directory", path));
            } else if (f.get().isSimpleFile()) {
                doRemoveObj(MinioPro.leftCleanPathSeparator(path.simplePath()));
            } else {
                throw new VFSIOException(Stf.f("\"{}\" is not a directory or simple file", path));
            }
        } else {
            // 20250517 该方法表意已修改，没有结果一致性语义，即若位置本来无一物，则抛出异常
            // 20250517 无需纠正，该方法已修改，没有结果一致性语义,结果一致性与否，下发给下级原生行为
            throw new VFSIOException(Stf.f("\"{}\" not exists", path));
        }
    }

    @Override
    public void rmdir(VPath path, boolean recursive) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        if ((!recursive) && (!lsDir(path).isEmpty())) {
            throw new VFSIOException(Stf.f("\"{}\" is not a empty directory", path));
        }
        if (recursive) {
            rmChildren(path, recursive);
        }
        // fix me to do 应检测这里的删除行为，应该使其能明确区分两种行为
        //  删除自身：要求必须为空文件夹，则可以删除自身
        //  删除自身及子，递归删除
        rmSelf(path);
        // 20250517 minio似乎不容易做到“本不存在，所以抛出异常“行为，遂这里不追加“本不存在，所以抛出异常“行为
        // 20250517 无需纠正，该方法已修改，没有结果一致性语义,结果一致性与否，下发给下级原生行为
    }

    private void rmChildren(VPath path, boolean recursive) throws VFSIOException {
        /*
        这里未使用 exclude 的，因为要完整删除，遂应当删除遇见的一切
         */
        Iterable<Result<Item>> itR = lsInner(path, recursive);
        List<String> itlDelNames = new LinkedList<>();
        for (Result<Item> ir : itR) {
            Item item = MinioPro.sneakyRun(ir::get);
            if (!item.isDir()) {
                /*
                因为通过api创建的”文件夹“其内部的同名标志实体的isDir属性为false，遂这里也可找到，无需担心
                 */
                itlDelNames.add(item.objectName());
            }
        }
        doRemoveObjs(itlDelNames);
    }

    /**
     * 经过该，可知其 cli.listObjects行为：添加 / 在 其后，能够查询到其内部元素，
     * 若无 / 在其后，则 若实体为文件，则可查询到其 迭代器有1-N个文件，若为文件夹 ，则可查询到其，迭代器有1~N个文件，这些文件为实体的表示，
     * 若有 / 在其后，则 若实体为文件夹，则迭代器中有1-N个文件，若实体为文件，则迭代器中有0个文件，
     * 通过其参数prefix也可知，因为为通过前缀匹配，且minio无文件夹概念，遂这样以 不以 / 结束的目录表示，能够查询到 有 给定 prefix 的实体，
     * 所以在以无 / 结尾的 prefix中 能够查询到 1~n个实体，表示的为含有这个前缀的实体
     *
     * @param path      给定一个目录，该目录为相对 minio bucket 的相对目录
     * @param recursive 是否递归查询
     * @return 迭代器，当中可能有内容，当给定的目录实体为minio中实际存在的文件时，该迭代器中无元素，
     * 当给定的目录实体为minio中实际存在的“文件夹时”该给定的迭代器中一定有元素，当给定的目录实体在minio中不存在时，该迭代器中无元素，
     * 综上，即，当返回的迭代器中有元素时，表示该目录实体为“文件夹”，当返回的迭代器中无元素时，表示该目录实体可能为minio上实际存在的Obj或是不存在
     */
    private Iterable<Result<Item>> lsInner(VPath path, boolean recursive) {
        return cli.listObjects(ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(MinioPro.listObjPrefixVPathTranslate(path))
                .recursive(recursive)
                .build());
    }

    private void doRemoveObj(String realMinioObjectName) {
        // do delete
        MinioPro.sneakyRun(() -> cli.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(MinioPro.leftCleanPathSeparator(realMinioObjectName))
                .build()));
    }

    private void doRemoveObjs(Iterable<String> realMinioObjNames) {
        List<DeleteObject> dl = StreamSupport.stream(realMinioObjNames.spliterator(), false)
                .map(MinioPro::leftCleanPathSeparator)
                .map(DeleteObject::new)
                .toList();
        Iterable<Result<DeleteError>> dlIt = cli.removeObjects(RemoveObjectsArgs.builder()
                .bucket(bucket)
                .objects(dl)
                .build());
        for (Result<DeleteError> r : dlIt) {
            DeleteError de = MinioPro.sneakyRun(r::get);
            log.warn("error in deleting object: {}, error message: {}", de.objectName(), de.message());
        }
    }

    private void rmSelf(VPath path) throws VFSIOException {
        /*
        因为minio的特殊性，删除文件夹内所有内容后，该文件夹也会自行消失，遂仅删除引用即可
        20240906：经测试，发现，通过minio API 官方示例中方式所创建的 “文件夹”，其内部会生成被创建的“文件夹”的同名“文件夹”
        这个生成的内部的同名实体，有如下性质：其在 listObjects 结果中可见，其 listObjects 结果中，ObjectName属性为外侧被创建的“文件夹”的ObjectName，
        其 isDir 属性结果为false，但从minio ui前端可见其为文件夹图标，且前端无法浏览其属性等
        20240906: 经测试，可见，删除通过 minio API创建的“文件夹” 内部 同名的 “文件夹”后，该被创建的”文件夹“将回到minio原始文件夹的行为（即内部有文件便存在，
        内部无文件便消失），也许可以利用这点创建实际的文件夹？
        20240906: 经测试，可知，cli.removeObject行为表达结果一致性
         */
        directoryAction.rmDir(path);
    }


    @Override
    public Optional<VFile> file(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        if (path.isRoot()) {
            return Optional.of(new DefaultVFile(this, root, VFileType.directory, 0));
        }
        Optional<StatObjectResponse> statOpt = objStat(path);
        if (statOpt.isPresent()) {
            StatObjectResponse stat = statOpt.get();
            return Optional.of(new DefaultVFile(this, path, VFileType.simpleFile, stat.size()));
        } else {
            if (directoryAction.directoryExists(path)) {
                return Optional.of(new DefaultVFile(this, path, VFileType.directory, 0));
            }
        }
        return Optional.empty();
    }

    @Override
    public InputStream fileInputStream(VFile file) throws VFSIOException {
        if (file.isDirectory()) {
            throw new VFSIOException(Stf.f("\"{}\" is a directory", file));
        } else if (file.isSimpleFile()) {
            return MinioPro.sneakyRun(() -> cli.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(MinioPro.leftCleanPathSeparator(file.toPath().simplePath()))
                    .build()));
        } else {
            throw new VFSIOException(Stf.f("Not a simple file or directory: '{}'", file));
        }
    }

    @Override
    public VFile mkFile(VPath path, InputStream newFileData) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        Objects.requireNonNull(newFileData, "given input stream is null");
        /*
        statObj 存在，证明已存在同名文件，因minio的文件夹性质，遂无需检查文件夹是否实际存在
        但这里有一个问题，当该path所指定的地方已有文件夹存在，则其应以何种方式响应该 mkFile？
        一种是先查看 其是否已删除？在 NativeDirectoryAction中？这样的话，每次创建都太繁琐，不应
        另一种是 listObject查看其内部是否已有文件，当其内部已有文件时，则证明其已存在，但该操作涉及到查询文件夹内详情，遂可能反应较为繁琐,
        但listObject查看其内部的方式较为健全，遂使用第二种方式
         */
        Err.realIf(objStat(path).isPresent(), VFSIOException::new, "\"{}\" 已存在文件，无法创建", path);
        //to do fix me 使用第二种方式查看其，查看其内部是否有文件以确定其是否真的存在
        Err.realIf(directoryAction.directoryExists(path),
                () -> new VFSIOException(Stf.f("\"{}\" 已存在文件夹，无法创建同名文件", path)));
        MinioPro.sneakyRun(() -> cli.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(MinioPro.leftCleanPathSeparator(path.simplePath()))
                .stream(newFileData, -1, putObjectBufSize)
                .build()));
        return path.toFile().orElseThrow(() -> new VFSIOException(Stf.f("\"{}\" not exists", path)));
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                cli.close();
            } catch (Exception e) {
                throw new MinioExceptionRuntimeWrap(e);
            }
        }
    }

}
