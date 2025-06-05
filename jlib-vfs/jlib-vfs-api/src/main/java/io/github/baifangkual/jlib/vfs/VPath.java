package io.github.baifangkual.jlib.vfs;


import io.github.baifangkual.jlib.core.lang.R;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;
import io.github.baifangkual.jlib.vfs.mark.VEntity;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import static io.github.baifangkual.jlib.vfs.VFSDefaults.PATH_SEPARATOR;

/**
 * <b>虚拟目录实体</b><br>
 * 不可变对象，线程安全，所有返回同类型的方法均创造了新对象，该类型可当作虚拟文件系统中的路径表示，
 * 一个该实例表示一个确定的{@link VFS}中一个确定的位置，但与对应位置的文件实体的存在与否无关，
 * 仅表示虚拟文件系统中目录的游走关系<br>
 *
 * <pre>
 *     {@code
 *     // 获取到虚拟文件系统的根
 *     VPath root = vfs.root();
 *     // 目录游走
 *     VPath a = root.join("a"); // path on "/a"
 *     VPath ab = a.join("b"); // path on "/a/b"
 *     VPath abcd = ab.join("c/d"); // path on "/a/b/c/d"
 *     VPath a1 = ab.back();
 *     VPath a2 = abcd.back(3);
 *     Assert.eq(a, a1);
 *     Assert.eq(a, a2);
 *     }
 * </pre>
 *
 * @author baifangkual
 * @implSpec 实现类应当实现 hash, eq, comparable方法，且实现类应当是一个不可变的线程安全对象，
 * 为确保不变性，实现类所有的需修改自身并返回{@link VPath}的方法都应当返回一个新的实例
 * @since 2024/8/23 v0.0.5
 */
public interface VPath extends VEntity, Comparable<VPath> {

    /**
     * 构造一个虚拟目录实体
     *
     * @param vfs  虚拟文件系统
     * @param path n个路径，当该参数为空，则该方法返回虚拟文件系统的根虚拟目录实体
     * @return 虚拟目录实体
     * @throws NullPointerException 给定的虚拟文件系统对象为空时
     */
    static VPath of(VFS vfs, String... path) {
        VPath root = Objects.requireNonNull(vfs, "vfs is null").root();
        return (path != null && path.length > 0) ? root.join(String.join(PATH_SEPARATOR, path)) : root;
    }

    /**
     * 获取该虚拟目录实体所表示位置的文件实体<br>
     * 当返回的{@link Optional}有值，表示文件的存在，若为{@link Optional#empty()}表示当前虚拟目录实体所
     * 表示的位置没有实际的文件存在，即{@link VFile}实体的存在一定能表示对应的文件/文件夹实体的存在
     *
     * @return 虚拟文件实体 | {@link Optional#empty()}
     */
    default Optional<VFile> toFile() {
        //noinspection resource
        VFS vfs = vfs();
        return vfs.file(this);
    }

    /**
     * 向上一级目录返回n次<br>
     * 当当前虚拟目录已经表示一个{@link VFS}的“根”目录时，该方法将返回自身
     *
     * @param numberOfBack 返回次数n
     * @return 虚拟目录实体
     * @throws IllegalArgumentException 给定的数字小于0时
     * @implSpec 该方法的默认实现只是兜底实现，会创建许多中间对象，遂下层实现最好覆写该方法
     * @see #back()
     */
    default VPath back(int numberOfBack) {
        Err.realIf(numberOfBack < 0, IllegalArgumentException::new, "Back number cannot be negative");
        if (this.isRoot()) {
            return this;
        }
        VPath ref = this;
        for (int i = 0; i < numberOfBack; i++) {
            ref = ref.back();
        }
        return ref;
    }

    /**
     * 向上一级目录返回<br>
     * 当当前虚拟目录已经表示一个{@link VFS}的“根”目录时，该方法将返回自身
     *
     * @return 虚拟目录实体
     * @implSpec 该方法的调用将返回一个新的虚拟目录实例，表示当前目录实体的上一级目录实体
     * @see #back(int)
     */
    VPath back();

    /**
     * 从当前目录进入一级或多级子目录<br>
     * 返回值为新的目录实体，不会改变原有目录实体<br>
     * 假设当前为 “/a”，给定参数 “b” 则返回 目录实体表示 “/a/b”
     * 假设当前为 “/”，给定参数 “a/b/c” 则返回 目录实体表示 “/a/b/c”
     *
     * @param relativePath 相对于当前目录的子目录层级字符串
     * @return 虚拟目录实体
     */
    VPath join(String relativePath);

    /**
     * 返回自身的{@link VFS}
     *
     * @return 虚拟文件系统
     */
    VFS vfs();

    /**
     * 该虚拟目录实体是否为所在{@link VFS}的表示的根目录
     *
     * @return true: 是，反之则否
     */
    boolean isRoot();

    /**
     * 获取该目录实体的 path 简单表现形式
     */
    String simplePath();

    /**
     * 目录层级，表示第几级目录<br>
     * 由0开始，当为0，表示为所在{@link VFS}的表示的根目录
     *
     * @return 目录层级
     */
    int level();

    /**
     * 获取当前所在目录层级的名称，也就是完整目录层级的最后一个层级名称，
     * 当该实体表示根时，将返回{@value VFSDefaults#PATH_SEPARATOR}
     * <pre>
     *     {@code
     *     VPath p1 = vfs.root().join("a/b/c/d");
     *     Assert.eq(p1.simplePath(), "/a/b/c/d");
     *     Assert.eq(p1.name(), "d");
     *     Assert.eq(p1.back().name(), "c");
     *     Assert.eq(vfs.root(), "/");
     *     }
     * </pre>
     *
     * @return 最后一个目录层级的名称
     */
    String name();

    /**
     * 返回该虚拟目录实体所指代位置是否实际存在一个实体
     *
     * @return true 存在，反之不存在
     */
    default boolean exists() {
        //noinspection resource
        return this.vfs().exists(this);
    }

    /**
     * 给定可读的inputStream，创建文件<br>
     * 将在当前{@link VPath}表示的位置通过输入流创建文件，
     * 注意，该方法不是向已存在的文件追加写入，而是必定要经历两个过程：创建文件，写入字节，当当前对象
     * 表示的位置已存在文件或文件夹时，该操作将失败，抛出异常
     *
     * @param newFileData 构成文件的流
     * @return 虚拟文件实体（表示被创建的文件）
     * @throws VFSIOException       当前对象表示的位置已有文件实体（文件或文件夹）或底层实现创建文件过程或写入文件过程发生异常时
     * @throws NullPointerException 给定的流为空时
     * @apiNote 该方法不负责给定的流的关闭
     */
    default VFile mkFile(InputStream newFileData) throws VFSIOException {
        Objects.requireNonNull(newFileData, "newFileData is null");
        if (this.isRoot()) throw new VFSIOException("directory already exists");
        //noinspection resource
        return this.vfs().mkFile(this, newFileData);
    }

    /**
     * 给定可读的inputStream，创建文件<br>
     * 将在当前{@link VPath}表示的位置通过输入流创建文件，
     * 注意，该方法不是向已存在的文件追加写入，而是必定要经历两个过程：创建文件，写入字节，当当前对象
     * 表示的位置已存在文件或文件夹时，该操作将失败，抛出异常
     *
     * @param newFileData 构成文件的流
     * @return {@link R.Ok}(虚拟文件实体(表示被创建的文件))| {@link R.Err}（创建过程的异常）
     * @apiNote 该方法不负责给定的流的关闭
     */
    default R<VFile> tryMkFile(InputStream newFileData) {
        return R.ofFnCallable(() -> mkFile(newFileData));
    }

    /**
     * 创建文件夹<br>
     * 在当前{@link VPath}所表示的位置创建文件夹，若当该位置已存在普通文件，则
     * 创建文件夹的行为将一定失败，因为同一个目录位置不允许存在文件夹和文件，仅能存在一个文件实体，
     * 该方法将抛出异常
     *
     * @return 虚拟文件实体（表示被创建或已存在的文件夹）
     * @throws VFSIOException 当前位置已存在普通文件时
     */
    default VFile mkDir() throws VFSIOException {
        /*
        20241107 fix 如果为root，即已经存在，又因为该方法的策略为确保结果一致性，
        遂当self为root时，跳过创建目录的过程即可
        20250517 该方法已取消结果一致性保证，方法语义更严格
         */
        if (this.isRoot()) throw new VFSIOException("directory already exists");
        else {
            //noinspection resource
            this.vfs().mkdir(this);
        }
        VFile dir = this.toFile().orElseThrow(() -> new VFSIOException("Can't create directory"));
        if (!dir.isDirectory()) {
            throw new VFSIOException(Stf
                    .f("Can't create directory, path '{}' already exists a non directory entity", simplePath()));
        }
        return dir;
    }

    /**
     * 创建文件夹<br>
     * 在当前{@link VPath}所表示的位置创建文件夹，该方法保证结果一致性，即
     * 若文件夹已经存在于当前对象表示的位置，该方法也不会抛出异常，若当该位置已存在普通文件，则
     * 创建文件夹的行为将一定失败，因为同一个目录位置不允许存在文件夹和文件，仅能存在一个文件实体，
     * 该方法将抛出异常
     *
     * @return {@link R.Ok}载荷虚拟文件实体（表示被创建或已存在的文件夹）| {@link R.Err}载荷执行过程中的异常
     * @throws VFSIOException 当前位置已存在普通文件时
     */
    default R<VFile> tryMkDir() {
        return R.ofFnCallable(this::mkDir);
    }
}
