package io.github.baifangkual.bfk.j.mod.vfs.minio.action;

import io.github.baifangkual.bfk.j.mod.vfs.minio.MinioDirectoryAction;
import io.minio.MinioClient;
import lombok.Getter;

/**
 * 抽象类，公共提取，以bucket为root的Minio文件夹行为
 *
 * @author baifangkual
 * @since 2024/9/3
 */
public abstract class AbsMinioBucketRootDirectoryAction implements MinioDirectoryAction {

    private final MinioClient cli;
    @Getter
    private final String bucket;

    AbsMinioBucketRootDirectoryAction(MinioClient cli, String bucket) {
        this.cli = cli;
        this.bucket = bucket;
    }

    public MinioClient getCliRef() {
        return cli;
    }

}
