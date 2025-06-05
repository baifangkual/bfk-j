package io.github.baifangkual.jlib.core.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * gzip 压缩 编解码器
 *
 * @author baifangkual
 * @since 2025/5/30 v0.0.7
 */
public class Gzip implements Codec<byte[], byte[]> {

    /**
     * 使用gzip压缩给定的字节序列
     */
    @Override
    public byte[] encode(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gos = new GZIPOutputStream(bos, data.length)) {
            gos.write(data);
            gos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("unable to gzip encode bytes", e);
        }
    }

    /**
     * 使用gzip解码data，data应为已压缩过的数据字节
     */
    @Override
    public byte[] decode(byte[] encoded) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(encoded.length);
             ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
             GZIPInputStream gis = new GZIPInputStream(bis, encoded.length)) {
            byte[] buffer = new byte[encoded.length];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("unable to gzip decode bytes", e);
        }
    }
}
