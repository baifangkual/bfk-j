package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.*;
import io.github.baifangkual.bfk.j.mod.vfs.ftp.conf.FTPCfgOptions;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author baifangkual
 * create time 2024/9/13
 */
@SuppressWarnings("unused")
public class FTPVFSTest {

//    private VFS buildingFTPVFS() {
//        Cfg cfg = Cfg.ofNew()
//                .set(FTPCfgOptions.host, "192.")
//                .set(FTPCfgOptions.user, "")
//                .set(FTPCfgOptions.passwd, "");
//        return VFSFactory.build(VFSType.ftp, cfg);
//    }
//
//    @Test
//    public void testVFSLs() throws Exception {
//        try (VFS vfs = buildingFTPVFS()) {
//            VPath root = vfs.root();
//            List<VPath> vPaths = vfs.lsDir(root);
//            vPaths.forEach(System.out::println);
//        }
//    }
//
//    @Test
//    public void testUpload() throws Exception {
//        try (VFS vfs = buildingFTPVFS();
//             FileInputStream bis = new FileInputStream("100MIB_file")) {
//            VPath root = vfs.root();
//            VFile vFile = root.join("upFile01").mkFile(bis);
//            System.out.println(vFile);
//        }
//    }
//
//    @Test
//    public void testSelfCopy() throws Exception {
//        try (VFS vfs = buildingFTPVFS()) {
//            VPath root = vfs.root();
//            VFile f = root.join("upFile01").toFile().orElseThrow();
//            try (InputStream i = f.getInputStream()) {
//                // to do fix me 这里会卡住且没有创建文件 需查看
//                // <Date/Time> Info [Type] Message
//                // A command is already being processed. Queuing the new one until the current one is finished.
//                VFile vFile = root.join("upFile01Copy").mkFile(i);
//                System.out.println(vFile);
//            }
//        }
//    }
//
//    @Test
//    public void testSelfCopyLocal() throws Exception {
//        try (VFS vfs = buildingFTPVFS()) {
//            VPath root = vfs.root();
//            VFile f = root.join("upFile01").toFile().orElseThrow();
//            try (InputStream i = f.getInputStream()) {
//                // to do fix me 这里会卡住且没有创建文件 需查看
//                // 20240918 经测试 发现 ftp vfs 创建 inputSteam 后 再传送 mkFile 会卡住，可能
//                // FtpClient （Apache commons net 包）不允许这种情况，该线程不安全
//                // 这里使用  本地的outPutSteam发现不会卡住
//                try (FileOutputStream ff = new FileOutputStream("upFile01.td")) {
//                    System.out.println("transfer...");
//                    i.transferTo(ff);
//                }
//            }
//        }
//    }


}
