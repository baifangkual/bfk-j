package io.github.baifangkual.bfk.j.mod.vfs.minio;

import io.github.baifangkual.bfk.j.mod.vfs.minio.action.AbsMinioBucketRootDirectoryAction;
import io.github.baifangkual.bfk.j.mod.vfs.minio.action.NativeDirectoryAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author baifangkual
 * @since 2024/9/3
 */
@SuppressWarnings({"CommentedOutCode", "unused"})
public class DirectoryActionTest {

    @Test
    public void absMethodTest() {

        AbsMinioBucketRootDirectoryAction ac = new NativeDirectoryAction(null, null);
        Assertions.assertEquals("abc/", MinioPro.rightAddPathSeparator("abc"));
        Assertions.assertEquals("/abc", MinioPro.leftAddPathSeparator("abc"));
        Assertions.assertEquals("abc", MinioPro.rightCleanPathSeparator("abc"));
        Assertions.assertEquals("abc", MinioPro.rightCleanPathSeparator("abc/"));
        Assertions.assertEquals("abc", MinioPro.leftCleanPathSeparator("abc"));
        Assertions.assertEquals("abc", MinioPro.leftCleanPathSeparator("/abc"));
        ArrayList<String> sl = new ArrayList<>();
        sl.add("abc");
        sl.add("def");
        sl.add("ghi");
        sl.add("jkl");
        MinioPro.doExcludeNames(sl, "jkl");
        Assertions.assertEquals(List.of("abc", "def", "ghi"), sl);
        MinioPro.doExcludeNames(sl);
        Assertions.assertEquals(List.of("abc", "def", "ghi"), sl);
        MinioPro.doExcludeNames(sl, (String) null);
        Assertions.assertEquals(List.of("abc", "def", "ghi"), sl);

    }
//
//    private static final String host = "";
//    private static final int port = 9000;
//    private static final String accKey = "";
//    private static final String secKey = "";
//    private static final String adminAccKey = "";
//    private static final String adminSecKey = "";
//
//    private MinioClient buildingCli() {
//        return MinioClient.builder()
//                .endpoint(host, port, false)
//                .credentials(accKey, secKey).build();
//    }
//
//
//    @Test
//    public void NativeBucketRootDirectoryActionTest01() throws Exception {
//
//        VPath root = new DefaultSliceAbsolutePath(null, "/");
//
//        try (MinioClient cli = buildingCli()) {
//            MinioDirectoryAction action = new NativeDirectoryAction(cli, "");
//            List<VPath> vPaths = action.lsDir(root);
//            vPaths.forEach(System.out::println);
//            System.out.println("===============");
//            List<VPath> vPaths1 = action.lsDir(root.join(""), "100MIB_file");
//            vPaths1.forEach(System.out::println);
//            System.out.println("EMPTY：+++++++++++++++");
//            List<VPath> vPaths2 = action.lsDir(root.join("acb"));
//            System.out.println(vPaths2.size());
//            vPaths2.forEach(System.out::println);
//        }
//    }
}
