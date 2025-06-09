package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.*;
import io.github.baifangkual.jlib.db.exception.DBQueryException;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.impl.pool.ConnPoolDBC;
import io.github.baifangkual.jlib.db.trait.MetaProvider;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

/**
 * DBC顶层抽象，描述了DBC接收并校验cfg等过程
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public abstract class BaseDBC implements DBC {

    private final Cfg readonlyCfg;

    /**
     * 该构造或可应为所有实现的父类，该类规范构造的生命周期过程，pre -> check -> post,
     * 该构造可能会改变传递的config的内部状态（因为发生在内部的配置的复制行为仅复制浅引用），
     * 当所有该构造定义的内部状态走完后，引用的connConfig将变为不可变对象
     *
     * @param cfg 数据源连接配置，具体配置查看{@link DBCCfgOptions}
     */
    public BaseDBC(Cfg cfg) {
        Objects.requireNonNull(cfg);
        /* 不应改变外界传递的对象 只复制浅引用 */
        final Cfg cf = Cfg.ofMap(cfg.toReadonlyMap());
        preCheckCfg(cf);
        throwOnIllegalCfg(cf);
        postCheckCfg(cf);
        this.readonlyCfg = cf.toReadonly();
    }



    /**
     * 返回该 DBC 的 jdbcUrl（额外参数不在url中体现）
     *
     * @return jdbcUrl
     */
    public abstract String jdbcUrl();


    @Override
    public abstract FnAssertValidConnect fnAssertValidConnect();

    @Override
    public PooledDBC pooled(int maxPoolSize) {
        return new ConnPoolDBC(this, maxPoolSize);
    }

    @Override
    public PooledDBC pooled() {
        return pooled(readonlyCfg().getOrDefault(DBCCfgOptions.poolMaxSize));
    }

    public List<Table.Meta> tablesMeta() {
        MetaProvider metaProvider = metaProvider();
        try (Connection conn = getConn()) {
            return metaProvider.tablesMeta(conn, readonlyCfg());
        } catch (Exception e) {
            throw new DBQueryException(e.getMessage(), e);
        }
    }

    public List<Table.ColumnMeta> columnsMeta(String table) {
        Objects.requireNonNull(table, "given table is null");
        MetaProvider metaProvider = metaProvider();
        try (Connection conn = getConn()) {
            return metaProvider.columnsMeta(conn, readonlyCfg(), table);
        } catch (Exception e) {
            throw new DBQueryException(e.getMessage(), e);
        }
    }

    public <ROWS> ROWS tableData(String table, int pageNo, int pageSize,
                                 ResultSetExtractor<? extends ROWS> rsExtractor) {
        Objects.requireNonNull(table, "given table is null");
        MetaProvider metaProvider = metaProvider();
        try (Connection conn = getConn()) {
            return metaProvider.tableData(conn, readonlyCfg(), table, pageNo, pageSize, rsExtractor);
        } catch (Exception e) {
            throw new DBQueryException(e.getMessage(), e);
        }
    }

    /**
     * @apiNote 该方法实际是委托至 {@link #tableData(String, int, int, ResultSetExtractor)} 方法并给定分页参数默认值
     * {@code pageNo = 1, pageSize = Integer.MAX_VALUE}，这可能会对数据库造成一定的计算性能消耗，
     * 且可能数据库的分页参数并不支持 Integer.MAX_VALUE 这么大，遂该方法后续应重写为简单的表查询即可，而不应该委托至分页查询，
     * 而且 {@code tableData} 系方法参数及返回值已修改，通过 {@link ResultSetExtractor} 函数，外界可自由控制读取行数，
     * 遂对于原本的分页查询实现 {@link #tableData(String, int, int, ResultSetExtractor)}，可能应做到覆盖
     */
    public <ROWS> ROWS tableData(String table,
                                 ResultSetExtractor<? extends ROWS> rsExtractor) {
        return tableData(table, 1, Integer.MAX_VALUE, rsExtractor);
    }
    /**
     * 当前 {@link DBC} 的配置
     *
     * @return 配置
     */
    public Cfg readonlyCfg() {
        return readonlyCfg;
    }

    /**
     * 当前 {@link DBC} 类型的元数据提供者
     *
     * @return 元数据提供者
     */
    public abstract MetaProvider metaProvider();

    /**
     * 在检查前对Config对象做某些事，可改变该对象状态，默认空实现
     *
     * @param cfg 连接对象参数对象
     */
    protected void preCheckCfg(Cfg cfg) {
    }

    /**
     * 对Config对象参数做检查，不同的数据源连接有不同的连接检查方式
     *
     * @param cfg 连接对象参数对象
     * @throws IllegalDBCCfgException 当Config格式不合法
     */
    protected abstract void throwOnIllegalCfg(final Cfg cfg) throws IllegalDBCCfgException;

    /**
     * 在检查后对Config对象做某些事，可改变该对象状态，默认空实现
     *
     * @param cfg 连接对象参数对象
     */
    protected void postCheckCfg(Cfg cfg) {
    }


}
