package io.github.baifangkual.bfk.j.mod.vfs.minio.action;

import io.github.baifangkual.bfk.j.mod.vfs.VFSDefaults;
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
 * minio 原生文件夹行为策略实现<br>
 * MinIO/S3 没有显式的文件夹，需要通过 检查该前缀下是否有对象 来判断“文件夹”是否存在
 *
 * @author baifangkual
 * @since 2024/9/3 v0.0.5
 */
public class NativeDirectoryAction
        extends AbsMinioBucketRootDirectoryAction implements MinioDirectoryAction {
    /**
     * 使用跳表缓存其已创建的空的“文件夹”，能够稍微调整 exists 结果
     */
    private final Set<VPath> nullDirPTempCache = new ConcurrentSkipListSet<>(VPath::compareTo);

    public NativeDirectoryAction(MinioClient cli, String bucket) {
        super(cli, bucket);
        nullDirPTempCache.clear();
    }

    @Override
    public boolean directoryExists(VPath path) throws VFSIOException {
        /*
         * 获取 ls path 的 迭代器，但凡迭代器中有一个元素，都认定该文件夹实际存在，
         * 需注意，该方法也可证明使用 minio API 官方示例中 创建的文件夹是否存在，
         * 在官方api中，创建文件夹行为为创建一个文件夹，其中有同名的文件夹，但ls能看到其内部该同名文件夹指向的仍为外部被创建的文件夹，
         * 遂该引用在ls中可见，即使该文件夹为空文件夹（仅创建，不存放任何）（以该方式认定空文件夹标准）其内部仍有一个可迭代元素，遂在该
         * 情况下，该方法将返回true （该方法应仅能检测文件夹的存在与否，无法检测文件是否存在与否）
         */
        // 列出该前缀下的对象（最多1个）
        Iterable<Result<Item>> results = getCliRef().listObjects(ListObjectsArgs.builder()
                .bucket(getBucket())
                .prefix(MinioPro.listObjPrefixVPathTranslate(path))
                .maxKeys(1)  // 只需要检查是否存在至少一个对象
                .build());
        // 如果结果不为空，说明“文件夹”存在
        boolean hasAtLeastOneNext = results.iterator().hasNext();
        // 调整缓存，因为有元素，从nullDirCache中移除该
        if (hasAtLeastOneNext) {
            nullDirPTempCache.remove(path);
        } // 实际前缀下有文件 或者 是一个空的被创建的
        return hasAtLeastOneNext || nullDirPTempCache.contains(path);
    }

    @Override
    public List<VPath> lsDir(VPath path) throws VFSIOException {
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
                if (decodeName.endsWith(VFSDefaults.PATH_SEPARATOR)) {
                    // 执行至此，即该item属性 isDir为false，且 objectName 以 “/” 结尾，抓到你了小逼崽子，跳过该
                    continue;
                }
            }
            // process get last name
            decodeName = MinioPro.lastName(decodeName);
            // add to coll
            names.add(decodeName);
        }
        // 调整缓存，因为有元素，从nullDirCache中移除该
        if (!names.isEmpty()) {
            nullDirPTempCache.remove(path);
        }
        return names.stream()
                .map(path::join)
                .toList();
    }

    @Override
    public void mkdir(VPath path) throws VFSIOException {
        // 缓存其，存在，但为“空的”“文件夹”
        nullDirPTempCache.add(path);
    }

    @Override
    public void rmDir(VPath path) throws VFSIOException {
        /*
        因为minio的特殊性，删除文件夹内所有内容后，该文件夹也会自行消失，遂仅删除引用即可
        20240906：经测试，发现，通过minio API 官方示例中方式所创建的 “文件夹”，其内部会生成被创建的“文件夹”的同名“文件夹”
        这个生成的内部的同名实体，有如下性质：其在 listObjects 结果中可见，其 listObjects 结果中，ObjectName属性为外侧被创建的“文件夹”的ObjectName，
        其 isDir 属性结果为false，但从minio ui前端可见其为文件夹图标，且前端无法浏览其属性等
        20240906: 经测试，可见，删除通过 minio API创建的“文件夹” 内部 同名的 “文件夹”后，该被创建的”文件夹“将回到minio原始文件夹的行为（即内部有文件便存在，
        内部无文件便消失），也许可以利用这点创建实际的文件夹？
        20240906: 经测试，可知，cli.removeObject行为表达结果一致性
         */
        nullDirPTempCache.remove(path);
    }
}
