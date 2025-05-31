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



}
