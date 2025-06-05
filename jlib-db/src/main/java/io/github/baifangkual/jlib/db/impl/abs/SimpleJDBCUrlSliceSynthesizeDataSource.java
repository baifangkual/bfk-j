package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.enums.DSType;

import java.util.Optional;

/**
 * @author baifangkual
 * create time 2024/7/12
 * 行为，要求子类只提供url部分切片，该处有jdbcUrl默认行为合成，适用于大部分数据源类型的jdbc url
 */
public abstract class SimpleJDBCUrlSliceSynthesizeDataSource extends DefaultJDBCUrlFormatDataSource {


    private static final String HEADER = "jdbc:";
    private static final String SL = "/";
    private static final String MP = ":";
    private static final String S1 = "://";

    public SimpleJDBCUrlSliceSynthesizeDataSource(Cfg connCfg) {
        super(connCfg);
    }

    /*
     * jdbc:mysql://localhost:3306/test
     * jdbc:postgresql://localhost:5432/postgres
     * jdbc:dm://localhost:5236
     * jdbc:sqlserver://localhost:1433
     * jdbc:oracle:thin:@localhost:1521/xepdb1 sid
     * jdbc:oracle:thin:@//localhost:1521/orcl serviceName
     * jdbc:sqlite:test.db || support?
     * jdbc:kingbase8://localhost:54321/db_test
     * jdbc:hive2://localhost:10000
     *
     * psql currentSchema
     * */


    @Override
    protected String buildingJdbcUrl(Cfg config) {
        final String prefix = urlPrefix(config);
        /*
        hive 没有db概念，jdbc API 标准中返回的也仅为schema
        sqlserver连接参数也可无db
        遂该处为通用如 db nullable
         */
        final String db = config.tryGet(ConnConfOptions.DB).orElse(null);
        final StringBuilder sb = new StringBuilder()
                .append(prefix)
                .append(S1)
                .append(config.get(ConnConfOptions.HOST))
                .append(MP)
                .append(config.get(ConnConfOptions.PORT));
        if (db != null) {
            sb.append(SL).append(db);
        }
        return sb.toString();
    }

    protected String urlPrefix(Cfg config) {
        Optional<String> pOpt = tryGetUrlPrefix();
        String prefix;
        if (pOpt.isPresent()) {
            prefix = pOpt.get();
        } else {
            DSType t = config.tryGet(ConnConfOptions.DS_TYPE)
                    .orElseThrow(() -> new IllegalArgumentException("dsType is null"));
            // to do fix me 这里的逻辑为了解耦可用默认枚举.name映射或交由下层，不应该在此定义
            prefix = switch (t) {
                case SQL_SERVER -> "sqlserver";
                case MYSQL -> "mysql";
                case ORACLE -> "oracle";
                case POSTGRESQL -> "postgresql";
                //noinspection UnnecessaryDefault
                default -> throw new IllegalStateException("Unexpected value: " + t);
            };
        }
        Err.realIf(prefix.isBlank(), IllegalStateException::new,
                "datasource type: {} jdbc url prefix undefined", config.get(ConnConfOptions.DS_TYPE));
        return prefix.startsWith(HEADER) ? prefix : HEADER + prefix;
    }

    protected Optional<String> tryGetUrlPrefix() {
        return Optional.empty();
    }


}
