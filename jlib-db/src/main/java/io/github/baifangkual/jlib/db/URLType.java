package io.github.baifangkual.jlib.db;

import lombok.RequiredArgsConstructor;

/**
 * @author baifangkual
 * create time 2024/7/11
 * jdbc url 格式描述, 部分数据库有多种jdbcUrl格式支持要求，
 * 与DSType关系为一对多
 */
@Deprecated // todo 20250607 删除，该应用面小，可仅为oracle特化，
            //  控制 url构型有多种方式，没必要专门弄个 enum
            //  若后续需控制，则在 buildJdbcUrl内可从cfg中配置项控制
@RequiredArgsConstructor
public enum URLType {


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
     * */

    // default jdbc conn url
    JDBC_DEFAULT,
    // todo 因对 sqLite的支持已废弃，遂当为oracle时，应当为专有参数
    //  应在 oracle内部对该两种做区分
    // oracle service name type
    JDBC_ORACLE_SERVICE_NAME,
    // oracle sid type
    JDBC_ORACLE_SID,
    // sqlite local
    @Deprecated /* todo 已不打算支持，后续删除该 */ JDBC_SQLITE_LOCAL_FILE,
    ;


}
