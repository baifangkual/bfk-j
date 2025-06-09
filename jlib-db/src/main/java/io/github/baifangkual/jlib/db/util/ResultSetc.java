package io.github.baifangkual.jlib.db.util;

import io.github.baifangkual.jlib.db.RSRowMapping;
import io.github.baifangkual.jlib.db.ResultSetExtractor;
import io.github.baifangkual.jlib.db.exception.CloseConnectionException;
import io.github.baifangkual.jlib.db.exception.DBQueryException;
import io.github.baifangkual.jlib.db.exception.RSRowMappingException;
import io.github.baifangkual.jlib.db.exception.ResultSetExtractException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Jdbc ResultSet Converter
 * 提供方法，将resultSet值转为多个行对象
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public class ResultSetc {
    private ResultSetc() {
        throw new AssertionError("utility class");
    }

    /**
     * 调用方给定{@link ResultSet}和操作ResultSet中行的函数{@link RSRowMapping},返回多行数据，
     * 该方法不负责调用{@link ResultSet#close()}
     *
     * @param rs       JDBC QUERY查询结果对象,该对象有状态
     * @param fnRowMap 函数-描述对{@link ResultSet}中行的操作，因为描述的为对行的操作，
     *                 遂该函数内不应显式调用{@link ResultSet#next()}，除非你有特殊需求（比如跳行读取）
     * @param <ROW>    通过{@link RSRowMapping}函数操作行和返回的对应行的结果
     * @return list[ROW...]
     */
    public static <ROW> List<ROW> rows(ResultSet rs,
                                       RSRowMapping<? extends ROW> fnRowMap) {
        try {
            Supplier<ArrayList<ROW>> listFactory = ArrayList::new;
            return ResultSetExtractor.fnListRowsByRsRowMapping(fnRowMap, listFactory)
                    .extract(rs);
        } catch (Exception e) {
            throw new RSRowMappingException(e.getMessage(), e);
        }
    }

    /**
     * 给定操作整个{@link ResultSet}的函数{@link ResultSetExtractor}和{@link ResultSet},返回通过函数转换而来的结果对象，该方法
     * 不同于{@link #rows(ResultSet, RSRowMapping)}方法，要求给定的函数为操作整个{@link ResultSet}的函数，遂要获取多行数据，应
     * 显式在函数体中调用{@link ResultSet#next()}
     *
     * @param rsExtractor 函数-完整读取整个 {@link ResultSet}
     * @param rs          JDBC QUERY 结果对象
     * @param <ROWS>      表示通过 {@link ResultSetExtractor} 函数操作后返回的结果类型
     * @return ROWS OBJ
     */
    public static <ROWS> ROWS rows(ResultSetExtractor<? extends ROWS> rsExtractor,
                                   ResultSet rs) {
        try {
            return rsExtractor.extract(rs);
        } catch (Exception e) {
            throw new ResultSetExtractException(e.getMessage(), e);
        }
    }

    /**
     * Closeable 的关闭钩子，适用于向 {@link Stream#onClose(Runnable)} 注册关闭钩子，
     * 尤其在构建Stream所需材料过程中多个需顺序关闭的对象时，
     * 使用 {@link #nest(AutoCloseable)} 顺序堆叠多个需关闭的对象
     * <p>参考：
     * <p><a href="https://stackoverflow.com/questions/32209248/java-util-stream-with-resultset">stream-with-resultset</a></p>
     */
    @FunctionalInterface
    interface UncheckedCloseHook extends Runnable, AutoCloseable {
        default void run() {
            try {
                close();
            } catch (Exception ex) {
                throw new CloseConnectionException(ex);
            }
        }

        static UncheckedCloseHook wrap(AutoCloseable c) {
            return c::close; // 将UncheckedCloseHook的Close方法引用
        }

        default UncheckedCloseHook nest(AutoCloseable c) {
            return () -> { //noinspection unused
                try (UncheckedCloseHook c1 = this) {
                    c.close();
                }
            };
        }

        @Override
        void close() throws Exception;
    }

    /**
     * 给定需执行的sql，和执行sql的连接对象，返回一个Stream
     * <p>外界不能显式在该方法返回后关闭给定的连接对象，因为此时流还没有消费其中元素</p>
     * <p><b>该方法会在 {@link Stream#onClose(Runnable)} 绑定关闭 Conn Stmt RS 对象，
     * 遂在流的使用方使用完流后应显式关闭流（或通过try-with-resource)</b></p>
     * <p>参考：
     * <p><a href="https://stackoverflow.com/questions/32209248/java-util-stream-with-resultset">stream-with-resultset</a></p>
     * <p><a href="https://stackoverflow.com/questions/64390132/consuming-a-database-cursor-using-a-java-stream">database-cursor-using-a-java-stream</a>
     * <p>以及 {@code org.springframework.jdbc.core.JdbcTemplate.queryForStream(...)} 系列方法
     *
     * @param sql      要执行的查询sql
     * @param conn     连接对象
     * @param fnRowMap 函数-将一行数据转为某个对象，函数提供者不应在函数内显式调用 {@link ResultSet#next()} 和 {@link ResultSet#close()}
     * @param <ROW>    行对象
     * @return Stream
     * @throws SQLException 在完整构建Stream之前抛出各种异常时，
     *                      （对已创建的各个{@link AutoCloseable} 对象，
     *                      将尝试关闭其并将关闭过程的异常 {@code addSuppressed}）
     * @apiNote 使用方在使用Stream之后应显式（或通过try-with-resource)关闭流，
     * 否则，可能造成Connection对象和ResultSet对象无法回收资源。
     * <p>在完整构建Stream之前若抛出异常，该方法将捕获该异常并抑制到抛出的 {@link DBQueryException} 中，
     * 在完整构建Stream后，该方法返回一个由 {@link Spliterator} 表达的 {@link Spliterator#ORDERED} 且大小难以计算的非并行流，
     * 在该流进行终端操作（terminal operation）时，因 {@link RSRowMapping} 造成的异常将被作为抛出的 {@link RSRowMappingException} 的 cause;
     * 在流关闭时 {@link Stream#close()} ，将在流中注册的回调钩子函数中关闭该次查询关联的 {@link Connection}、
     * {@link java.sql.Statement}、{@link ResultSet}，其中，
     * 在关闭 {@link Connection}时的异常将作为抛出的异常 {@link CloseConnectionException} 的 cause,
     * 在关闭 {@link java.sql.Statement} 时的异常将作为关闭 {@link Connection} 时的异常的 {@link Throwable#getSuppressed()},
     * 在关闭 {@link ResultSet} 时的异常将作为关闭 {@link java.sql.Statement} 时的异常的 {@link Throwable#getSuppressed()}，
     * 关闭顺序为 {@code RS -> Stmt -> Conn}，若下一个对象关闭时没有发生异常，则{@link Throwable#addSuppressed(Throwable)} 到更下一个，
     * 直到作为 {@link CloseConnectionException} 的 cause</p>
     */
    public static <ROW> Stream<ROW> stream(String sql,
                                           Connection conn,
                                           RSRowMapping<? extends ROW> fnRowMap)
            throws SQLException {
        UncheckedCloseHook close = null;
        try {
            close = UncheckedCloseHook.wrap(conn);
            //noinspection SqlSourceToSinkFlow
            PreparedStatement pSt = conn.prepareStatement(sql);
            close = close.nest(pSt);
            /*
            // 部分数据库提供商可能没有实现该 fetch
            // DatabaseMetaData md = conn.getMetaData();
            // if (md.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY)) {}
            // 也不应当在这里判定，否则每次构建Stream进行查询时都要获取元数据
            // 应当在 simpleDBImpl中记录是否支持fetch，因为这是数据库类型强相关的
            // 先暂时不进行该修改，后续若某数据库类型setFetch出问题时在进行该修改
             */
            pSt.setFetchSize(1000);
            ResultSet resultSet = pSt.executeQuery();
            close = close.nest(resultSet);
            return StreamSupport.stream(new Spliterators.AbstractSpliterator<ROW>(
                    Long.MAX_VALUE, Spliterator.ORDERED) {
                int rowNum = 0;

                @Override
                public boolean tryAdvance(Consumer<? super ROW> action) {
                    try {
                        if (!resultSet.next()) return false;
                        action.accept(fnRowMap.map(rowNum++, resultSet));
                        return true;
                    } catch (Exception ex) {
                        throw new RSRowMappingException(ex);
                    }
                }

            }, false).onClose(close);
        } catch (SQLException errOnBuildingStream) {
            if (close != null)
                try {
                    close.close();
                } catch (Exception ex) {
                    errOnBuildingStream.addSuppressed(ex);
                }
            throw errOnBuildingStream;
        }
    }


    /**
     * 给定ResultSet，直接返回读取ResultSet的结果，多行，一行表现为一个{@link Object}数组
     *
     * @param rs JDBC QUERY 结果集
     * @return [Object...]
     */
    public static List<Object[]> rows(ResultSet rs) {
        try {
            int colNum = rs.getMetaData().getColumnCount();
            List<Object[]> rows = new LinkedList<>();
            while (rs.next()) {
                Object[] row = new Object[colNum];
                for (int i = 1; i <= colNum; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            throw new RSRowMappingException(e.getMessage(), e);
        }
    }

}
