package io.github.baifangkual.jlib.core.mark;

import io.github.baifangkual.jlib.core.lang.R;

/**
 * <b>安全函数标记接口</b><br>
 * 被标记的函数使用{@link #toSafe()}方法变为安全函数后，执行过程的异常将被包装为{@link R}
 *
 * @author baifangkual
 * @since 2025/5/3 v0.0.3
 */
public interface FnMutToSafe<FnSafe> {
    /**
     * 调用该方法以表示将函数转为“安全函数”
     *
     * @return 安全函数
     */
    FnSafe toSafe();

}
