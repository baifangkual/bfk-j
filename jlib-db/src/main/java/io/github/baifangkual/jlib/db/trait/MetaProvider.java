package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.DBC;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;

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
     * @param pageNo               页码-分页参数
     * @param pageSize             页大小-分页参数
     * @param fnResultSetCollector 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @return 表下符合分页的行信息
     * @apiNote 该方法是保守的，因为表中该数据量不确定，遂若表过大，
     * 给定较小的参数分页读取不会造成堆内存溢出
     */
    <ROWS> ROWS tableData(Connection conn, Cfg config, String table, long pageNo, long pageSize,
                          FnResultSetCollector<? extends ROWS> fnResultSetCollector);


}
