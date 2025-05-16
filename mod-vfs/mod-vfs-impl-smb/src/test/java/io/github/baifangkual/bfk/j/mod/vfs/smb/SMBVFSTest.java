package io.github.baifangkual.bfk.j.mod.vfs.smb;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFile;
import io.github.baifangkual.bfk.j.mod.vfs.VPath;
import io.github.baifangkual.bfk.j.mod.vfs.smb.conf.SMBCfgOptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author baifangkual
 * @since 2024/8/29
 */
@SuppressWarnings({"CommentedOutCode", "unused"})
public class SMBVFSTest {

//    private static final Cfg vfsConf = Cfg.ofNew()
//            .set(SMBCfgOptions.share, "")
//            .set(SMBCfgOptions.host, "192.")
//            .set(SMBCfgOptions.user, "")
//            .set(SMBCfgOptions.passwd, "");
//
//
//    @Test
//    public void lsPathTest() {
//        SMBShareRootVirtualFileSystem vfs = new SMBShareRootVirtualFileSystem(vfsConf);
//        List<VPath> ls = vfs.lsDir(vfs.root());
//        ls.forEach(System.out::println);
//        VPath root = vfs.root();
//        VPath bfk = root.join("bfk");
//        List<VPath> ls1 = vfs.lsDir(bfk);
//        System.out.println("=============");
//        ls1.forEach(System.out::println);
//        System.out.println("================");
//        VPath enter = bfk.join("/temp");
//        vfs.lsDir(enter).forEach(System.out::println);
//        String s = enter.simplePath();
//        System.out.println(s);
//        String s1 = enter.realPath();
//        System.out.println(s1);
//        vfs.close();
//    }
//
//    @Test
//    public void lsFileTest() {
//        SMBShareRootVirtualFileSystem vfs = new SMBShareRootVirtualFileSystem(vfsConf);
//        List<VFile> ls = vfs.lsDir(vfs.root().toFile().orElseThrow());
//        System.out.println("========================");
//        ls.forEach(System.out::println);
//        System.out.println("========================");
//        vfs.close();
//    }
//
//    @Test
//    public void toStringTest() throws IOException {
//        try (SMBShareRootVirtualFileSystem vfs = new SMBShareRootVirtualFileSystem(vfsConf)) {
//            System.out.println(vfs);
//            VPath bfk = vfs.root().join("/bfk");
//            vfs.lsDir(bfk.toFile().orElseThrow()).forEach(System.out::println);
//            System.out.println(":::::::::::::::::::::::::::::::::");
//            VFile mib100 = bfk.join("/vfs-smb-test").join("/100MIB_file").toFile().orElseThrow();
//            try (InputStream inputStream = mib100.getInputStream();) {
//                VPath copyTargetPath = bfk.join("/vfs-smb-test/copy100file");
//                vfs.rmFile(copyTargetPath);
//                vfs.mkFile(copyTargetPath, inputStream);
//            }
//        }
//    }
//
//    @Test
//    public void mkdirTest() throws Exception {
//        VFS vfs = new SMBShareRootVirtualFileSystem(vfsConf);
//        VPath enter = vfs.root().join("/vfs-smb-test/mkdir");
//        vfs.mkdir(enter);
//        vfs.close();
//    }

}
