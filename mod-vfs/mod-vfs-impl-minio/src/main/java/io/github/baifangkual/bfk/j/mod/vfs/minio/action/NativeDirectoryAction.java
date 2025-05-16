package io.github.baifangkual.bfk.j.mod.vfs.minio.action;

import io.github.baifangkual.bfk.j.mod.vfs.VFSDefaultConst;
import io.github.baifangkual.bfk.j.mod.vfs.VPath;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import io.github.baifangkual.bfk.j.mod.vfs.minio.MinioDirectoryAction;
import io.github.baifangkual.bfk.j.mod.vfs.minio.MinioPro;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * minio 原生文件夹行为策略实现, 假装文件夹存在或被删除
 *
 * @author baifangkual
 * @since 2024/9/3 v0.0.5
 */
public class NativeDirectoryAction
        extends AbsMinioBucketRootDirectoryAction implements MinioDirectoryAction {
    /**
     * 使用跳表缓存其已删除的，能够稍微调整 exists结果
     */
    private final Set<VPath> rmCache = new ConcurrentSkipListSet<>(VPath::compareTo);

    public NativeDirectoryAction(MinioClient cli, String bucket) {
        super(cli, bucket);
        rmCache.clear();
    }

    @Override
    public boolean directoryExists(VPath path) throws VFSIOException {
        /*
        当逻辑走到该处，证明已确定该位置不存在文件
         */
        return !rmCache.contains(path);
    }

    @Override
    public List<VPath> lsDir(VPath path, String... excludeNames) throws VFSIOException {
        /*
         * minio该查询，行为：
         * 假设查询 bucketName/dir01 文件夹下元素，prefix必须以 / 结尾，才可看到dir01中元素，即应使用 dir01/
         * 若使用为 dir01作为 prefix，则显示的元素只有一个，为dir01本身的属性，无意义
         * 对minio中的文件实体使用该方法则返回文件本身元素，无意义
         * 遂判断给定path是否为文件，应在上层拦截判断之
         * 还有，返回的元素编码由url编码，应解码之，statObj则无需解码
         * 查询出的元素Item其ObjName中，文件，则为完整文件名，文件夹，则以 / 结尾 需处理
         */
        Iterable<Result<Item>> itR = getCliRef().listObjects(ListObjectsArgs.builder()
                .bucket(getBucket())
                .prefix(MinioPro.listObjPrefixVPathTranslate(path))
                .recursive(false)
                .build());

        /*
        以原生方式，大多没有，遂使用0
        20240906 update ：linklist更优
         */
        List<String> names = new LinkedList<>();
        for (Result<Item> it : itR) {
            Item i = MinioPro.sneakyRun(it::get);
            /*
            无需从url解码，因为其objectName方法会自己解码，而不像其toString结果那样未解码
            String urlEncodeName = i.objectName();
            String decodeName = URLDecoder.decode(urlEncodeName, StandardCharsets.UTF_8);
            */
            String decodeName = i.objectName();
            // fix me 20241021 这里的逻辑有待优化，可能会有问题,
            //  通过API创建的空文件夹本身可以被扫描，但空文件夹的标志位(既非文件也非文件夹)不应被扫描，否则会报非法参数""。
            //  所以此类NativeDirectoryAction本身的逻辑不符合Minio的后端逻辑，即无法扫描到标志位
            // fix ed 20241021 为了兼容性，并且，minio本身的cli通过API特殊形式创建的“空文件夹”本身也应当算作minio的原生行为的一部分
            //  遂该处应兼容该情况，兼容性逻辑：
            //  已知minio通过Cli的API新建的文件夹的逻辑：在类 MinioAPIDirectoryAction 上有说明，
            //  遂该处仅需判断 当其isDir属性为false的情况下objectName是否以"/"结尾即可
            boolean isDir = i.isDir();
            if (isDir) {
                decodeName = MinioPro.rightCleanPathSeparator(decodeName);
            } else {
                // fix ed 20241021 minio cli API 创建的异常实体的判断
                //  该else内为 minio cli API 创建的文件夹内部的非文件夹和非文件的实体的判断逻辑
                //  该异常的标志实体的行为：isDir属性为 false，并且 objectName以 / 结尾，并且其 objectName eq path full
                if (decodeName.endsWith(VFSDefaultConst.PATH_SEPARATOR)) {
                    // 执行至此，即该item属性 isDir为false，且 objectName 以 “/” 结尾，抓到你了小逼崽子，跳过该
                    continue;
                }
            }
            // process get last name
            decodeName = MinioPro.lastName(decodeName);
            // add to coll
            names.add(decodeName);
        }
        MinioPro.doExcludeNames(names, excludeNames);
        return names.stream()
                .map(path::join)
                .toList();
    }

    @Override
    public void mkdir(VPath path) throws VFSIOException {
        /*
        当逻辑走到该处，证明该位置不存在文件
        直接将其从rmCache中移除即可
         */
        rmCache.remove(path);
    }

    @Override
    public void rmDir(VPath path) throws VFSIOException {
        /*
        当逻辑走到该处，证明该位置不存在文件
        将该添加到cache即可
         */
        rmCache.add(path);
    }
}
