package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import io.github.baifangkual.bfk.j.mod.core.func.FnGet;
import io.github.baifangkual.bfk.j.mod.core.func.FnRun;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对FTP协议相关命令等做校验
 *
 * @author baifangkual
 * @since 2024/9/10 v0.0.5
 */
@Slf4j
class FTPSupport {

    static final String MAGIC_POINT = ".";
    static final String MAGIC_PRO_POINT = "..";
    static final Pattern LINE_MATCH = Pattern.compile("^[A-Z]{3,4}(?:\\s{1,2}[A-Z]{3,4})+$");
    static final Pattern ITEM_MATCH = Pattern.compile("[A-Z]{3,4}");
    /**
     * 过滤器 不可包含 null 符号链接 魔法目录
     */
    static final FTPFileFilter NOT_NULL_AND_NOT_MAGIC_AND_NOT_LINK = (FTPFile f) -> {
        if (f != null) {
            String name = f.getName();
            return !f.isSymbolicLink() // 不可为链接
                   && !MAGIC_POINT.equals(name) //不可为当前
                   && !MAGIC_PRO_POINT.equals(name); // 不可为上一级
        }
        return false;
    };
    /**
     * 过滤器 不可包含 null 符号链接
     */
    static final FTPFileFilter NOT_NULL_AND_NOT_LINK = (FTPFile f) -> {
        if (f != null) {
            return !f.isSymbolicLink();
        }
        return false;
    };
    /**
     * 过滤器 不可包含 null 魔法目录
     */
    static final FTPFileFilter NOT_NULL_AND_NOT_MAGIC = (FTPFile f) -> {
        if (f != null) {
            if (!f.isDirectory()) {
                return true;
            } else {
                String name = f.getName();
                return !MAGIC_POINT.equals(name)
                       && !MAGIC_PRO_POINT.equals(name);
            }
        }
        return false;
    };

    /**
     * 解析FTP HELP 命令的结果，返回FTP服务端支持的命令列表
     *
     * @param help HELP 命令返回值
     * @return FTP服务器支持的命令列表
     */
    static List<String> analysisSupportCMDList(String help) {
        if (help == null) return Collections.emptyList();
        String[] sp = help.trim().split("\n");
        Iterator<String> iter = Arrays.stream(sp)
                .map(String::trim)
                .filter(ul -> !ul.isEmpty())
                .filter(ul -> LINE_MATCH.matcher(ul).matches())
                .iterator();
        List<String> r = new ArrayList<>();
        while (iter.hasNext()) {
            String l = iter.next();
            Matcher m = ITEM_MATCH.matcher(l);
            while (m.find()) {
                r.add(m.group());
            }
        }
        return r;
    }

    /**
     * 查看ftp服务器是否支持基本的命令，这些命令能使FTP Vfs 发挥基本功能，
     * 若为false，ftp vfs 应当 在 构建时 异常，因为无法支撑基本功能要求
     *
     * @return true 提供基本支持，false 不提供基本支持
     */
    static boolean isBasicSupport(List<String> helps) {
        if (!supportMLST(helps)) {
            log.warn("连接的 FTP 服务器不支持MLST命令，将导致toFile相关API低效");
        }
        return supportLIST(helps)
               && supportDELE(helps)
               && supportMKD(helps)
               && supportRMD(helps)
               && supportRETR(helps)
               && supportSTOR(helps);
    }

    static final String CMD_MLST = "MLST";

    /**
     * 给定命令列表，查看是否支持 MLST 命令 单个详情
     */
    static boolean supportMLST(List<String> helps) {
        return helps.contains(CMD_MLST);
    }

    static final String CMD_LIST = "LIST";

    /**
     * 给定命令列表，查看是否支持 LIST 命令 多个详情，目录内部 或 单文件
     */
    static boolean supportLIST(List<String> helps) {
        return helps.contains(CMD_LIST);
    }

    static final String CMD_SIZE = "SIZE";

    /**
     * 给定命令列表，查看是否支持 SIZE 命令 大小
     */
    static boolean supportSIZE(List<String> helps) {
        return helps.contains(CMD_SIZE);
    }

    static final String CMD_MDTM = "MDTM";

    /**
     * 给定命令列表，查看是否支持 MDTM 命令 最后修改时间
     */
    static boolean supportMDTM(List<String> helps) {
        return helps.contains(CMD_MDTM);
    }

    static final String CMD_DELE = "DELE";

    /**
     * 给定命令列表，查看是否支持 DELE 命令 删除文件
     */
    static boolean supportDELE(List<String> helps) {
        return helps.contains(CMD_DELE);
    }

    static final String CMD_MKD = "MKD";

    /**
     * 给定命令列表，查看是否支持 MKD 命令 创建文件夹
     */
    static boolean supportMKD(List<String> helps) {
        return helps.contains(CMD_MKD);
    }

    static final String CMD_RMD = "RMD";

    /**
     * 给定命令列表，查看是否支持 RMD 命令 删除文件夹
     */
    static boolean supportRMD(List<String> helps) {
        return helps.contains(CMD_RMD);
    }

    static final String CMD_RETR = "RETR";

    /**
     * 给定命令列表，查看是否支持 RETR 命令 返回要求的文件字节流
     */
    static boolean supportRETR(List<String> helps) {
        return helps.contains(CMD_RETR);
    }

    static final String CMD_STOR = "STOR";

    /**
     * 给定命令列表，查看是否支持 STOR 命令 接收文件字节流，生成文件，默认行为为覆盖原有文件
     */
    static boolean supportSTOR(List<String> helps) {
        return helps.contains(CMD_STOR);
    }


    static <R> R sneakyRun(FnGet<R> fn) {
        try {
            return fn.unsafeGet();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void sneakyRun(FnRun fn) {
        sneakyRun(() -> {
            fn.run();
            return null;
        });
    }


}
