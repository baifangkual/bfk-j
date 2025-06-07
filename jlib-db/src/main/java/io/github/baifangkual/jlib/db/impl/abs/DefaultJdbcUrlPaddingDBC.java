package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.DBType;

import java.util.Optional;

/**
 * 抽象类，要求下级实现行为：
 * 子类只提供url部分切片，该处有jdbcUrl默认行为合成，适用于大部分数据源类型的jdbc url
 *
 * @author baifangkual
 * @since 2024/7/12
 */
public abstract class DefaultJdbcUrlPaddingDBC extends JdbcUrlPaddingDBC {

    private static final String HEADER = "jdbc:";
    private static final String SL = "/";
    private static final String MP = ":";
    private static final String S1 = "://";

    public DefaultJdbcUrlPaddingDBC(Cfg connCfg) {
        super(connCfg);
    }

    @Override
    protected String buildingJdbcUrl(Cfg readonlyCfg) {
        final String prefix = jdbcUrlPrefix(readonlyCfg);
        /*
        部分数据库连接可不需要db参数，遂这里db可以为null，不设置该
        hive 没有db概念，jdbc API 标准中返回的也仅为schema
        sqlserver连接参数也可无db
        遂该处为通用如 db nullable
         */
        final String db = readonlyCfg.tryGet(DBCCfgOptions.db).orElse(null);
        final StringBuilder sb = new StringBuilder()
                .append(prefix)
                .append(S1)
                .append(readonlyCfg.get(DBCCfgOptions.host))
                .append(MP)
                .append(readonlyCfg.get(DBCCfgOptions.port));
        if (db != null) {
            sb.append(SL).append(db);
        }
        return sb.toString();
    }

    protected String jdbcUrlPrefix(Cfg readonlyCfg) {
        Optional<String> pOpt = tryGetJdbcUrlPrefix();
        String prefix;
        if (pOpt.isPresent()) {
            prefix = pOpt.get();
        } else {
            DBType t = type();
            // to do fix me 这里的逻辑为了解耦可用默认枚举.name映射或交由下层，不应该在此定义
            // option logic... default db type jdbc url prefix
            prefix = switch (t) {
                case sqlServer -> "sqlserver";
                case mysql -> "mysql";
                case oracle11g -> "oracle";
                case postgresql -> "postgresql";
                //noinspection UnnecessaryDefault
                default -> throw new IllegalStateException("not found database jdbcUrl prefix of type: " + t);
            };
        }
        Err.realIf(prefix.isBlank(), IllegalStateException::new,
                "datasource type: {} jdbc url prefix undefined", type());
        return prefix.startsWith(HEADER) ? prefix : HEADER + prefix;
    }

    /**
     * 下层数据库可覆盖该默认实现：提供其Jdbc协议的Url前缀，
     * 形如：postgresql sqlserver...
     *
     * @return jdbc协议的Url前缀
     */
    protected Optional<String> tryGetJdbcUrlPrefix() {
        return Optional.empty();
    }


}
