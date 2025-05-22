package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.Closeable;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>虚拟文件系统（Virtual File System）</b><br>
 * 顶级接口，该模块使用者应使用该接口与该模块各功能集交互<br>
 * 该为有状态对象，构造可能失败，且使用完成需要关闭<br>
 * <b>已知的行为和注意事项:</b>
 * <ul>
 *     <li>VFS为有状态对象，其关闭后，调用{@link #isClosed()}方法必须要返回true，外界可多次
 *     调用VFS的{@link #close()}方法，但仅有第一次调用为有效性为，多次调用的行为应当为空操作</li>
 *     <li>当VFS的行为异常时，在非高危行为中，应当尽量抛出运行时异常而非预检异常，当进行高危行为时，
 *     应当返回{@link Optional}类型的{@link Optional#empty()}表示行为的失败</li>
 *     <li>各VFS实现类的线程安全性等应查看其类注释说明，VFS实现类有义务在实现类上进行已知的情况说明</li>
 *     <li>各VFS实现类中私有方法（涉及内部状态变更等）有义务进行方法说明</li>
 *     <li>各VFS实现类的构造成功即应表明连接参数的正确性，即应当在构造其实例时进行各项参数检查及测试等；
 *     当实例构造成功，即表示VFS已可使用，调用其{@link #isClosed()}方法应当返回false，但因各协议权限等问题，
 *     VFS的构造成功并不能保证也无法保证后续需进行的行为的正确性</li>
 *     <li>可以对该{@link VFS}接口行为进行拓展，进行行为拓展的开发者有义务对所有自己拓展的行为进行实现</li>
 *     <li>当前（20240920）部分VFS实现类中，当调用{@link #close()}方法后，部分方法仍可正常使用，这是待完善项</li>
 *     <li>当前（20240920）未对同一个VFS实例上的字节流传输进行特化；例如在smb-cifs协议中，已知有相关行为的特化命令（move、cp等），
 *     这种特化行为可对部分场景的流传输进行速度上的优化，这是待完善项</li>
 *     <li>VFS实例构造成功后，{@link #root()}调用要求一定能够成功的返回自己可操作或连接参数中配置的“根”路径，
 *     该根的{@link VPath#toFile()}行为一定将返回存在的{@link VFile}并且其类型应当一定为{@link VFileType#directory}</li>
 *     <li>部分实现可能需要手动实现连接保活机制，不同实现应在实现类文档说明</li>
 * </ul>
 * <b>示例</b>
 * <pre>
 *     {@code
 *     // 配置VFS所需参数
 *     Cfg vfsConfig = ...;
 *     // 通过VFS spi获取实例
 *     VFS vfs = VFSFactoryProvider.build(vfsType, vfsConfig);
 *     // 获取到vfs的“根”，可以以根为起点，在目录中游走
 *     VPath root = vfs.root();
 *     VPath p1 = root.join("abc");
 *     VPath p2 = root.join("def/ijk");
 *     VPath p3 = p1.back().join("def");
 *     Assert.isTrue(p1.back().isRoot());
 *     Assert.isTrue(p1.back().equals(root));
 *     Assert.isTrue(p3.equals(p2.back()));
 *     // VPath即表示目录层级关系等，VPath的存在并不一定表示实际的目录位置有文件
 *     Optional<VFile> fOpt = p1.toFile();
 *     if (!fOpt.isPresent){
 *         throw new VFSIOException("文件不存在");
 *     }
 *     // 若能够从VPath到VFile，则证明该位置一定有文件实体存在
 *     VFile f = fOpt.get();
 *     // VFile即可获得各项属性
 *     Assert.isTrue(f.name().equals("abc"));
 *     VFileType ft = f.type();
 *     long size = f.sizeOfBytes();
 *     }
 *     其他可进行的行为可探索{@link VPath}和{@link VFile}说明
 * </pre>
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public interface VFS extends Closeable {
    /**
     * 是什么类型的虚拟文件系统
     *
     * @return 虚拟文件系统类型
     */
    VFSType type();

    /**
     * 获取该虚拟文件系统所在的“虚拟根路径”
     *
     * @return 虚拟根路径
     * @apiNote 该返回的“虚拟根路径”并不表示实际文件系统的某个目录，
     * 而是表示该{@link VFS}可操作的“根”，这是相对于任意一个{@link VPath}的相对路径
     */
    VPath root();

    /**
     * 表示该虚拟文件系统是否已被关闭
     *
     * @return true 已被关闭，反之则未被关闭
     */
    @Override
    boolean isClosed();

    /**
     * 给定一个虚拟目录实体，返回该目录下的目录实体<br>
     * 若返回的{@link List}为{@link List#isEmpty()}，则表示该目录为空目录
     *
     * @param path 虚拟目录实体
     * @return 该目录下的虚拟目录实体
     * @throws VFSIOException       当过程出现异常（如权限不足，给定路径不存在等）或给定的{@link VPath}所在位置的实体为文件时
     * @throws NullPointerException 当给定的{@link VPath}为空时
     * @see #tryLsDir(VPath)
     */
    List<VPath> lsDir(VPath path) throws VFSIOException;

    /**
     * 给定一个虚拟目录实体，返回该目录下的目录实体<br>
     * 若返回的{@link List}为{@link List#isEmpty()}，则表示该目录为空目录
     *
     * @param path 虚拟目录实体
     * @return {@link R.Ok}载荷该目录下的虚拟目录实体 | {@link R.Err}载荷过程中出现的异常
     * @throws VFSIOException       当过程出现异常（如权限不足，给定路径不存在等）或给定的{@link VPath}所在位置的实体为文件时
     * @throws NullPointerException 当给定的{@link VPath}为空时
     * @see #tryLsDir(VPath)
     */
    default R<List<VPath>, Exception> tryLsDir(VPath path) {
        return R.ofCallable(() -> lsDir(path));
    }

    /**
     * 给定一个虚拟文件实体，返回该文件实体代表的文件夹下的文件实体<br>
     * 若返回的{@link List}为{@link List#isEmpty()}，则表示该目录为空目录
     *
     * @param file 虚拟文件实体
     * @return 该文件实体代表的文件夹下的文件实体
     * @throws VFSIOException       当过程出现异常（如权限不足，给定路径不存在等），或给定的{@link VFile}不为文件夹时
     * @throws NullPointerException 当给定的{@link VFile}为空时
     */
    default List<VFile> lsDir(VFile file) throws VFSIOException {
        Objects.requireNonNull(file, "given file is null");
        Err.realIf(!file.isDirectory(), VFSIOException::new, "Not a directory: '{}'", file.toPath().simplePath());
        VPath p = file.toPath();
        return lsDir(p).stream()
                .map(VPath::toFile)
                .map(Optional::orElseThrow) // 因为lsDir(VPath)返回的一定存在，遂该步骤一定不会抛出异常
                .toList();
    }

    /**
     * 给定一个虚拟文件实体，返回该文件实体代表的文件夹下的文件实体<br>
     * 若返回的{@link List}为{@link List#isEmpty()}，则表示该目录为空目录
     *
     * @param file 虚拟文件实体
     * @return {@link R.Ok}载荷该文件实体代表的文件夹下的文件实体 | {@link R.Err}载荷过程中出现的异常
     * @throws VFSIOException       当过程出现异常（如权限不足，给定路径不存在等），或给定的{@link VFile}不为文件夹时
     * @throws NullPointerException 当给定的{@link VFile}为空时
     */
    default R<List<VFile>, Exception> tryLsDir(VFile file) {
        return R.ofCallable(() -> lsDir(file));
    }

    /**
     * 给定虚拟目录实体，在该虚拟目录实体所表示的位置创建文件夹<br>
     * 该方法仅能在目录所表示位置可写且无实体，且该位置上级目录存在时，文件夹能被创建成功
     *
     * @param path 要创建文件夹的位置
     * @return 被创建的文件夹
     * @throws VFSIOException       当过程出现异常（如权限不足，给定目录上级目录不存在，位置已有实体等）时
     * @throws NullPointerException 当给定的{@link VPath}为空时
     */
    VFile mkdir(VPath path) throws VFSIOException;

    /**
     * 给定虚拟目录实体，在该虚拟目录实体所表示的位置创建文件夹<br>
     * 该方法仅保证在目录所表示位置可写且无实体，且该位置上级目录存在时，文件夹能被创建成功
     *
     * @param path 要创建文件夹的位置
     * @return {@link R.Ok}载荷被创建的文件夹 | {@link R.Err}载荷过程出现异常（如权限不足，给定目录上级目录不存在，位置已有实体等）
     */
    default R<VFile, Exception> tryMkdir(VPath path) {
        return R.ofCallable(() -> mkdir(path));
    }

    /**
     * 给定虚拟目录实体，删除该虚拟目录实体指代的位置的文件或文件夹<br>
     * 如果所在位置为一个文件夹，则会递归的进行文件夹删除操作（其内部的文件等也会被删除）
     *
     * @param path 要删除的虚拟目录实体
     * @throws VFSIOException       当过程出现异常，如权限不足等
     * @throws NullPointerException 给定的参数为null时
     * @apiNote 该方法确保执行结果的一致性（即若对应位置目录实体不存在，该方法也不会抛出异常）
     */
    default void rmIfExists(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        Optional<VFile> fOpt = path.toFile();
        if (fOpt.isEmpty()) return;
        if (fOpt.get().isDirectory()) {
            rmdir(path, true);
        } else if (fOpt.get().isSimpleFile()) {
            rmFile(path);
        } else {
            throw new VFSIOException("未知的状态，VFile既不是一个普通文件，又不是一个文件夹, vFile: " + fOpt.get());
        }
    }

    /**
     * 给定虚拟目录实体，该实体应为文件，删除之<br>
     * 该方法应当在明确虚拟目录实体表达的位置有一个文件（非文件夹）时调用，
     * 若为文件夹，该方法会抛出异常
     *
     * @throws VFSIOException       当给定实体为文件夹 或 无法删除时
     * @throws NullPointerException 给定参数为空时
     * @see #rmIfExists(VPath)
     */
    void rmFile(VPath path) throws VFSIOException;

    /**
     * 给定虚拟目录实体，该实体应为文件夹，删除之<br>
     * 该方法应当在明确虚拟目录实体表达的位置有一个文件夹（非文件）时调用，
     * 若为文件，该方法会抛出异常
     *
     * @param recursive 是否递归删除
     * @throws VFSIOException       当给定实体为文件 或 无法删除时
     * @throws NullPointerException 给定参数为空时
     * @see #rmIfExists(VPath)
     */
    void rmdir(VPath path, boolean recursive) throws VFSIOException;

    /**
     * 给定虚拟目录实体，返回布尔值表示该实体所指代位置是否实际存在一个实体
     *
     * @param path 要查询存在与否的目录实体
     * @return bool true 存在，false 不存在
     * @throws VFSIOException       当过程出现异常，如访问权限不足等
     * @throws NullPointerException 给定参数为空时
     */
    default boolean exists(VPath path) throws VFSIOException {
        Objects.requireNonNull(path, "given path is null");
        return getFile(path).isPresent();
    }

    /**
     * 给定虚拟目录实体，返回目录实体所指代位置的实际文件<br>
     * 若返回的optional有值，表示该文件/文件夹实体存在，否则，表示不存在
     *
     * @param path 虚拟目录实体
     * @return {@link Optional}载荷所指代位置的实际文件 | {@link Optional#empty()}
     * @throws VFSIOException       当过程出现异常，如访问权限不足等
     * @throws NullPointerException 当给定的参数为空时
     */
    Optional<VFile> getFile(VPath path) throws VFSIOException;

    /**
     * 获取该虚拟文件实体的字节流inputStream<br>
     * 当该实体为文件夹等、因网络、权限等无法读取时，抛出异常
     *
     * @return 文件字节流
     * @throws VFSIOException       该实体为文件夹等、因网络、权限等无法读取时
     * @throws NullPointerException 当给定的参数为空时
     * @apiNote 该方法要么返回一个文件字节流，要么抛出异常，一定不会返回{@code null}
     */
    InputStream getFileInputStream(VFile file) throws VFSIOException;

    /**
     * 获取该虚拟文件实体的字节流inputStream<br>
     * 当该实体为文件夹等、因网络、权限等无法读取时，返回{@link R.Err}
     *
     * @return {@link R.Ok}载荷文件字节流 | {@link R.Err}载荷异常
     */
    default R<InputStream, Exception> tryGetFileInputStream(VFile file) {
        return R.ofCallable(() -> getFileInputStream(file));
    }

    /**
     * 给定虚拟目录实体表示位置，通过给定的流创建文件<br>
     * 该方法不负责关闭给定的inputStream, 当前线程阻塞直到inputStream被消费完
     *
     * @param path        虚拟目录实体-要新建文件的位置
     * @param newFileData 填充要新建的文件的流
     * @return 虚拟文件实体-表示被创建的文件
     * @throws VFSIOException       当给定目录位置已有文件夹存在、权限不足无法写入等时
     * @throws NullPointerException 给定的虚拟目录实体为空或给定的流为空时
     */
    VFile mkFile(VPath path, InputStream newFileData) throws VFSIOException;

    /**
     * 给定虚拟目录实体表示位置，通过给定的流创建文件<br>
     * 该方法不负责关闭给定的inputStream, 当前线程阻塞直到inputStream被消费完
     *
     * @param path        虚拟目录实体-要新建文件的位置
     * @param inputStream 填充要新建的文件的流
     * @return {@link R.Ok}载荷虚拟文件实体-表示被创建的文件 | {@link R.Err}载荷过程发生的异常
     */
    default R<VFile, Exception> tryMkFile(VPath path, InputStream inputStream) {
        return R.ofCallable(() -> mkFile(path, inputStream));
    }
}
