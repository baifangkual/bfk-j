package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.trait.DataSource;

import java.util.Objects;

/**
 * @author baifangkual
 * create time 2024/7/11
 */
public abstract class AbstractDataSource implements DataSource {

    protected final Cfg connConfig;

    /**
     * 该构造或可应为所有实现的父类，该类规范构造的生命周期过程，pre -> check -> post,
     * 该构造可能会改变传递的config的内部状态（因为发生在内部的配置的复制行为仅复制浅引用），
     * 当所有该构造定义的内部状态走完后，引用的connConfig将变为不可变对象
     *
     * @param connConfig 数据源连接配置，具体配置查看{@link io.github.baifangkual.jlib.db.constants.ConnConfOptions}
     */
    public AbstractDataSource(Cfg connConfig) {
        Objects.requireNonNull(connConfig);
        /* 不应改变外界传递的对象 20240826 该只能复制浅引用 */
        final Cfg dsDomainConfig = Cfg.ofMap(connConfig.toReadonlyMap());
        /* 允许在构造处改变Config状态 状态改变1 */
        preCheckConfig(dsDomainConfig); //
        throwOnConnConfigIllegal(dsDomainConfig);
        /* 允许在构造处改变Config状态 状态改变2 */
        postCheckConfig(dsDomainConfig);
        /* 20240826 该处限定，所有实例持有的配置引用都应为不可变引用 */
        this.connConfig = dsDomainConfig.toReadonly();
    }

    @Override
    public Cfg getConfig() {
        return connConfig;
    }

    /**
     * 在检查前对Config对象做某些事，可改变该对象状态，默认空实现
     *
     * @param config 连接对象参数对象
     */
    protected void preCheckConfig(Cfg config) {
    }

    /**
     * 对Config对象参数做检查，不同的数据源连接有不同的连接检查方式
     *
     * @param config 连接对象参数对象
     * @throws IllegalConnectionConfigException 当Config格式不合法
     */
    protected abstract void throwOnConnConfigIllegal(final Cfg config) throws IllegalConnectionConfigException;

    /**
     * 在检查后对Config对象做某些事，可改变该对象状态，默认空实现
     *
     * @param config 连接对象参数对象
     */
    protected void postCheckConfig(Cfg config) {
    }


}
