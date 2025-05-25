package io.github.baifangkual.bfk.j.mod.vfs.smb;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.connection.NegotiatedProtocol;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Set;

/**
 * @author baifangkual
 * @since 2024/8/27
 */
@SuppressWarnings({"CommentedOutCode", "unused"})
@Slf4j
public class SMBJTest {

//    final String user = "";
//    final String pass = "";
//    final String ipv6 = "";
//    final String ipv4 = "192.";
//
//    @Test
//    public void testConn() throws IOException {
//
//        SMBClient client = new SMBClient();
//
//        try (Connection connection = client.connect(ipv4)) {
//            AuthenticationContext ac = new AuthenticationContext(user, pass.toCharArray(), null);
//            Session session = connection.authenticate(ac);
//            NegotiatedProtocol negotiatedProtocol = connection.getNegotiatedProtocol();
//            System.out.println(STF.f("protocol: {}", negotiatedProtocol));
//
//            // Connect to Share
//            try (DiskShare share = (DiskShare) session.connectShare("")) {
//                for (FileIdBothDirectoryInformation f : share.list("")) {
//                    System.out.println("File : " + f.getFileName());
//
//                }
//            }
//        }
//        client.close();
//
//    }
//
//    @Test
//    public void testConn2() throws IOException {
//
//        SMBClient client = new SMBClient();
//
//        try (Connection connection = client.connect(ipv4)) {
//            AuthenticationContext ac = new AuthenticationContext(user, pass.toCharArray(), null);
//            Session session = connection.authenticate(ac);
//            NegotiatedProtocol negotiatedProtocol = connection.getNegotiatedProtocol();
//            System.out.println(STF.f("protocol: {}", negotiatedProtocol));
//
//            // Connect to Share
//            try (DiskShare share = (DiskShare) session.connectShare("")) {
//                for (FileIdBothDirectoryInformation f : share.list(null)) {
//                    System.out.println("File : " + f.getFileName());
//
//                }
//            }
//        }
//        client.close();
//
//    }
//
//    @Test
//    public void testReadFile() throws IOException {
//
//
//        final int smbCiPort = 445;
//        final String user = "";
//        final String pass = "";
//        final String ipv6 = "";
//        final String share = "";
//
//        final String rPath = "";
//        final String rFileP = "100MIB_file";
//
//        // default
//        SmbConfig conf = SmbConfig.createDefaultConfig();
//        conf = SmbConfig.builder(conf)
//                .withReadBufferSize(1024 * 1024 * 2).build();
//        int readBufferSize = conf.getReadBufferSize();
//        log.info(":: readBufferSize: {}", readBufferSize);
//
//        // auth
//        AuthenticationContext ac = new AuthenticationContext(user, pass.toCharArray(), null);
//
//        // conn
//        try (SMBClient smbClient = new SMBClient(conf);
//             Connection conn = smbClient.connect(ipv6, smbCiPort)) {
//            Session session = conn.authenticate(ac);
//            NegotiatedProtocol negotiatedProtocol = conn.getNegotiatedProtocol();
//            log.info(":: protocol: {}", negotiatedProtocol);
//
//            DiskShare dShare = (DiskShare) session.connectShare(share);
//
//            try (File file = dShare.openFile(rPath + "/" + rFileP,
//                    Set.of(AccessMask.FILE_READ_DATA),
//                    null,
//                    Set.of(SMB2ShareAccess.FILE_SHARE_READ),
//                    SMB2CreateDisposition.FILE_OPEN,
//                    null
//            );) {
//                try (BufferedInputStream bufIp = new BufferedInputStream(file.getInputStream())) {
//                    final byte[] buf = new byte[readBufferSize];
//                    try (BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream("./COPY_100MIB_FILE"))) {
//                        int bs;
//                        while ((bs = bufIp.read(buf)) != -1) {
//                            bufOut.write(buf, 0, bs);
//                        }
//                        log.info(":: end of read copy");
//                    }
//                }
//            }
//        }
//
//    }
//
//    @Test
//    public void testWriteFile() throws IOException {
//
//        final int smbCiPort = 445;
//        final String user = "";
//        final String pass = "";
//        final String ipv6 = "";
//        final String share = "";
//
//        final String rPath = "";
//        final String rFileP = "WRITE_100MIB_file1";
//
//        // default
//        SmbConfig conf = SmbConfig.createDefaultConfig();
//        conf = SmbConfig.builder(conf)
//                .withWriteBufferSize(1024 * 1024 * 2).build();
//        int writeBufSize = conf.getWriteBufferSize();
//        log.info(":: writeBufSize: {}", writeBufSize);
//
//        // auth
//        AuthenticationContext ac = new AuthenticationContext(user, pass.toCharArray(), null);
//
//        // conn
//        try (SMBClient smbClient = new SMBClient(conf);
//             Connection conn = smbClient.connect(ipv6, smbCiPort)) {
//            Session session = conn.authenticate(ac);
//            NegotiatedProtocol negotiatedProtocol = conn.getNegotiatedProtocol();
//            log.info(":: protocol: {}", negotiatedProtocol);
//            DiskShare dShare = (DiskShare) session.connectShare(share);
//            try (File file = dShare.openFile(rPath + "/" + rFileP,
//                    Set.of(AccessMask.FILE_WRITE_DATA),
//                    null,
//                    Set.of(SMB2ShareAccess.FILE_SHARE_WRITE),
//                    SMB2CreateDisposition.FILE_CREATE,
//                    Set.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH)
//            );) {
//                try (BufferedOutputStream bufOt = new BufferedOutputStream(file.getOutputStream())) {
//                    final byte[] buf = new byte[writeBufSize];
//                    try (BufferedInputStream bufIn = new BufferedInputStream(new FileInputStream("./COPY_100MIB_FILE"))) {
//                        int bs;
//                        while ((bs = bufIn.read(buf)) != -1) {
//                            bufOt.write(buf, 0, bs);
//                        }
//                        log.info(":: end of write copy");
//                    }
//                }
//            }
//        }
//
//    }


}
