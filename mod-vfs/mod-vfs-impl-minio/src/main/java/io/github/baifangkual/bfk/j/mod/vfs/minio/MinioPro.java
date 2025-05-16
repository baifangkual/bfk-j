package io.github.baifangkual.bfk.j.mod.vfs.minio;

import io.github.baifangkual.bfk.j.mod.core.func.FnGet;
import io.github.baifangkual.bfk.j.mod.core.func.FnRun;
import io.github.baifangkual.bfk.j.mod.vfs.VFSDefaultConst;
import io.github.baifangkual.bfk.j.mod.vfs.VPath;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * minio相关api工具类
 *
 * @author baifangkual
 * @since 2024/9/2
 */
public class MinioPro {
    private MinioPro() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * 检查给定的桶是否存在，当不存在，则返回false，该方法不会关闭给定的AutoClose对象
     *
     * @param cli    minioClient ，要求已连接
     * @param bucket 桶名称
     * @return bool
     */
    static boolean checkBucketExists(MinioClient cli, String bucket) {
        return sneakyRun(() -> cli.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()));
    }

    /**
     * 包装 minio 各项操作的预检异常，将其包装为运行时异常，对于常见异常，给定了各项简短说明，
     * 该方法将进行函数的执行，函数允许抛出预检异常，详见{@link FnGet} 说明
     *
     * @param process 操作流程
     * @param <R>     返回值类型
     * @return 执行操作的结果
     */
    public static <R> R sneakyRun(FnGet<? extends R> process) {
        try {
            return process.unsafeGet();
        } catch (ErrorResponseException e) {
            throw new ErrorResponseRuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new VFSIOException("inputStream没有足够数据", e);
        } catch (InternalException e) {
            throw new IllegalStateException("minio内部库错误", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("HMAC SHA-256库丢失", e);
        } catch (InvalidResponseException e) {
            throw new IllegalStateException("S3服务返回无效或无错误响应", e);
        } catch (IOException e) {
            throw new VFSIOException("S3操作上的I/O错误", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5或SHA-256摘要库丢失", e);
        } catch (ServerException e) {
            throw new IllegalStateException("minioHTTP服务器错误", e);
        } catch (XmlParserException e) {
            throw new MinioExceptionRuntimeWrap("XML解析错误", e);
        } catch (Exception e) {
            throw new MinioExceptionRuntimeWrap(e);
        }
    }

    public static void sneakyRun(FnRun process) {
        sneakyRun(() -> {
            process.run();
            return null;
        });
    }

    public static String listObjPrefixVPathTranslate(VPath path) {
        if (path.isRoot()) return null;
        String har = path.simplePath();
        har = har.substring(VFSDefaultConst.PATH_SEPARATOR.length());
        return rightAddPathSeparator(har);
    }

    public static String leftAddPathSeparator(String path) {
        return VFSDefaultConst.PATH_SEPARATOR + path;
    }

    public static String rightAddPathSeparator(String path) {
        return path + VFSDefaultConst.PATH_SEPARATOR;
    }

    public static String leftCleanPathSeparator(String path) {
        if (path.startsWith(VFSDefaultConst.PATH_SEPARATOR)) {
            path = path.substring(VFSDefaultConst.PATH_SEPARATOR.length());
        }
        return path;
    }

    public static String rightCleanPathSeparator(String path) {
        if (path.endsWith(VFSDefaultConst.PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - VFSDefaultConst.PATH_SEPARATOR.length());
        }
        return path;
    }

    /**
     * 给定一个可变列表和一个要过滤的名称列表，当方法执行结束后，可变列表中的属于要过滤的名称列表的元素将被过滤，
     * 当给定的要过滤的列表为null或为empty时，直接返回，
     * 当参数excludeNames中描述的元素都已被过滤时，直接返回，
     * 当给定的可变列表中无元素时，直接返回，
     * 该方法最多创建一个List对象，时间复杂度O(nm)
     *
     * @param refMutableList 可变列表，要操作该，要求该的迭代器remove方法可用
     * @param excludeNames   要从可变列表中删除的元素列表
     * @throws IllegalStateException 当给定的可变列表不可变或其迭代器remove方法不可用时
     */
    public static void doExcludeNames(List<String> refMutableList, String... excludeNames) throws IllegalStateException {

        if (excludeNames == null
            || excludeNames.length == 0
            || refMutableList.isEmpty()) return;
        List<String> excL;
        Iterator<String> it = refMutableList.iterator();
        if (!it.hasNext()) {
            return;
        } else {
            excL = Arrays.asList(excludeNames);
        }
        try {
            while (it.hasNext()) {
                /*
                不可行，因为Arrays.asList方法返回的List为Arrays类内部的ArrayList，该List未实现其remove方法
                if (excL.isEmpty()) {
                    break;
                }
                */
                String i = it.next();
                if (excL.contains(i)) {
                    it.remove();
                    /*
                    不可行，因为Arrays.asList方法返回的List为Arrays类内部的ArrayList，该List未实现其remove方法
                    excL.remove(i);
                    */
                }
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 因minio cli 返回的list object中，objectName包含了多级，遂取其最后一级，纯净构成VPath
     *
     * @param fullCleanedObjectName 以bucket为根的相对路径，形如 “/abc/def/lastName” 其左右斜杠已清理
     * @return lastName
     */
    public static String lastName(String fullCleanedObjectName) {
        int i = fullCleanedObjectName.lastIndexOf(VFSDefaultConst.PATH_SEPARATOR);
        // 未有，返回同对象
        if (i == -1) return fullCleanedObjectName;
        else {
            int length = VFSDefaultConst.PATH_SEPARATOR.length();
            return fullCleanedObjectName.substring(i + length);
        }
    }
}
