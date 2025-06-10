## 概述

提供对jdbc查询及其部分数据库元数据查询的简单封装

## 使用

```java
import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public void foo() {
    // 使用jdbcUrl连接某数据库
    DB db = DB.simple("jdbc://...", "user", "***", Map.of(),
                    // 连接有效性校验函数
                    DB.FnAssertValidConnect.SELECT_1)
            // 断言已可连接到数据库，否则抛出异常
            .assertConnect();
    // 查询，行转换函数
    List<Foo> fooList = db.execQuery("select * from foo",
            LinkedList::new,
            (rn, rs) -> new Foo(rs.getInt("id"), rs.getString("name")));
    // 查询，rs完全控制
    Map<Integer, String> nameLike = db.execQuery("select * from foo",
            (rs) -> {
                Map<Integer, String> m = new HashMap<>();
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name.contains("nameLike")) {
                        m.put(rs.getInt("id"), name);
                    }
                }
                return m;
            });
    // 池化... 池为有状态对象，使用完需关闭其
    try (PooledDB pooled = db.pooled(10)) {
        for (int i = 0; i < 10; i++) {
            CompletableFuture.runAsync(() -> {
                try (java.sql.Connection conn = pooled.getConn()) {
                    // 可多个线程同时访问获取conn并进行作业...
                    // do some...
                } catch (java.sql.SQLException sqlE) {
                    // process sql e...
                }
            });
        }
    }
    // 或可构建DBC，其能提供部分类型数据库的元数据
    Cfg psqlCfg = Cfg.newCfg()
            .set(DBCCfgOptions.type, DBType.postgresql)
            .set(DBCCfgOptions.user, "user")
            .set(DBCCfgOptions.passwd, "***");
    // other setting ...

    DBC dbc = DBCFactory.build(psqlCfg)
            // 断言已可连接到数据库，否则抛出异常
            .assertConnect();
    // 可获取到部分元数据
    List<Table.Meta> tables = dbc.tablesMeta();
    List<Table.ColumnMeta> columnsMeta = dbc.columnsMeta("table_foo");
    // 池化... 池为有状态对象，使用完需关闭其
    // 多个线程可同时访问获取conn并进行作业
    try (PooledDBC pooled = dbc.pooled()) {
        // do some...
    }
}
```

## 主要实体

* io.github.baifangkual.jlib.db.DB 提供对任意Jdbc数据源的查询封装
* io.github.baifangkual.jlib.db.PooledDB 同DB，不过其为池化的有状态对象
* io.github.baifangkual.jlib.db.DBC 扩展DB接口，额外提供部分数据库的元数据，实现是专有于数据库类型的
* io.github.baifangkual.jlib.db.PooledDBC 同DBC，不过其为池化的有状态对象，实现是专有于数据库类型的
* io.github.baifangkual.jlib.db.DBCCfgOptions DBC构建过程的选项及说明
* io.github.baifangkual.jlib.db.trait.MetaProvider 无状态对象，提供某种类型数据库的元数据，实现是专有于数据库类型的
* io.github.baifangkual.jlib.db.ResultSetExtractor 函数-操作ResultSet
* io.github.baifangkual.jlib.db.RSRowMapping 函数-ResultSet的行转换