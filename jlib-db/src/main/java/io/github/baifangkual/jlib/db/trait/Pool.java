package io.github.baifangkual.jlib.db.trait;

/**
 * @author baifangkual
 * create time 2024/7/26
 * <p>
 * 池类型，当中存放了可借用对象
 */
public interface Pool<T extends Pool.Borrowable> {

    /**
     * 从池中借用一个对象
     *
     * @return 被借用对象
     */
    T borrow();

    /**
     * 回收一个可借用对象
     *
     * @param borrowable 可借用对象
     */
    void recycle(T borrowable);

    /**
     * @author baifangkual
     * create time 2024/7/26
     * <p>
     * 可借用的对象，与Pool联系，标记接口
     */
    interface Borrowable {
        /**
         * 该方法被调用时将this(self)放回池中
         */
        void recycleSelf();
    }
}
