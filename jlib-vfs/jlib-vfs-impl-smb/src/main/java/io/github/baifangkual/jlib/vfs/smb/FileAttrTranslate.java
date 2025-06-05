package io.github.baifangkual.jlib.vfs.smb;

/**
 * <b>smb file attr 32bit 消息:</b><br>
 * <ul>
 *     <li>0x01：Read-only file attribute</li>
 *     <li>0x02：Hidden file attribute</li>
 *     <li>0x04：System file attribute</li>
 *     <li>0x10：Directory file attribute</li>
 *     <li>0x20： Archive file attribute</li>
 *     <li>0x40： Device file attribute</li>
 *     <li>0x80： Normal file attribute</li>
 * </ul>
 *
 * <a href="https://learn.microsoft.com/zh-cn/openspecs/windows_protocols/ms-smb/65e0c225-5925-44b0-8104-6b91339c709f">微软的SMBFileAttr消息</a>
 *
 * @author baifangkual
 * @since 2024/8/29 v0.0.5
 */
class FileAttrTranslate {

    private FileAttrTranslate() {
        throw new UnsupportedOperationException("utility class");
    }

    final static int ATTR_IS_DIRECTORY = 0x10;

    static boolean isDir(long fileAttr) {
        return (fileAttr & ATTR_IS_DIRECTORY) == ATTR_IS_DIRECTORY;
    }

}
