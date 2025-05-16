package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * apache-commons-net 原生api的测试
 *
 * @author baifangkual
 * @since 2024/9/6
 */
@SuppressWarnings({"CommentedOutCode", "unused"})
@Slf4j
public class CommonsNetTest {

//    private final static String host = "192.";
//    private final static int port = 21;
//    private final static String username = "";
//    private final static String password = "";
//    private final static int bufSize = 1024 * 1024 * 8; // def 8MIB
//    private final static boolean passiveMode = true;
//
//    private FTPClient buildingCli() throws IOException {
//
//        //client config
//        FTPClientConfig clientConfig = new FTPClientConfig(); // default FTPClientConfig.SYST_UNIX
//        clientConfig.setServerTimeZoneId("Asia/Shanghai");
//        clientConfig.setUnparseableEntries(false);
//
//        //building
//        FTPClient ftpClient = new FTPClient();
//        ftpClient.setConnectTimeout(1000 * 10);
//        ftpClient.configure(clientConfig);
//        ftpClient.setControlEncoding("GBK");
//        ftpClient.setAutodetectUTF8(true); /* 是否启用服务器自动编码检测(仅支持UTF8) 连接前设置 */
//        ftpClient.setRemoteVerificationEnabled(false); /* 禁用主机验证 提高效率 */
//        ftpClient.connect(host, port);
//
//        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) { /* 查看服务器响应码 是否连接成功 */
//            ftpClient.disconnect();
//            throw new IOException("ftpClient connect to " + host + ":" + port + "fail");
//        }
//
//        ftpClient.login(username, password);
//
//        ftpClient.setBufferSize(bufSize); /* 设置传输缓冲区大小 */
//        // 当该为 false 时 不显示 隐藏文件，但 . 及 .. 也不显示了，在无 mlst 的服务器上 无法查看自身，遂 应当显示
//        ftpClient.setListHiddenFiles(true);
//
//        if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) { /* 连接后设置文件传输类型 重新连接将重置该设置 */
//            log.warn("设置FTP服务器文件格式失败 将使用服务器默认值");
//        }
//        if (passiveMode) {  //tod 最好为 被动模式 由客户端主动联系服务端 服务端的数据端口使用被动模式 方式客户端防火墙拦截
//            ftpClient.enterLocalPassiveMode();
//        }
//        return ftpClient;
//    }

//    @Test
//    public void testLsCurr() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        String s = ftpClient.printWorkingDirectory();
//        System.out.println(s);
//        // fix me 这里不报错，该问题应该处理，即即使目录并不存在，该也不会报错
//        FTPFile[] ftpFiles = ftpClient.listFiles("./.bashrc");
//        for (FTPFile f : ftpFiles) {
//            String name = f.getName();
//            System.out.println(name + "\t" + f.isDirectory());
//        }
//        String one = "./.config";
//        int sizeInt = ftpClient.size(one);
//        String sizeStr = ftpClient.getSize(one);
//        System.out.println(STF.f("size str: {}, size int: {}", sizeStr, sizeInt));
//        ftpClient.disconnect();
//
//    }
//
//    @Test
//    public void testLsRoot() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        String s = ftpClient.printWorkingDirectory();
//        System.out.println(s);
//        FTPFile[] ftpFiles = ftpClient.listFiles("testRoot", FTPFileFilters.ALL);
//        for (FTPFile f : ftpFiles) {
//            String name = f.getName();
//            System.out.println(name + "\t" + f.isDirectory());
//        }
//        ftpClient.disconnect();
//    }
//
//
//    @Test
//    public void testLastModificationTime() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        String modificationTime = ftpClient.getModificationTime("./.config");
//        System.out.println(modificationTime);
//        ftpClient.disconnect();
//    }
//
//
//    @Test
//    public void testLsOne() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        String s = ftpClient.printWorkingDirectory();
//        System.out.println(s);
//        FTPFile[] ftpFiles = ftpClient.listFiles();
//        for (FTPFile f : ftpFiles) {
//            String name = f.getName();
//            System.out.println(name + "\t" + f.isDirectory());
//        }
//        FTPFile ftpFile = ftpClient.mlistFile("/dir-create-by-bfk");
//        System.out.println(ftpFile);
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testUseListCMDShowOneInfo() throws IOException {
//        FTPClient ftpClient = buildingCli();
//
//        FTPFile[] ftpFiles = ftpClient.listFiles("./.bashrc", (f) -> {
//            if (f != null) {
//                return !f.isSymbolicLink();
//            }
//            return false;
//        });
//        for (FTPFile f : ftpFiles) {
//            String name = f.getName();
//            System.out.println(name + "\t" + f.isDirectory());
//        }
//
//
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testMLSTSupport() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        String s = ftpClient.printWorkingDirectory();
//        System.out.println(s);
//
//        // test support mlst
//        int i = ftpClient.sendCommand(FTPCmd.MLST, "/.");
//        boolean success = FTPReply.isPositiveCompletion(i);
//        System.out.println(success);
//
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void supportCmdLs() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        String s = ftpClient.listHelp();
//        System.out.println(s);
//
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testMkDirectory() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        boolean b = ftpClient.makeDirectory("./aaa中2文3/342");
//        System.out.println("mkdir result: " + b);
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testRmFile() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        boolean b = ftpClient.deleteFile("./fffl");
//        System.out.println("rm f result: " + b);
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testRMDirectory() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        boolean b = ftpClient.removeDirectory("./fff.txt");
//        System.out.println("rm d result: " + b);
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testTryGetInputStream() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void testLsLink() throws IOException {
//        FTPClient ftpClient = buildingCli();
//        FTPFile[] ftpFiles = ftpClient.listFiles(null, (f) -> {
//            if (f != null) {
//                if (!f.isDirectory()) {
//                    return true;
//                } else {
//                    return !".".equals(f.getName()) && !"..".equals(f.getName());
//                }
//            }
//            return false;
//        });
//        for (FTPFile f : ftpFiles) {
//            String name = f.getName();
//            System.out.println(STF.f("item:{}, isDir:{}, isLink:{}, isFile:{}", name, f.isDirectory(), f.isSymbolicLink(), f.isFile()));
//        }
//        ftpClient.disconnect();
//    }
//
//    @Test
//    public void processHelpString() {
//
//        String fileZaHelp = "214-The following commands are recognized.\n" +
//                            " NOP  USER TYPE SYST SIZE RNTO RNFR RMD  REST QUIT\n" +
//                            " HELP XMKD MLST MKD  EPSV XCWD NOOP AUTH OPTS DELE\n" +
//                            " CWD  CDUP APPE STOR ALLO RETR PWD  FEAT CLNT MFMT\n" +
//                            " MODE XRMD PROT ADAT ABOR XPWD MDTM LIST MLSD PBSZ\n" +
//                            " NLST EPRT PASS STRU PASV STAT PORT\n" +
//                            "214 Help ok.";
//        String vsftpdHelp = "214-The following commands are recognized.\n" +
//                            " ABOR ACCT ALLO APPE CDUP CWD  DELE EPRT EPSV FEAT HELP LIST MDTM MKD\n" +
//                            " MODE NLST NOOP OPTS PASS PASV PORT PWD  QUIT REIN REST RETR RMD  RNFR\n" +
//                            " RNTO SITE SIZE SMNT STAT STOR STOU STRU SYST TYPE USER XCUP XCWD XMKD\n" +
//                            " XPWD XRMD\n" +
//                            "214 Help OK.\n";
//
//        Pattern lineC = Pattern.compile("^[A-Z]{3,4}(\\s{1,2}[A-Z]{3,4})+$");
//        Pattern oneMatch = Pattern.compile("[A-Z]{3,4}");
//
//        String[] sp = vsftpdHelp.trim().split("\n");
//        Iterator<String> iter = Arrays.stream(sp)
//                .map(String::trim)
//                .filter(ul -> lineC.matcher(ul).matches())
//                .iterator();
//
//        List<String> r = new ArrayList<>();
//
//        while (iter.hasNext()) {
//            String l = iter.next();
//            Matcher m = oneMatch.matcher(l);
//            while (m.find()) {
//                r.add(m.group());
//            }
//        }
//
//        System.out.println(r);
//
//
//    }
//
//    @Test
//    public void testLsOneCmd() throws IOException {
//        FTPClient ftpClient = buildingCli();
//
//        FTPFile[] ftpFiles = ftpClient.listFiles(null, FTPFileFilters.ALL);
//
//        for (FTPFile f : ftpFiles) {
//            if (f != null) {
//                System.out.println(f.getName());
//            }
//        }
//
//
//        ftpClient.disconnect();
//    }


}
