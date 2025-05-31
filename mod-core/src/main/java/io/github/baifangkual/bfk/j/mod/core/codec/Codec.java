package io.github.baifangkual.bfk.j.mod.core.codec;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <b>编解码器 (Codec)</b>
 * <p>所有实现都应保证：
 * <pre>{@code
 * Assert.eq(data, decode(encode(data)));
 * }</pre>
 *
 * @param <D> 编码前类型(解码后类型)
 * @param <C> 编码后类型(解码前类型)
 * @author baifangkual
 * @since 2025/5/30 v0.0.7
 */
public interface Codec<D, C> {

    /**
     * 编码<br>
     * 一般约束：不允许给定的需编码实体为 {@code null}
     *
     * @param data 数据
     * @return 编码后数据
     */
    C encode(D data);

    /**
     * 解码
     * 一般约束：不允许给定的需解码实体为 {@code null}
     *
     * @param encoded 编码后数据
     * @return 数据
     */
    D decode(C encoded);


    /**
     * 链式调用，多次编解码
     * <pre>{@code
     * byte[] result = Codec.chain(data)
     *     .gzip()
     *     .base62()
     *     .getBytes();
     * }</pre>
     *
     * @deprecated 应用面太小，后续或删除
     */
    @Deprecated(forRemoval = true)
    class Chain {
        private byte[] data;
        private final List<Function<byte[], byte[]>> encodeFunctions = new ArrayList<>();

        public Chain(byte[] data) {
            this.data = data;
        }

        public Chain add(Function<byte[], byte[]> function) {
            encodeFunctions.add(function);
            return this;
        }

        public byte[] toBytes() {
            for (Function<byte[], byte[]> function : encodeFunctions) {
                data = function.apply(data);
            }
            return data;
        }

        public String toString(Charset cs) {
            return new String(toBytes(), cs);
        }

        public String toString() {
            return toString(UTF_8);
        }
    }

}
