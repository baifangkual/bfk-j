package io.github.baifangkual.jlib.db.impl.pool;

/**
 * 池类型，当中存放了可借用对象
 *
 * @author baifangkual
 * @since 2024/7/26
 */
public interface Poolable<T extends Poolable.Borrowable> {

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
     * 可借用的对象，与Pool联系，标记接口
     */
    interface Borrowable {
        /**
         * 该方法被调用时将this(self)放回池中
         */
        void recycleSelf();
        /*
         * 20250607：现在看来，该接口声明不太好，因为要达成这样的行为，
         * 必须要Borrowable实例能访问到Poolable实例，
         * 或者说，Borrowable直接或间接的持有了Poolable的引用，
         * 要知道，不是所有场景的Borrowable都适合这样，遂该接口及
         * Poolable接口不应扩大至jlib-core内的trait
         */
    }
}
