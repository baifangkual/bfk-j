package cn.bfk.j.mod.core.trait.fn;

/**
 * @author baifangkual
 * create time 2025/5/3
 * <p>
 * <h3>安全函数标记接口</h3>
 * 被标记的函数使用{@link #toSafe()}方法变为安全函数后，执行过程的异常将被包装为{@link cn.bfk.j.mod.core.model.R}
 */
public interface Safe<FnSafe> {
    /**
     * 调用该方法以表示将函数转为“安全函数”
     *
     * @return 安全函数
     */
    FnSafe toSafe();

}
