package io.github.baifangkual.jlib.vfs;


import io.github.baifangkual.jlib.core.lang.R;
import io.github.baifangkual.jlib.core.lang.Tree;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;
import io.github.baifangkual.jlib.vfs.mark.VEntity;

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
     * 另外，当该文件实体为根目录时，将返回{@value VFSDefaults#PATH_SEPARATOR}
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
     * 返回该文件实体代表的文件夹下的文件实体<br>
     * 若返回的{@link List}为{@link List#isEmpty()}，则表示该目录为空目录
     *
     * @return 该文件实体代表的文件夹下的文件实体
     * @throws VFSIOException 当过程出现异常（如权限不足，给定路径不存在等），或当前实体不为文件夹时
     */
    default List<VFile> lsDir() throws VFSIOException {
        //noinspection resource
        return this.vfs().lsDir(this);
    }

    /**
     * 给定一个虚拟文件实体，返回该文件实体代表的文件夹下的文件实体<br>
     * 若返回的{@link List}为{@link List#isEmpty()}，则表示该目录为空目录
     *
     * @return {@link R.Ok}载荷该文件实体代表的文件夹下的文件实体 | {@link R.Err}载荷过程中出现的异常
     * @apiNote 当过程出现异常（如权限不足，给定路径不存在等）或{@link VFile}所在位置的实体为文件时，
     * 返回 {@code R.Err(...)}，其中携带的异常即原因
     */
    default R<List<VFile>> tryLsDir() {
        return R.ofFnCallable(this::lsDir);
    }


    /**
     * 给定虚拟目录实体，将当前虚拟文件实体所代指的实体拷贝至虚拟目录实体所代指的位置<br>
     * 该方法的语义在以下条件等同于 {@code cp -r /path/to/src /path/to/dst} :
     * <p>{@code dst} 不存在，{@code dst} 的上级目录已存在</p>
     * 该方法成功（不抛出异常）的结果:
     * <p>若 {@code src} 为文件夹，则 {@code dst} 为文件夹，
     * 且 {@code src} 内实体都递归的拷贝到 {@code dst} 内，
     * 若 {@code src} 为文件，则 {@code dst} 将为文件</p>
     * <pre>
     *     {@code
     *     VFile src = ...;            // (smb:/path/foo/a) (exists)
     *     VPath dst = ...;            // (ftp:/path/bar/b) (not exists)
     *     src.copyTo(dst);
     *     // smb:/path/foo/a                      smb:/path/foo/a
     *     //               ↓ (rename: a -> b)  →
     *     // ftp:/path/bar/                       ftp:/path/bar/b
     *     -------------------------------------------------------
     *     VFile src = ...;            // (ftp:/path/foo/a) (exists)
     *     VPath dst = ...;            // (smb:/path/bar)   (exists)
     *     dst = dst.join(src.name()); // (smb:/path/bar/a) (not exists)
     *     src.copyTo(dst);
     *     // ftp:/path/foo/a                      ftp:/path/foo/a
     *     //               ↓          →
     *     // smb:/path/bar/                       smb:/path/bar/a
     *     }
     * </pre>
     *
     * @param dst 目标-虚拟目录实体
     * @throws VFSIOException       当过程发生异常如 给定的虚拟目录实体已存在文件，当拷贝实际实体为文件夹时，
     *                              给定虚拟目录实体文件夹内部已有同名文件存在等
     * @throws NullPointerException 当给定的目标为 {@code null} 时
     * @apiNote 该方法是fail-fast的，且不负责对目标虚拟目录实体的清理、一致性检查等作业。
     * 该方法会阻塞当前线程，遂若调用方已知或预计该要进行的拷贝操作为大作业或长时任务时，
     * 应另起线程进行该方法调用的作业，不应阻塞主线程<br>
     * @implNote 该方法的实现应该有一点需要明确:目前该方法的实现方式是朴素且低效的，
     * 该方法的过程实际为将两个远端的字节流进行传输，
     * 因为字节流的目标和源都为远端，遂线程对IO流的可读可写唤醒较为频繁，尤其为网络情况不佳时。
     * 但有额外可以实现的可行的补救或是优化方法，即若已知 {@code src} 和
     * {@code dst} 作用于同一个vfs上（即同一个远端服务器实体），
     * 则可通过vfs实际的依赖的传输协议（smb至少支持）进行如local_copy、local_move等操作直接通过协议本身的
     * 支持进行对文件的拷贝等操作，实际的执行耗时基本靠近本地（vfs指向的）磁盘读写耗时，而非当前实现方案的耗时
     * @see VFS#copy(VFile, VPath)
     */
    default void copyTo(VPath dst) throws VFSIOException {
        //noinspection resource
        this.vfs().copy(this, dst);
    }


    /**
     * 返回该 虚拟文件实体 的目录树<br>
     * 该实体必须是文件夹，当前实体将作为唯一树根
     *
     * @return 目录树
     * @throws IllegalArgumentException 该实体不是文件夹
     * @see #tree(int)
     */
    default Tree<VFile> tree() throws IllegalArgumentException {
        return this.tree(Integer.MAX_VALUE); // 单向
    }

    /**
     * 返回该 虚拟文件实体 的目录树<br>
     * 该实体必须是文件夹，当前实体将作为唯一树根
     *
     * @param depth 要构造的目录树的深度（包含）（边数计数法（给定的file所在深度为0，直接子级为1，以此类推）
     * @return 目录树
     * @throws IllegalArgumentException 该实体不是文件夹
     * @throws IllegalArgumentException 给定的 depth 小于 0
     * @see VFS#tree(List, int)
     */
    default Tree<VFile> tree(int depth) throws IllegalArgumentException {
        //noinspection resource
        return this.vfs().tree(List.of(this), depth); // 单向
    }


}
