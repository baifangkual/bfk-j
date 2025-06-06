package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.DBC;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.DBType;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.trait.MetaProvider;

import java.util.Objects;

/**
 * 抽象类，抽象了 dbc 中接收并校验cfg等过程
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public abstract class AbsDBC implements DBC {

    protected final Cfg readonlyCfg;
    protected final DBType dbType;

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
        this.dbType = cf.get(DBCCfgOptions.DS_TYPE);
        this.readonlyCfg = cf.toReadonly();
    }

    @Override
    public DBType type() {
        return dbType;
    }

    @Override
    public Cfg readonlyCfg() {
        return readonlyCfg;
    }

    @Override
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
