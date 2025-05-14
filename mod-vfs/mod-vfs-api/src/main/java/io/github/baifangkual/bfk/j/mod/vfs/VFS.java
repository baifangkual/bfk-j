package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.Closeable;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;

import java.io.InputStream;
import java.util.List;
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
 *     Config vfsConfig = ...;
 *     // 通过VFS spi获取实例
 *     VFS vfs = VFSFactory.build(vfsConfig);
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
 *     // 也可获取可能存在的文件流，当VFile表示的实体为文件夹时，将没有文件流
 *     try(InputStream in = f.tryGetInputStream().orElseThrow(VFSIOException::new)){
 *         in.read...
 *     }
 *     }
 *     其他可进行的行为可探索{@link VPath}和{@link VFile}说明
 * </pre>
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public interface VFS extends Closeable {
    /**
     * 表示支持什么类型的虚拟文件系统
     *
     * @return {@link VFSType}
     */
    VFSType supportType();

    /**
     * 表示该虚拟文件系统所依附的实际的根目录
     *
     * @return 根目录字符串
     */
    String realRootPathString();

    /**
     * 获取该虚拟文件系统的虚拟根路径对象
     *
     * @return 虚拟根路径对象
     */
    VPath root();

    /**
     * 表示该虚拟文件系统是否已被关闭，被关闭后便无法使用
     *
     * @return bool
     */
    @Override
    boolean isClosed();

    /**
     * 给定一个虚拟目录实体，返回该目录下的目录实体，
     * 需注意，一个Optional方法{@link #ls(VPath)} 已将行为委托至该方法，
     * 遂具体的实现类中，要么实现方在该方法的实现中进行对给定路径的检查并重写{@link #ls(VPath)}，
     * 要么，该方法的行为不进行检查，否则则会多次进行行为检查，虽说多次检查不会造成异常，但考虑到实际依托于网络的
     * 部分实现，性能总是不太好，且除了第一次检查以外，后续的检查无意义
     *
     * @param path 虚拟目录实体
     * @return 该目录下的虚拟目录实体
     * @throws VFSIOException 当过程出现异常或给定的vPath实体为文件时
     */
    List<VPath> lsDir(VPath path) throws VFSIOException;

    /**
     * 给定一个虚拟文件实体，返回该文件实体代表的目录下的实体
     *
     * @param file 虚拟文件实体
     * @return 该目录下的虚拟文件实体
     * @throws VFSIOException 当过程出现异常，或给定的虚拟文件实体类型不为文件夹时
     */
    default List<VFile> lsDir(VFile file) throws VFSIOException {
        Err.realIf(!file.isDirectory(), VFSIOException::new, "Not a directory: {}", file.toPath());
        VPath p = file.toPath();
        return lsDir(p).stream().map(VPath::toFile).map(Optional::orElseThrow).toList();
    }

    /**
     * 相较于 {@link #lsDir(VPath)} 该方法的入参若实际为文件实体，则返回{@link Optional#empty()},
     * 若文件夹为空，则返回 Option[Some(List.empty)]
     *
     * @param path 虚拟目录实体
     * @return 该目录下的虚拟目录实体
     * @throws VFSIOException 当过程出现异常,如权限不足，给定路径不存在等
     */
    default Optional<List<VPath>> ls(VPath path) throws VFSIOException {
        VFile vf = this.tryGetFile(path).orElseThrow(() -> new VFSIOException("Can't find path " + path));
        if (!vf.isDirectory()) {
            return Optional.empty();
        }
        return Optional.of(lsDir(path));
    }


    /**
     * 给定虚拟目录实体，创建该目录，该方法确保执行的结果一致性（即若已有目录时，该方法也认为创建目录成功）
     *
     * @param path 要创建的虚拟目录实体
     * @throws VFSIOException 当过程出现异常，如权限不足时，目录实体已有同名文件时
     */
    void mkdir(VPath path) throws VFSIOException;

    /**
     * 给定虚拟目录实体，删除该目录或文件，该方法确保执行结果的一致性（即若目录实体不存在，该方法也认为删除成功）
     *
     * @param path 要删除的虚拟目录实体
     * @throws VFSIOException 当过程出现异常，如权限不足等
     */
    default void rm(VPath path) throws VFSIOException {
        Optional<VFile> fOpt = path.toFile();
        if (fOpt.isEmpty()) return;
        if (fOpt.get().isDirectory()) {
            rmdir(path, true);
        } else if (fOpt.get().isSimpleFile()) {
            rmFile(path);
        }
    }

    /**
     * 给定虚拟目录实体，该实体应为文件，删除之
     *
     * @throws VFSIOException 当给定实体为文件夹 或 无法删除时
     */
    void rmFile(VPath path) throws VFSIOException;

    /**
     * 给定虚拟目录实体，该实体应为文件夹，删除之
     *
     * @param recursive 是否递归删除
     * @throws VFSIOException 当给定实体为文件 或 无法删除时
     */
    void rmdir(VPath path, boolean recursive) throws VFSIOException;

    /**
     * 给定虚拟目录实体，返回布尔值表示该实体是否实际存在
     *
     * @param path 要查询存在与否的目录实体
     * @return bool true 存在，false 不存在
     * @throws VFSIOException 当过程出现异常，如访问权限不足等
     */
    default boolean exists(VPath path) throws VFSIOException {
        return tryGetFile(path).isPresent();
    }

    /**
     * 给定虚拟目录实体，返回表示目录实体的实际文件，若返回的optional有值，表示该文件/文件夹实体存在，否则，表示不存在
     *
     * @param path 虚拟目录实体
     * @return {@link Optional#empty()} 虚拟目录实体所表示的位置无文件，若非empty，表示目录实体所在位置有文件，返回文件虚拟表示
     * @throws VFSIOException 当过程出现异常，如访问权限不足等
     */
    Optional<VFile> tryGetFile(VPath path) throws VFSIOException;


    /**
     * 获取该虚拟文件的 字节流 inputStream，当该实体为文件夹等时，必定返回 {@link Optional#empty()},
     * 因为文件夹没有inputStream 所以为 empty，当因网络、权限等无法读取时，抛出异常
     *
     * @return {@link InputStream}
     */
    Optional<InputStream> tryGetFileInputStream(VFile file) throws VFSIOException;


    /**
     * 给定目录位置和输入流，在给定的目录位置通过给定的流创建文件，该方法不负责关闭给定的inputStream,
     * 当前线程阻塞直到inputStream被消费完
     *
     * @param path        要新建文件的目录位置
     * @param inputStream 要新建的文件的输入流
     * @return 创建的文件
     * @throws VFSIOException 当给定目录位置已有文件夹存在等，抛出异常
     */
    VFile mkFile(VPath path, InputStream inputStream) throws VFSIOException;

    /**
     * 不同于{@link #tryGetFileInputStream(VFile)} 当VFile为文件夹时，抛出异常
     *
     * @throws VFSIOException 当因网络、权限等无法读取时
     */
    default InputStream readFile(VFile file) throws VFSIOException {
        return tryGetFileInputStream(file).orElseThrow(() -> new VFSIOException(STF
                .f("\"{}\" 无输入流", file.toPath())));
    }


    /**
     * {@link #tryGetFile(VPath)} , 但仅查看实体类型，当给定路径不存在文件实体时，返回{@link Optional#empty()}
     *
     * @param path 虚拟目录实体
     * @return {@link VFileType}
     * @throws VFSIOException 当过程出现异常，如访问权限不足等
     */
    default Optional<VFileType> tryGetFileType(VPath path) throws VFSIOException {
        return tryGetFile(path).map(VFile::type);
    }

    /**
     * 相较于 {@link #tryGetFileType(VPath)} 当给定目录表示的实体不存在时，将抛出异常
     *
     * @throws VFSIOException 当给定的实体不存在时，或IO过程出现异常，如访问权限不足等
     */
    default VFileType getFileType(VPath path) throws VFSIOException {
        return tryGetFileType(path).orElseThrow(() -> new VFSIOException("Can't find path " + path));
    }


}
