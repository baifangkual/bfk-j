package io.github.baifangkual.jlib.vfs.fs;

import io.github.baifangkual.jlib.core.Const;
import io.github.baifangkual.jlib.core.mark.Iter;
import io.github.baifangkual.jlib.core.util.Stf;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 工具类，本地文件系统各操作
 *
 * @author baifangkual
 * @since 2025/6/10 v0.1.2
 */
public class LocalSupport {

    private LocalSupport() {
        throw new AssertionError("not instantiable");
    }

    public enum LocalType {
        windows,
        linuxLike; // linux-like... MacOs?
    }

    /**
     * 获取当前系统的根目录，该方法每次调用都会使用 {@link FileSystems#getDefault()} 获取，
     * 其中 {@code Windows} 系统的根目录将返回多个盘符，形如 {@code C:\}，{@code Linux} 系系统的根目录为 {@code /}
     *
     * @return 当前系统的根目录 {@link Path}
     * @apiNote 若出现如拔插U盘的情况，该方法可以反映出新插入的U盘，但是该方法的调用次数过多，可能会影响性能
     */
    public static Iterable<Path> newCurrRootDirPathIter() {
        return FileSystems.getDefault().getRootDirectories();
    }


    public static List<String> newCurrCleanedRootDirNames() throws IllegalStateException {
        List<String> roots = Iter
                .toStream(newCurrRootDirPathIter())
                .map(Path::toString)
                .toList();
        // assert
        int rCount = roots.size();
        if (rCount > 1 && localType == LocalType.linuxLike) {
            throw new IllegalStateException("Linux-like system should only have one root directory");
        }
        if (localType == LocalType.linuxLike && !roots.get(0).equals(Const.String.SLASH)) {
            throw new IllegalStateException("Linux-like system root is not '/', root: " + roots.get(0));
        }
        // on win c or many
        return switch (localType) {
            case linuxLike -> roots;
            case windows -> roots.stream() // process C:\ -> /c
                    .map(r -> Const.String.SLASH + r.substring(0, r.length() - 2))
                    .map(String::toLowerCase)
                    .toList();
        };
    }


    public static final LocalType localType = newCurrLocalType();

    /**
     * 获取当前系统的类型
     *
     * @return 当前系统的类型
     */
    static LocalType newCurrLocalType() {
        String osn = System.getProperty("os.name");
        Optional<String> orWin = Optional.of(osn) // assert not null
                .map(String::toUpperCase)
                .filter(o -> o.contains("WINDOWS"));
        String fsp = File.separator;
        boolean isWinOsName = orWin.isPresent();
        boolean isNotWinOsName = orWin.isEmpty();
        boolean isWinOsFsp = fsp.equals("\\");
        boolean isLinuxLikeOsFsp = fsp.equals("/");
        if (isWinOsName && isWinOsFsp) {
            return LocalType.windows;
        }
        if (isLinuxLikeOsFsp && isNotWinOsName) {
            return LocalType.linuxLike;
        }
        // 谁知道呢
        throw new IllegalStateException(Stf.f("un support os type : '{}', file separator: '{}'", osn, fsp));
    }


}
