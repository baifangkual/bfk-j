package io.github.baifangkual.bfk.j.mod.vfs.minio;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFile;
import io.github.baifangkual.bfk.j.mod.vfs.VPath;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author baifangkual
 * @since 2024/9/5
 */
@SuppressWarnings({"CommentedOutCode", "unused", "CallToPrintStackTrace"})
public class MinioVFSTest {

//
//    private static final String host = "";
//    private static final int port = 9000;
//    private static final String accKey = "";
//    private static final String secKey = "";
//    private static final String bucket = "";
//
//
//    private MinioBucketRootVirtualFileSystem buildingVfs() {
//        Cfg config = Cfg.ofNew()
//                .set(MinioConfOptions.host, host)
//                .set(MinioConfOptions.port, port)
//                .set(MinioConfOptions.accessKey, accKey)
//                .set(MinioConfOptions.secretKey, secKey)
//                .set(MinioConfOptions.bucket, bucket);
//        return new MinioBucketRootVirtualFileSystem(config);
//    }
//
//    @Test
//    public void testProcessOnRoot01() {
//        final byte[] FLAG_BYTES = new byte[]{0x0f, 0x75, 0x0c, 0x6b, 0x00, 0x0d, 0x69, 0x72};
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VPath vPath = vfs.root();
//            VPath oneFile = vPath.join("testFile");
//            System.out.println(oneFile);
//            VFile vFile = vfs.mkFile(oneFile, new ByteArrayInputStream(FLAG_BYTES));
//            System.out.println(vFile);
//        }
//    }
//
//    @Test
//    public void testGetDirInfo() {
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VPath dirPath = vfs.root().join("dir/100MIB_file");
//            Optional<VFile> file = vfs.tryGetFile(dirPath);
//            if (file.isPresent()) {
//                System.out.println(file.get());
//            } else {
//                System.out.println(STF.f("\"{}\" not exists", dirPath));
//            }
//        }
//    }
//
//    @Test
//    public void testDeleteDirAndQuery() {
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VPath dirPath = vfs.root().join("r");
//            boolean exists = vfs.exists(dirPath);
//            if (exists) {
//                System.out.println(STF.f("\"{}\" is exists, delete it...", dirPath));
//                vfs.rm(dirPath);
//            } else {
//                System.out.println(STF.f("\"{}\" not exists", dirPath));
//            }
//            boolean exists2 = vfs.exists(dirPath);
//            if (exists2) {
//                System.out.println(STF.f("\"{}\" is exists, delete it...", dirPath));
//                vfs.rm(dirPath);
//            } else {
//                System.out.println(STF.f("\"{}\" not exists", dirPath));
//            }
//            vfs.mkdir(dirPath);
//            boolean exists3 = vfs.exists(dirPath);
//            if (exists3) {
//                System.out.println(STF.f("\"{}\" is exists, delete it...", dirPath));
//                vfs.rm(dirPath);
//            } else {
//                System.out.println(STF.f("\"{}\" not exists", dirPath));
//            }
//        }
//    }
//
//    @Test
//    public void copyFIleTest() {
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VPath m100File = vfs.root().join("dir/100MIB_file");
//            VFile vFile = m100File.toFile().orElseThrow(() -> new VFSIOException("file not found"));
//            try (InputStream inputStream = vFile.getInputStream()) {
//                VFile vFile1 = vfs.mkFile(m100File.back().join("100MIB_file_copy"), inputStream);
//                System.out.println(vFile1);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Test
//    public void testCreateFuckingDir() {
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VPath delDir = vfs.root().join("");
//            vfs.mkdir(delDir);
//            for (int i = 0; i < 10; i++) {
//                try (InputStream inp = new ByteArrayInputStream("".repeat(10).getBytes(StandardCharsets.UTF_8))) {
//                    vfs.mkFile(delDir.join("file" + i), inp);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }
//
//    @Test
//    public void testFuckingRecursiveDelDir() {
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VPath delDir = vfs.root().join("");
//            vfs.rmdir(delDir, true);
//        }
//    }
//
//
//    @Test
//    public void minioVFSFIleCopyToUseDIrTest(){
//        try (MinioBucketRootVirtualFileSystem vfs = buildingVfs()) {
//            VFile testDirFile = vfs.root().join("").toFile().orElseThrow();
//            testDirFile.copyTo(vfs.root().join("copy"));
//        }
//    }


}
