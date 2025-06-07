package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.*;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;
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
public abstract class AbsDBC implements DBC {

    private final Cfg readonlyCfg;
    private final DBType dbType;

    /**
     * 该构造或可应为所有实现的父类，该类规范构造的生命周期过程，pre -> check -> post,
     * 该构造可能会改变传递的config的内部状态（因为发生在内部的配置的复制行为仅复制浅引用），
     * 当所有该构造定义的内部状态走完后，引用的connConfig将变为不可变对象
     *
     * @param cfg 数据源连接配置，具体配置查看{@link DBCCfgOptions}
     */
    public AbsDBC(Cfg cfg) {
        Objects.requireNonNull(cfg);
        /* 不应改变外界传递的对象 只复制浅引用 */
        final Cfg cf = Cfg.ofMap(cfg.toReadonlyMap());
        preCheckCfg(cf);
        throwOnIllegalCfg(cf);
        postCheckCfg(cf);
        this.dbType = cf.get(DBCCfgOptions.type);
        this.readonlyCfg = cf.toReadonly();
    }

    @Override
    public PooledDBC pooled(int maxPoolSize) {
        return new ConnPoolDBC(this, maxPoolSize);
    }

    @Override
    public PooledDBC pooled() {
        return pooled(readonlyCfg().getOrDefault(DBCCfgOptions.maxPoolSize));
    }

    public List<Table.Meta> tablesMeta() {
        MetaProvider metaProvider = metaProvider();
        try (Connection conn = getConn()) {
            return metaProvider.tablesMeta(conn, readonlyCfg());
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    public List<Table.ColumnMeta> columnsMeta(String table) {
        Objects.requireNonNull(table, "given table is null");
        MetaProvider metaProvider = metaProvider();
        try (Connection conn = getConn()) {
            return metaProvider.columnsMeta(conn, readonlyCfg(), table);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    public <ROWS> ROWS tableData(String table, long pageNo, long pageSize,
                                 FnResultSetCollector<? extends ROWS> fnResultSetCollector) {
        Objects.requireNonNull(table, "given table is null");
        MetaProvider metaProvider = metaProvider();
        try (Connection conn = getConn()) {
            return metaProvider.tableData(conn, readonlyCfg(), table, pageNo, pageSize, fnResultSetCollector);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    public DBType type() {
        return dbType;
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
