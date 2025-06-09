package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.trait.Closeable;
import io.github.baifangkual.jlib.db.exception.DBConnectException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * <b>连接对象池化的DB</b>
 * <p>线程安全，能够管理自己生产的 {@link Connection} 对象生命周期，
 * 相对于仅能提供Connection对象的不可变的 {@link DB} 类型，该类型在生命周期内内部状态将会有变化
 * <p>使用完后应关闭连接池 {@link PooledDB#close()} ，以释放持有的连接对象资源</p>
 * <p>该池通过 {@link #getConn()} 提供的连接对象使用完后应显式关闭（或通过try-with-resource),
 * 以此将连接对象归还到连接池中</p>
 *
 * @author baifangkual
 * @see #close()
 * @since 2025/6/9 v0.1.1
 */
public interface PooledDB extends DB, Closeable {


    @Override
    PooledDB assertConnect() throws DBConnectException;

    /**
     * 从连接池中获取一个可用的连接对象
     * <p>若没有空闲可用连接对象，且连接对象数量已达到最大值，
     * 则调用该方法的线程将阻塞直到任意一个连接可用</p>
     *
     * @return 连接对象
     * @throws SQLException 当创建或返回{@link Connection} 过程发生异常时
     */
    @Override
    Connection getConn() throws SQLException;

    /**
     * 关闭连接池
     *
     * @throws Exception 当close过程发生异常
     */
    @Override
    void close() throws Exception;

    /**
     * 连接池是否已被关闭
     * <p>已被关闭的连接池不应再使用
     *
     * @return true 已被关闭，反之则未被关闭
     */
    @Override
    boolean isClosed();

    /*
    因为该的线程安全特性，遂可以提供方法签名形如 asyncXXX...
    可不对外暴露Conn和Rs生命周期等，外界仅提供查询需求如sql和将rs转为ROWS或ROW的函数等
    后续可追加相关API
     */


}
