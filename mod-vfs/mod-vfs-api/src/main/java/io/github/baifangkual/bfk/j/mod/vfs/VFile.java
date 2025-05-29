package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.util.Stf;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import io.github.baifangkual.bfk.j.mod.vfs.mark.VEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * <b>虚拟文件实体</b><br>
 * 一个{@link VPath} 可通过 {@link VPath#toFile()} 方法尝试变为该类型实例，
 * 一旦成功，或者说，该类型实例能够存在即表示在{@link VFS}中对应{@link VPath}所表示的位置存在一个实际文件（文件夹）<br>
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public interface VFile extends VEntity {
    /**
     * 返回该“虚拟文件实体”所对应的“虚拟目录实体”（位置）
     *
     * @return 虚拟目录实体
     */
    VPath toPath();

    /**
     * 实际位置的文件类型
     *
     * @return 文件类型
     */
    VFileType type();

    /**
     * 是否是一个普通文件
     *
     * @return true 是，反之则不是
     */
    default boolean isSimpleFile() {
        return type() == VFileType.simpleFile;
    }

    /**
     * 是否是一个文件夹
     *
     * @return true 是，反之则不是
     */
    default boolean isDirectory() {
        return type() == VFileType.directory;
    }

    /**
     * 获取该虚拟文件实体的字节流inputStream<br>
     *
     * @return 文件字节流
     * @throws VFSIOException 当该实体为文件夹/无字节流/无法获取字节流等时
     */
    default InputStream inputStream() throws VFSIOException {
        //noinspection resource
        return vfs().fileInputStream(this);
    }

    /**
     * 获取该虚拟文件实体的字节流inputStream<br>
     *
     * @return 文件字节流 | 获取文件字节流过程中的异常
     */
    default R<InputStream> tryInputStream() {
        //noinspection resource
        return vfs().tryFileInputStream(this);
    }

    /**
     * 获取该文件实体的名称<br>
     * 例如该文件所在路径为“/abc/def”，则该方法调用将返回"def"，
     * 另外，当该文件实体为根目录时，将返回{@value VFSDefaultConst#PATH_SEPARATOR}
     */
    default String name() {
        return toPath().name();
    }

    /**
     * 该虚拟文件实体实际所占用的字节数<br>
     * 当实体表示为文件夹时，该值依照不同VFS实现有不同表达
     * <ul>
     *     <li>表示其及其内部所有子文件占用的字节数</li>
     *     <li>表其其本身占用的字节数（大部分文件系统都将文件夹占用的字节数表示为0）</li>
     * </ul>
     *
     * @return 所占用字节数
     */
    long sizeOfBytes();

    /**
     * 返回自身的{@link VFS}
     *
     * @return 虚拟文件系统
     */
    VFS vfs();

    /**
     * 给定一个虚拟目录实体，将当前虚拟文件所代指的实体拷贝至给定的虚拟目录实体所在位置<br>
     * 该方法的语义类似于{@code cp -r /path/to/source /path/to/target}<br>
     *
     * @param target 目标虚拟目录实体
     * @throws VFSIOException 当过程发生异常如 给定的虚拟目录实体已存在文件，当拷贝实际实体为文件夹时，给定虚拟目录实体文件夹内部已有同名文件存在等
     * @apiNote 该方法是fail-fast的，且不负责对目标虚拟目录实体的清理、一致性检查等作业。
     * 该方法会阻塞当前线程，遂若调用方已知或预计该要进行的拷贝操作为大作业或长时任务时，
     * 应另起线程进行该方法调用的作业，不应阻塞主线程<br>
     * 20250114：该方法使用时若源为minio 且使用的 MinioDirectoryAction为 MINIO_NATIVE，
     * 则可能造成的情况：将在目标位置拷贝一个空文件夹，该问题后续或修复，使用MINIO_API原生的“文件夹”释义或修改minio实现中VPath2VFile的部分
     * @implNote 该方法的实现应该有一点需要明确:目前该方法的实现方式是朴素且低效的，该方法的过程实际为将两个远端的字节流进行传输，
     * 因为字节流的目标和源都为远端，遂线程对IO流的可读可写唤醒较为频繁，尤其为网络情况不佳时。但有额外可以实现的可行的补救或是优化方法，即若已知source和
     * target作用于同一个vfs上（即同一个远端服务器实体），则可通过vfs实际的依赖的传输协议（smb至少支持）进行如local_copy、local_move等操作直接通过协议本身的
     * 支持进行对文件的拷贝等操作，实际的执行耗时基本靠近本地（vfs指向的）磁盘读写耗时，而非当前实现方案的耗时
     */
    default void copyTo(VPath target) throws VFSIOException {
        if (this.isSimpleFile()) {
            // 若当前为普通文件，并且给定的vPath为root，则这种非法情况应当抛出异常
            if (target.isRoot()) {
                throw new VFSIOException(Stf.f("copy {}:\"{}\" to {}:\"{}\" fail, IOErr: " +
                                               "cannot overwrite directory '{}' with non-directory '{}'",
                        this.vfs(), this.toPath(), target.vfs(), target, target, this.toPath()));
            }
            try (InputStream input = this.inputStream()) {
                target.mkFile(input);
            } catch (IOException e) {
                throw new VFSIOException(
                        Stf.f("copy {}:\"{}\" to {}:\"{}\" fail, IOErr: {}",
                                this.vfs(),
                                this.toPath(),
                                target.vfs(),
                                target,
                                e.getMessage()), e);
            }
        } else if (this.isDirectory()) {
            VFile targetDir = target.mkDir();
            //noinspection resource
            List<VFile> sourceItems = this.vfs().lsDir(this);
            for (VFile sourceOne : sourceItems) {
                // 这里last不会异常，因为该为source的子级，遂一定不为root
                String sLast = sourceOne.toPath().name();
                VPath targetOne = targetDir.toPath().join(sLast);
                // dfs copy
                sourceOne.copyTo(targetOne);
            }
        } else {
            throw new UnsupportedOperationException(Stf.f("尚未实现的类型的拷贝操作, 类型: {}", this.type()));
        }
    }
}
