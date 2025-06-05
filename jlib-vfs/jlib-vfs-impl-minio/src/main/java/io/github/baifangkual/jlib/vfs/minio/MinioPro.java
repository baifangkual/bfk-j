package io.github.baifangkual.jlib.vfs.minio;

import io.github.baifangkual.jlib.core.func.FnGet;
import io.github.baifangkual.jlib.core.func.FnRun;
import io.github.baifangkual.jlib.vfs.VFSDefaults;
import io.github.baifangkual.jlib.vfs.VPath;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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
            process.unsafeRun();
            return null;
        });
    }

    public static String listObjPrefixVPathTranslate(VPath path) {
        if (path.isRoot()) return null;
        String har = path.simplePath();
        har = har.substring(VFSDefaults.PATH_SEPARATOR.length());
        return rightAddPathSeparator(har);
    }

    public static String leftAddPathSeparator(String path) {
        return VFSDefaults.PATH_SEPARATOR + path;
    }

    public static String rightAddPathSeparator(String path) {
        return path + VFSDefaults.PATH_SEPARATOR;
    }

    public static String leftCleanPathSeparator(String path) {
        if (path.startsWith(VFSDefaults.PATH_SEPARATOR)) {
            path = path.substring(VFSDefaults.PATH_SEPARATOR.length());
        }
        return path;
    }

    public static String rightCleanPathSeparator(String path) {
        if (path.endsWith(VFSDefaults.PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - VFSDefaults.PATH_SEPARATOR.length());
        }
        return path;
    }

    /**
     * 因minio cli 返回的list object中，objectName包含了多级，遂取其最后一级，纯净构成VPath
     *
     * @param fullCleanedObjectName 以bucket为根的相对路径，形如 “/abc/def/lastName” 其左右斜杠已清理
     * @return lastName
     */
    public static String lastName(String fullCleanedObjectName) {
        int i = fullCleanedObjectName.lastIndexOf(VFSDefaults.PATH_SEPARATOR);
        // 未有，返回同对象
        if (i == -1) return fullCleanedObjectName;
        else {
            int length = VFSDefaults.PATH_SEPARATOR.length();
            return fullCleanedObjectName.substring(i + length);
        }
    }
}
