package io.github.baifangkual.jlib.vfs.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author baifangkual
 * @since 2024/9/2
 */
@Slf4j
@SuppressWarnings({"CommentedOutCode", "unused"})
public class MinIOClientTest {
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
//    @Test
//    public void testMiniOClientBucketExists() throws Exception {
//        MinioClient clt = buildingCli();
//        boolean tbkEx = clt.bucketExists(BucketExistsArgs.builder()
//                .bucket("")
//                .build());
//        boolean abkEx = clt.bucketExists(BucketExistsArgs.builder()
//                .bucket("test-anonymous")
//                .build());
//        System.out.println(tbkEx);
//        System.out.println(abkEx);
//        boolean noE = clt.bucketExists(BucketExistsArgs.builder().bucket("no-exists-bk").build());
//        System.out.println(noE);
//        clt.close();
//    }
//
//    @Test
//    public void testMiniOClientObjectExists() throws Exception {
//        try (MinioClient cli = buildingCli()) {
//            // Get information of an object.
//            final String bkN = "";
//
//            // 经测试，args中各项，似乎必须要指定bucket，否则构建问题
//            StatObjectResponse fileStat =
//                    cli.statObject(StatObjectArgs.builder()
//                            .bucket(bkN)
//                            .object("")
//                            .build());
//
//            // fix me ??? 没有文件夹概念还是我查错了
//            // https://www.minio.org.cn/docs/minio/linux/administration/concepts.html
//            // 没有文件夹概念，仅有 “前缀” 概念以模仿文件夹
//            // 原生似乎无法查询文件夹属性
////            StatObjectResponse folderStat =
////                    cli.statObject(StatObjectArgs.builder()
////                            .bucket(bkN)
////                            .object("")
////                            .build());
//
//
//        }
//    }
//
//    @Test
//    public void testStatObjIfNotExistsNoThrowException() throws Exception {
//
//        final String bk = "";
//
//        try (MinioClient cli = buildingCli()) {
//
//            try {
//
//                StatObjectResponse statObjectResponse = cli.statObject(StatObjectArgs.builder()
//                        .bucket(bk)
//                        .object("")
//                        .build());
//                System.out.println(statObjectResponse.toString());
//                System.out.println(statObjectResponse.object());
//            } catch (ErrorResponseException e) {
//                // 该处捕获标识不存在
//                String message = e.errorResponse().message();
//                System.out.println(message);
//                log.info("not exists!");
//            }
//        }
//    }
//
//    @Test
//    public void testDirExists() throws Exception {
//
//        // to do 判断之
//        // 可使用 ... 判断之，若 ... 不存在 则 再使用 listObject判断之
//        final String bkN = "";
//        ByteArrayInputStream bIp = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
//
//        try (MinioClient cli = buildingCli()) {
//            StatObjectResponse testObjFile = cli.statObject(StatObjectArgs.builder()
//                    .bucket(bkN)
//                    .object("")
//                    .build());
//            System.out.println(testObjFile);
//        }
//    }
//
//    @Test
//    public void testCreateFile() throws Exception {
//
//        final String bkN = "";
//        ByteArrayInputStream bIp = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
//
//        try (MinioClient cli = buildingCli()) {
//            ObjectWriteResponse testObjFile = cli.putObject(PutObjectArgs.builder()
//                    .bucket(bkN)
//                    .object("")
//                    .stream(bIp, -1, 1024 * 1024 * 8)
//                    .build());
//            System.out.println(testObjFile);
//        }
//    }
//
//    @Test
//    public void testCreateDirFlagFile() throws Exception {
//        final String bkN = "";
//        final byte[] FLAG_BYTES = new byte[]{0x0f, 0x75, 0x0c, 0x6b, 0x00, 0x0d, 0x69, 0x72};
//        // 已测试其不可使用 . 和 .. 遂使用 ...
//        // 或者 使用 默认所有 “文件夹“ 存在或都不存在
//        try (MinioClient cli = buildingCli()) {
//            ObjectWriteResponse objectWriteResponse = cli.putObject(PutObjectArgs.builder()
//                    .bucket(bkN)
//                    .object("")
//                    .stream(new ByteArrayInputStream(FLAG_BYTES), 8, -1)
//                    .build());
//        }
//    }
//
//    @Test
//    public void testCreateFuckingDir() throws Exception {
//        final String bkN = "";
//        final byte[] FLAG_BYTES = new byte[]{0x0f, 0x75, 0x0c, 0x6b, 0x00, 0x0d, 0x69, 0x72};
//        // 已测试其不可使用 . 和 .. 遂使用 ...
//        // 或者 使用 默认所有 “文件夹“ 存在或都不存在
//        try (MinioClient cli = buildingCli()) {
//            ObjectWriteResponse objectWriteResponse = cli.putObject(PutObjectArgs.builder()
//                    .bucket(bkN)
//                    .object("")
//                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
//                    .build());
//        }
//    }
//
//    @Test
//    public void lsOneDir() throws Exception {
//        /*
//        经过该，可知其 cli.listObjects行为：添加 / 在 其后，能够查询到其内部元素
//        若无 / 在其后，则 若实体为文件，则可查询到其 迭代器有1-N个文件，若为文件夹 ，则可查询到其，迭代器有1~N个文件，这些文件为实体的表示
//        若有 / 在其后，则 若实体为文件夹，则迭代器中有1-N个文件，若实体为文件，则迭代器中有0个文件
//        通过其参数prefix也可知，因为为通过前缀匹配，且minio无文件夹概念，遂这样以 不以 / 结束的目录表示，能够查询到 有 给定 prefix 的实体
//         */
//        final String bkN = "";
//        List<Item> its = new ArrayList<>();
//        try (MinioClient cli = buildingCli()) {
//            Iterable<Result<Item>> results = cli.listObjects(ListObjectsArgs.builder()
//                    .bucket(bkN)
//                    .prefix("")
//                    .recursive(false).build());
//            for (Result<Item> result : results) {
//                String s = result.get().objectName();
//                System.out.println(s);
//                its.add(result.get());
//            }
//        }
//        its.forEach(System.out::println);
//    }
//
//    @Test
//    public void testLsFuckingDir() throws Exception {
//        final String bkN = "";
//        final List<Item> l = new ArrayList<>();
//        try (MinioClient cli = buildingCli()) {
//            Iterable<Result<Item>> it = cli.listObjects(ListObjectsArgs.builder()
//                    .bucket(bkN)
//                    // bucket 下 使用 “” ，头无需带 /
//                    .prefix("")
//                    .recursive(true)
//                    .build());
//
//            for (Result<Item> ir : it) {
//                Item item = ir.get();
//                l.add(item);
//            }
//        }
//        List<String> rNameList = l.stream().map(Item::objectName)
////                .map(nCodeName -> URLDecoder.decode(nCodeName, StandardCharsets.UTF_8))
//                .toList();
//
//        l.forEach(System.out::println);
//        rNameList.forEach(System.out::println);
//        System.out.println("end");
//    }
//
//    @Test
//    public void testLsFuckingFile() throws Exception {
//        final String bkN = "";
//        final List<Item> l = new ArrayList<>();
//        try (MinioClient cli = buildingCli()) {
//            Iterable<Result<Item>> it = cli.listObjects(ListObjectsArgs.builder()
//                    .bucket(bkN)
//                    // bucket 下 使用 “” 或 null 皆可 ，头无需带 /，且所有次级查都应以 / 为结尾
//                    // item中 objectName 为 url编码形式，需解码
//                    // 不存在的目录不会异常，而是一个空目录
//                    .prefix("")
//                    .recursive(false)
//                    .build());
//
//            for (Result<Item> ir : it) {
//                Item item = ir.get();
//                l.add(item);
//            }
//        }
//
//        l.forEach(System.out::println);
//    }
//
//
//    @Test
//    public void testListAPICreatedDir() throws Exception {
//        final String bkN = "";
//
//        List<Item> l = new ArrayList<>();
//        try (MinioClient cli = buildingCli()) {
//
////            cli.removeObject(RemoveObjectArgs.builder()
////                    .bucket(bkN)
////                    .);
//
//            Iterable<Result<Item>> results = cli.listObjects(ListObjectsArgs.builder()
//                    .bucket(bkN)
//                    .prefix("")
//                    .recursive(true)
//                    .build());
//            for (Result<Item> r : results) {
//                Item it = MinioPro.sneakyRun(r::get);
//                l.add(it);
//            }
//
//        }
//
//        l.forEach(System.out::println);
//    }
//
//
//    @Test
//    public void testDelAPICreatedDir() {
//        final String bkN = "";
//        try (MinioClient cli = buildingCli()) {
//            cli.removeObject(RemoveObjectArgs.builder()
//                    .bucket(bkN)
//                    .object("")
//                    .build());
//        } catch (Exception ignored) {
//        }
//
//
//    }
//
//    @Test
//    public void testMidCreateDir() {
//        final String bkN = "";
//        try (MinioClient cli = buildingCli()) {
//            cli.putObject(PutObjectArgs.builder()
//                    .bucket(bkN)
//                    .object("")
//                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
//                    .build());
//        } catch (Exception ignored) {
//        }
//
//
//    }
//

}
