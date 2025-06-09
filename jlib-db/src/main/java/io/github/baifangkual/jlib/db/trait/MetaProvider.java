package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.DBC;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.ResultSetExtractor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

/**
 * <b>数据库元数据提供者</b>
 * <p>无状态对象，接收conn对象，根据给定参数返回所需数据库的元数据等<br>
 * 所有类型实现仅描述行为，不应存储状态或可变状态<br>
 * 该不负责关闭从外侧传递的入参{@link Connection}，仅获取部分数据，即该类型当中描述的是获取某种类型数据库的元数据的逻辑<br>
 * 一种类型数据库{@link DBC}默认行为对应一种类型的{@link MetaProvider}<br>
 * 该对象应当仅有实现对某db的部分数据的输出要求，该应为无状态对象或不可变对象，应为无参构造，以便{@link DBC}对象可方便共享引用该
 *
 * @author baifangkual
 * @apiNote 外部不应当持有该实例引用，该实例仅为方便下级各数据库实现，
 * 该实例中方法已完全在 {@link DBC} 提供的方法中有所体现，也因此，该接口实现不应对参数做任何校验，
 * 因为这些校验都应由外界或 {@link DBC} 进行
 * @since 2024/7/12
 */

public interface MetaProvider {

    /*
    想要实现形如 DBCFactory.simpleFromUrl(jdbcUrl, nullableLoginProp, MetaProvider) 这样的方法
    比较困难，原因是 MetaProvider的接口签名要求参数 Cfg，
    而形如 SimpleDBC 这样的实现根本就没有cfg...
    细想后发现，tableMeta等等由MetaProvider提供的方法，DBC不应声明这些方法
    因为在纯jdbcUrl的DBC中，这些数据是无法从纯JdbcUrl中获取的（其各种表库schema等专有配置项及规则及sql等，
    特定于数据库提供商），遂若后续改造，仅需将tableMeta这些实现从DBC中移除，
    或者更上层设立DB接口封装execQuery等行为，而不提供tableMeta等这些特定于数据库的实现...
    ===================================
    后续或可做修改：
    DBC接口签名需提供 db schema等方法，DBC接口不应提供tablesMeta等方法，
    tableMeta等这些特定于数据库的实现的方法交由MetaProvider，MetaProvider变更签名，应完全提供 db，schema，table等这些参数的签名，
    而非为了防止空使用Cfg包装，这样，就不用提供下级重载接口了，各数据库类型实现即可，
    MetaProvider仍为无状态对象，即，可共享使用，即，可实例集中存在在形如MeteProviders等地方，因为特定于实现，遂实例通过DBType查询得到引用
    ... 或者完全废除MetaProvider？所有JdbcAPI的Meta获取仅由DefaultJdbcMetaSupports提供？
    另外，上浮一层 DB 接口，接收jdbcUrl构建，可向外提供Conn，可池化，接口签名方法 execQuery（sql...）这些
     */

    /**
     * 给定一个可用的连接对象和一个 {@link DBC} 的 {@link Cfg}，
     * 使用给定的连接对象查询数据库下所有的表的元数据
     *
     * @param conn   连接对象
     * @param config 配置-内应含有该查询中必要的信息
     * @return 库下所有表
     */
    List<Table.Meta> tablesMeta(Connection conn, Cfg config);

    /**
     * 给定一个可用的连接对象和一个 {@link DBC} 的 {@link Cfg} 和 表名，
     * 使用给定的连接对象查询某库下某表的所有列的元数据
     *
     * @param conn   连接对象
     * @param config 配置-内应含有该查询中必要的信息
     * @param table  要查询的表名
     * @return 表下所有列
     */
    List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table);

    /**
     * 给定一个可用的连接对象和一个 {@link DBC} 的 {@link Cfg} 和 表名 以及要查询的行的分页信息，
     * 使用给定的连接对象查询某库下某表中符合分页的行
     *
     * @param conn                 连接对象
     * @param config               配置-内应含有该查询中必要的信息
     * @param table                要查询的表名
     * @param pageNo               页码-分页参数 从1开始
     * @param pageSize             页大小-分页参数 从1开始
     * @param resultSetExtractor 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @return 表下符合分页的行信息
     * @apiNote 该方法是保守的，因为表中该数据量不确定，遂若表过大，
     * 给定较小的参数分页读取不会造成堆内存溢出
     */
    <ROWS> ROWS tableData(Connection conn, Cfg config, String table, int pageNo, int pageSize,
                          ResultSetExtractor<? extends ROWS> resultSetExtractor);


}
