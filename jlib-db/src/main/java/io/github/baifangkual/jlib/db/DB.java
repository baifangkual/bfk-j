package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.lang.R;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.exception.CloseConnectionException;
import io.github.baifangkual.jlib.db.exception.DBConnectException;
import io.github.baifangkual.jlib.db.exception.DBQueryException;
import io.github.baifangkual.jlib.db.exception.RSRowMappingException;
import io.github.baifangkual.jlib.db.impl.SimpleDBImpl;
import io.github.baifangkual.jlib.db.util.ResultSetc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.github.baifangkual.jlib.db.impl.abs.TypedJdbcUrlPaddingDBC.SQL_SELECT_1;
import static io.github.baifangkual.jlib.db.impl.ds.Oracle11gServerNameJdbcUrlDBC.SQL_ORACLE_SELECT_1;

/**
 * <b>数据库</b>
 * <p>不可变，本身线程安全，简单封装了数据库的查询方法</p>
 * <pre>{@code
 * DB db = DB.simple("jdbc://...",
 *                   "user", "***",
 *                   Map.of(),
 *                   FnAssertValidConnect.SELECT_1)
 *                   .assertConnect();
 * try(PooledDB pooled = db.pooled(...)) {
 *    ...
 * }
 * }</pre>
 *
 * @author baifangkual
 * @see FnAssertValidConnect
 * @since 2025/6/9
 */
public interface DB {

    /**
     * 创建一个简单封装的实例
     *
     * @param jdbcUrl              jdbcUrl
     * @param nullableUser         认证-user（nullable）
     * @param nullablePasswd       认证-passwd（nullable）
     * @param nullableJdbcParams   jdbc额外餐宿（nullable）
     * @param fnAssertValidConnect 断言连接可用的函数，可引用形如 {@link FnAssertValidConnect#SELECT_1}
     * @return DB实例
     */
    static DB simple(String jdbcUrl,
                     String nullableUser, String nullablePasswd,
                     Map<String, String> nullableJdbcParams,
                     FnAssertValidConnect fnAssertValidConnect) {
        return SimpleDBImpl.of(jdbcUrl, nullableUser, nullablePasswd, nullableJdbcParams, fnAssertValidConnect);
    }


    /**
     * 函数-断言连接对象是一个有效对象
     * <p>函数入参为一个连接对象，若并非有效对象，函数体应抛出异常以表示不是一个有效连接对象</p>
     * 函数体无需关闭给定的对象，对连接对象的关闭应由函数调用者负责，而非函数提供者
     * <p>函数应当是一个无副作用函数，以便安全的共享使用</p>
     */
    @FunctionalInterface
    interface FnAssertValidConnect {
        /**
         * 断言连接对象是一个有效对象，否则抛出异常
         *
         * @param conn 连接对象
         * @throws Exception 当连接对象不是一个有效对象
         */
        void assertIsValid(Connection conn) throws Exception;

        /**
         * 执行指定查询Sql以校验是否是一个有效连接对象的函数
         * <p>判定为有效连接对象的依据是执行完查询语句后返回一个带有查询结果的 {@link ResultSet}，
         * （{@link Statement#execute(String)} 形容的那般）</p>
         *
         * @param selectSql 查询sql
         * @return 函数
         */
        static FnAssertValidConnect withSelectSql(String selectSql) {
            return (conn) -> {
                //noinspection DuplicatedCode
                try (Statement stmt = conn.createStatement()) {
                    //noinspection SqlSourceToSinkFlow
                    if (stmt.execute(selectSql)) {
                        return;
                    }
                    throw new SQLException(Stf
                            .f("AssertConnect with sql: '{}', but not found any result", selectSql));
                }
            };
        }

        /**
         * exec sql 'SELECT 1'
         */
        FnAssertValidConnect SELECT_1 = FnAssertValidConnect.withSelectSql(SQL_SELECT_1);
        /**
         * oracle exec sql 'SELECT 1 FROM DUAL'
         */
        FnAssertValidConnect ORACLE_SELECT_1 = FnAssertValidConnect.withSelectSql(SQL_ORACLE_SELECT_1);

    }

    /**
     * 返回函数-函数能断言连接对象可用
     * <p>函数不负责关闭连接对象，连接对象应由函数调用者关闭，而非函数提供者
     *
     * @return 断言函数
     */
    FnAssertValidConnect fnAssertValidConnect();

    /**
     * 创建一个连接池
     * <p>多次调用该方法可创建多个连接池，创建的连接池彼此互不影响，
     * 连接池线程安全，多个线程从一个线程池能获取到不同的连接对象以供使用</p>
     * <pre>{@code
     * DB db = ...;
     * try(PooledDB pool1 = db.pooled(6)) {
     *     for (int i = 0; i < 10; i++) {
     *         CompletableFuture.runAsync(() -> {
     *             try (Connection conn = pool1.getConn()) {
     *                 ...
     *             } catch (SQLException e) {
     *                 ...
     *             }
     *         });
     *     }
     * }
     * try(PooledDB pool2 = db.pooled(9)) { ... }
     * }</pre>
     *
     * @param maxPoolSize 最大连接数
     * @return 连接池
     * @apiNote 连接池使用完成后应进行关闭 {@link PooledDB#close()}
     * @see PooledDB
     */
    PooledDB pooled(int maxPoolSize);


    /**
     * 返回该连接的 jdbcUrl
     *
     * @return jdbcUrl
     */
    String jdbcUrl();

    /**
     * 检查连接是否可用
     *
     * @return true为可用，反之不可用
     * @see #assertConnect()
     */
    default boolean testConnect() {
        try {
            assertConnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 断言能连接到数据库（即能够构建 {@link Connection} 并使用）
     * <p>即给定的参数能连接到数据库，也即表示给定的参数正确，否则抛出异常
     *
     * @return this
     * @throws DBConnectException 当尝试连接数据源失败
     * @see #testConnect()
     */
    DB assertConnect() throws DBConnectException;

    /**
     * 获取一个连接对象
     * <p>不负责conn对象的管理和关闭等，conn 使用结束之后的关闭应由调用方负责
     * <p>每次调用该方法将返回一个新的连接对象</p>
     *
     * @return {@link Connection}
     * @throws SQLException 当创建conn对象过程中发生异常
     */
    Connection getConn() throws SQLException;

    /**
     * 尝试获取一个连接对象，获取失败时返回 {@link R.Err} 携带异常（获取失败原因）
     *
     * @return {@code R.Ok(Connection)} | {@code R.Err(Exception)}
     */
    default R<Connection> tryGetConn() {
        return R.ofFnCallable(this::getConn);
    }

    /**
     * 执行sql查询并返回结果
     * <p>返回的结果将根据给定的 {@link RSRowMapping} 函数进行转换,
     * 与 {@link #execQuery(String, ResultSetExtractor)} 不同，要求的函数不应负责 {@link ResultSet#next()} 的调用，
     * 也因此，该函数 {@link RSRowMapping} 应仅从 {@link ResultSet} 中获取某一行的数据并返回 {@link ROW} 即可，
     * 方法内部会自动调用 {@code Result.next()}
     * <p>该方法不会检查sql注入问题
     * <pre>{@code
     * record Person(int id, String name, int age) { }
     * DB db = ...;
     * List<Indexed<Person>> indexedPerson = db.execQuery(
     *         "select id, name, age from person",
     *         LinkedList::new,
     *         // no need call rs.next(),
     *         // or you want to skip some row
     *         (rowNum, rs) -> Indexed.of(
     *                 rowNum, // just rs row number, not person id
     *                 new Person(rs.getInt("id"),
     *                         rs.getString("name"),
     *                         rs.getInt("age")
     *                 )
     *         )
     * );
     * }</pre>
     *
     * @param <ROW>       ResultSet中行转换为的行对象
     * @param sql         要执行的sql查询语句
     * @param listFactory 函数-提供一个List，形如 {@code ArrayList::new}
     * @param rowMap      函数-ResultSet中行的转换方法，函数入参为 {@code (int rowNum, ResultSet rs)}，
     *                    其中 {@code int rowNum} 表示当前 rs 中的行号（从0开始）
     * @return ResultSet中多个行, 构成了List[ROW...]
     * @apiNote 函数不应负责 {@link ResultSet#next()} 和 {@link ResultSet#close()} 的调用
     * @see RSRowMapping
     * @see #execQuery(String, ResultSetExtractor)
     */
    default <ROW> List<ROW> execQuery(String sql, Supplier<? extends List<ROW>> listFactory,
                                      RSRowMapping<? extends ROW> rowMap) throws DBQueryException {
        return this.execQuery(sql, ResultSetExtractor.fnListRowsByRsRowMapping(rowMap, listFactory));
    }

    /**
     * 执行sql查询并返回结果
     * <p>返回的结果将根据给定的 {@link ResultSetExtractor} 函数进行转换，该函数 {@link ResultSetExtractor}
     * 拥有 {@link ResultSet} 的完全控制权力，遂该函数内应负责显式调用 {@link ResultSet#next()} 方法，函数将 {@link ResultSet} 完全
     * 转为 {@link ROWS} 类型对象，该结果对象或包含多行数据
     * <p>该方法不会检查sql注入问题
     * <pre>{@code
     * record Person(int id, String name, int age) { }
     * DB db = ...;
     * List<Person> person10 = db.execQuery(
     *         "select id, name, age from person",
     *         (rs) -> {
     *             List<Person> personFirst10 = new ArrayList<>();
     *             int count = 0;
     *             // use FnResultSetCollector should call rs.next()
     *             // or you just want read one row
     *             while (count++ < 10 && rs.next()) {
     *                 Person p = new Person(rs.getInt("id"),
     *                         rs.getString("name"),
     *                         rs.getInt("age"));
     *                 personFirst10.add(p);
     *             }
     *             return personFirst10;
     *         }
     * );
     * }</pre>
     *
     * @param <ROWS>      返回值，查询结果
     * @param sql         要执行的sql查询语句
     * @param rsExtractor 函数-入参为ResultSet，返回值为 {@link ROWS} ,该函数或应负责 {@link ResultSet#next()} 的调用
     * @return 查询结果对象
     * @apiNote 函数不应负责 {@link ResultSet#close()} 的调用
     * @see ResultSetExtractor
     * @see #execQuery(String, Supplier, RSRowMapping)
     */
    default <ROWS> ROWS execQuery(String sql, ResultSetExtractor<? extends ROWS> rsExtractor)
            throws DBQueryException {
        //noinspection SqlSourceToSinkFlow
        try (Connection conn = getConn();
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery(sql)) {
            return ResultSetc.rows(rsExtractor, rs);
        } catch (Exception e) {
            throw new DBQueryException(e.getMessage(), e);
        }
    }

    /**
     * 执行sql查询并返回流结果
     * <p><b>使用完流后必须显式关闭流（或通过try-with-resource)，否则可能造成 Connection 对象无法回收资源</b></p>
     * <p>返回的流在不进行终端操作（terminal operation）时将不会从 {@link ResultSet} 中读取结果，
     * 即该方法适合查询大的数据量</p>
     * <pre>{@code
     * record Man(int id, String name) { }
     * DB db = ...;
     * // try-with-resource use stream, stream must close.
     * try (Stream<Man> personStream = db.execQuery(
     *                 "select * from man_table",
     *                 (rowNum, rs) -> new Man(
     *                         rs.getInt("id"),
     *                         rs.getString("name")
     *                 )
     *         )
     *         .limit(10)) {
     *     List<Man> p10 = personStream.toList();
     * }
     * }</pre>
     *
     * @param sql      要执行的查询sql
     * @param fnRowMap 函数-将一行数据转为某个对象，函数提供者不应在函数内显式调用 {@link ResultSet#next()} 和 {@link ResultSet#close()}
     * @param <ROW>    行对象
     * @return Stream
     * @throws DBQueryException 在完整构建Stream之前抛出各种异常时，
     *                          （对已创建的各个{@link AutoCloseable} 对象，
     *                          将尝试关闭其并将关闭过程的异常 {@code addSuppressed}）
     * @apiNote 使用方在使用Stream之后必须显式（或通过try-with-resource)关闭流，
     * 否则可能造成 Connection 对象无法回收资源。
     * <p>在完整构建Stream后，该方法返回一个由 {@link Spliterator} 表达的 {@link Spliterator#ORDERED} 且大小难以计算的串行流，
     * 在该流进行终端操作（terminal operation）时，因 {@link RSRowMapping} 造成的异常将被作为抛出的 {@link RSRowMappingException} 的 cause;
     * 在流关闭时 {@link Stream#close()} ，将在流中注册的回调钩子函数中关闭该次查询关联的 {@link Connection}、
     * {@link java.sql.Statement}、{@link ResultSet}，其中，
     * 在关闭 {@link Connection}时的异常将作为抛出的异常 {@link CloseConnectionException} 的 cause,
     * 在关闭 {@link java.sql.Statement} 时的异常将作为关闭 {@link Connection} 时的异常的 {@link Throwable#getSuppressed()},
     * 在关闭 {@link ResultSet} 时的异常将作为关闭 {@link java.sql.Statement} 时的异常的 {@link Throwable#getSuppressed()}，
     * 关闭顺序为 {@code RS -> Stmt -> Conn}，在关闭的先后顺序中，若后一个关闭时没有发生异常，
     * 则{@link Throwable#addSuppressed(Throwable)} 到更后一个，
     * 直到作为 {@link CloseConnectionException} 的 cause</p>
     * <p>该流虽可以转为并行流，但其拆分的 {@link Spliterator} 为有限并行的 {@link Spliterator#trySplit()}，
     * 遂最好不用转为并行流，因为性能不会提升多少</p>
     */
    default <ROW> Stream<ROW> execQuery(String sql, RSRowMapping<? extends ROW> fnRowMap)
            throws DBQueryException {
        try { // 不能 try-with-resource，因为stream还没到终端操作...
            Connection conn = getConn();
            return ResultSetc.stream(sql, conn, fnRowMap);
        } catch (Exception e) {
            throw new DBQueryException(e.getMessage(), e);
        }
    }


}
