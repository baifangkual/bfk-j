package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.vfs.exception.IllegalVPathException;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import io.github.baifangkual.bfk.j.mod.vfs.mark.VEntity;

import java.io.InputStream;
import java.util.Optional;

import static io.github.baifangkual.bfk.j.mod.vfs.immutable.VFSDefaultConst.PATH_SEPARATOR;

/**
 * <b>虚拟目录实体</b><br>
 * 不可变对象，线程安全，所有返回同类型的方法均创造了新对象，该类型可当作虚拟文件系统中的路径表示，
 * 该类型实例的存在与虚拟文件系统中对应位置的文件实体的存在与否无关，仅表示虚拟文件系统中目录的游走和关系<br>
 * 实现类应当实现 hash, eq, comparable
 * <b>示例:</b>
 * <pre>
 *     {@code
 *     // 获取到虚拟文件系统的根
 *     VPath root = vfs.root();
 *     // 目录游走
 *     VPath a = root.join("a");
 *     VPath ab = a.join("b");
 *     VPath abcd = ab.join("c/d");
 *     VPath a1 = ab.back();
 *     VPath a2 = abcd.back(3);
 *     Assert.eq(a, a1);
 *     Assert.eq(a, a2);
 *     }
 * </pre>
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public interface VPath extends VEntity, Comparable<VPath> {

    /**
     * 语法糖，用以通过该接口直接获取一个虚拟目录实体
     *
     * @param vfs  虚拟文件系统
     * @param path n个路径，当该参数为空，则该方法返回 虚拟文件系统的根路径
     * @return 虚拟目录实体
     */
    static VPath of(VFS vfs, String... path) {
        VPath root = vfs.root();
        return (path != null && path.length > 0) ? root.join(String.join(PATH_SEPARATOR, path)) : root;
    }

    /**
     * 获取该虚拟目录实体所表示的文件/文件夹实体，当返回的{@link Optional}有值，表示文件的存在，若为{@link Optional#empty()}表示当前虚拟目录实体所
     * 表示的位置没有实际的文件存在，即{@link VFile}实体的存在一定能表示对应的文件/文件夹实体的存在
     *
     * @return nullable 文件实体
     */
    default Optional<VFile> toFile() {
        VFS vfs = getVFileSystem();
        return vfs.tryGetFile(this);
    }

    /**
     * 多次向上一级目录返回，{@link #back()}，
     * 当前接口的该方法只是兜底实现，因类型特性，会创建许多中间对象，遂下层实现最好复写该方法，
     * 当当前实例已经表示“根”，则对该方法的调用将会返回本身
     *
     * @param numberOfBack 返回次数
     * @return 新的 虚拟目录实体
     */
    default VPath back(int numberOfBack) {
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
     * 向上一级目录返回，该方法的调用将返回一个新的虚拟目录实例，表示当前目录实体的上一级目录实体，
     * 当当前实例已经表示“根”，则对该方法的调用将会返回本身
     *
     * @return 新的 虚拟目录实体 表示当前目录实体的上一级目录实体
     */
    VPath back();

    /**
     * 表示从当前目录进入一级或多级子目录，返回值为新的目录实体，不会改变原有目录实体<br>
     * 假设当前为 “/a”，给定参数 “b” 则返回 目录实体表示 “/a/b”
     * 假设当前为 “/”，给定参数 “a/b/c” 则返回 目录实体表示 “/a/b/c”
     *
     * @param relativePath 相对于当前目录的子目录层级字符串
     * @return 子目录实体
     */
    VPath join(String relativePath);

    VFS getVFileSystem();

    /**
     * 返回bool表示该目录实体是否为根目录
     */
    boolean isRoot();

    /**
     * 获取该目录实体的 path 简单表现形式
     */
    String simplePath();

    /**
     * 获取该目录实体的 实际目录<br>
     * 20241023 该名称可能具有一定误导性，所谓“实际目录” 不同的虚拟文件系统实现的和表示的“实际目录”不同，
     * 如果使用的是 VFS 的整个 API 完成你的业务，则该方法无需调用，推荐使用{@link #simplePath()} 即可
     */
    String realPath();

    /**
     * 目录层级，表示第几级层级，由0开始，当为0，表示为根
     *
     * @return 目录层级
     */
    int level();

    /**
     * 获取当前所在目录层级的名称，也就是完整目录层级的最后一个层级名称，
     * 当该实体表示根时，将返回{@link Optional#empty()}
     * <pre>{@code
     * VPath p1 = vfs.root().join("a/b/c/d");
     * Assert.eq(p1.simplePath(), "/a/b/c/d");
     * Assert.eq(p1.tryLast().get(), "d");
     * Assert.eq(p1.back().tryLast().get(), "c");}</pre>
     *
     * @return 最后一个目录层级的名称
     */
    Optional<String> tryLast();

    /**
     * 与{@link #tryLast()} 方法类似，区别是当当前目录实体为根时，将抛出异常
     *
     * @throws IllegalVPathException 当当前实体为根时
     */
    default String last() throws IllegalVPathException {
        return this.tryLast()
                .orElseThrow(() -> new IllegalVPathException(STF
                        .f("Can't get last path, current is \"{}\"", this.simplePath())));
    }

    /**
     * 创建文件，给定可读的inputStream，将在当前{@link VPath}表示的位置通过输入流创建文件，
     * 注意，该方法不是向已存在的文件追加写入，而是必定要经历两个过程：创建文件，写入字节，当当前对象
     * 表示的位置已存在文件或文件夹时，该操作将失败，抛出异常
     *
     * @param fileByteStream 构成文件的流
     * @throws VFSIOException 当当前对象表示的位置已有文件实体（文件或文件夹）时，抛出异常
     */
    default VFile mkFile(InputStream fileByteStream) throws VFSIOException {
        return this.getVFileSystem().mkFile(this, fileByteStream);
    }

    /**
     * 创建文件夹，在当前{@link VPath}所表示的位置创建文件夹，该方法保证结果一致性，即
     * 若文件夹已经存在于当前对象表示的位置，该方法也不会抛出异常，若当该位置已存在普通文件，则
     * 创建文件夹的行为将一定失败，因为同一个目录位置不允许存在文件夹和文件，仅能存在一个文件实体
     *
     * @throws VFSIOException 当当前位置已存在普通文件时
     */
    default VFile mkDir() throws VFSIOException {
        /*
        20241107 fix 如果为root，即已经存在，又因为该方法的策略为确保结果一致性，
        遂当self为root时，跳过创建目录的过程即可
         */
        if (!this.isRoot()) {
            this.getVFileSystem().mkdir(this);
        }
        return this.toFile().orElseThrow(() -> new VFSIOException("Can't create directory"));
    }
}
