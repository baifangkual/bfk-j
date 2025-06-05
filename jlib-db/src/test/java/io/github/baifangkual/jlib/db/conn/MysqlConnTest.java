//package io.github.baifangkual.jlib.db.conn;
//
//import com.btrc.datacenter.common.core.config.Config;
//import com.btrc.datacenter.common.core.entities.Tup2;
//import com.btrc.datacenter.common.core.fmt.STF;
//import com.btrc.datacenter.common.datasource.entities.ConnectionConfig;
//import com.btrc.datacenter.common.datasource.entities.Table;
//import com.btrc.datacenter.common.datasource.enums.DSType;
//import com.btrc.datacenter.common.datasource.enums.URLType;
//import com.btrc.datacenter.common.datasource.impl.ds.MysqlDataSource;
//import com.btrc.datacenter.common.datasource.trait.DataSource;
//import com.btrc.datacenter.common.datasource.trait.MetaProvider;
//import com.btrc.datacenter.common.datasource.utils.DataSourceCreators;
//import com.btrc.datacenter.common.datasource.utils.ResultSetConverter;
//import org.junit.jupiter.api.Test;
//
//import java.sql.*;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * @author baifangkual
// * create time 2024/7/12
// */
//public class MysqlConnTest {
//
//    @Test
//    public void testConnTry(){
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(3306)
//                .setDbName("test_db")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .setUrlType(URLType.JDBC_DEFAULT)
//                .toConfig();
//        DataSource dataSource = DataSourceCreators.create(config);
//        dataSource.throwableCheckConnection();
//
//
//    }
//
//
//    @Test
//    public void testConnMysqlMeta() throws SQLException {
//
//        Config config = new ConnectionConfig()
//                .setHost("192.168.3.133")
//                .setPort(3306)
//                .setDbName("mysql")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .setUrlType(URLType.JDBC_DEFAULT)
//                .toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//
//        boolean b = dataSource.tryCheckConnection();
//        System.out.println(STF.f("check conn: {}", b));
//        Connection conn = dataSource.tryGetConnection().orElseThrow(IllegalStateException::new);
//
//        try (Connection connection = conn) {
//
//            // this get tables
//            DatabaseMetaData metaData = connection.getMetaData();
//            ResultSet tables = metaData.getTables("test_db", null, null, null);
////            ResultSet testTable = metaData.getColumns("test_db", null, "test_table", null);
//
//            Tup2<List<String>, List<Object[]>> tup2 = ResultSetConverter.metaAndRows(tables);
//            List<String> left = tup2.left();
//            List<Object[]> right = tup2.right();
//            System.out.println(STF.f("colNames: {}", left));
//            for (Object[] row : right) {
//                System.out.println(STF.f("row: {}", Arrays.toString(row)));
//            }
//            tables.close();
//
//
//        }
//    }
//
//    @Test
//    public void testUseMysqlDatasourceDSMeta() {
//
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(3306)
//                .setDbName("test_db")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        try (Connection connection = dataSource.tryGetConnection().orElseThrow(IllegalStateException::new)) {
//
//            MetaProvider metaProvider = new MysqlDataSource.MetaProviderImpl();
//            List<Table.Meta> metas = metaProvider.tablesMeta(connection, config);
//            System.out.println(metas);
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//
//
//    }
//
//    @Test
//    public void testUseMysqlDefaultDSMetaShow() {
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(3306)
//                .setDbName("test_db")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        List<Table.Meta> metas = dataSource.tablesMeta();
//        System.out.println(metas);
//    }
//
//
//    @Test
//    public void testUseShowColumnMeta() {
//
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(3306)
//                .setDbName("test_db")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//
//        try (Connection conn = dataSource.tryGetConnection()
//                .orElseThrow(IllegalArgumentException::new)) {
//            DatabaseMetaData md = conn.getMetaData();
//            ResultSet columns = md.getColumns("test_db", null, "test_table", null);
//            Tup2<List<String>, List<Object[]>> tup2 = ResultSetConverter.metaAndRows(columns);
//            List<String> left = tup2.left();
//            List<Object[]> right = tup2.right();
//            System.out.println(STF.f("colNames: {}", left));
//            for (Object[] row : right) {
//                System.out.println(STF.f("row: {}", Arrays.toString(row)));
//            }
//            columns.close();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Test
//    public void testUseShowColMetaDefault() {
//
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(3306)
//                .setDbName("test_db")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        List<Table.ColumnMeta> testTable = dataSource.getMetaProvider().columnsMeta(dataSource, "test_table");
//        testTable.forEach(System.out::println);
//    }
//
//    @Test
//    public void testUseShowRowsDefault() {
//
//        ConnectionConfig config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(3306)
//                .setDbName("test_db")
//                .setUser("root")
//                .setPasswd("123456")
//                .setDsType(DSType.MYSQL)
//                .setUrlType(URLType.JDBC_DEFAULT);
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        Table.Rows rows = dataSource.getMetaProvider().tableData(dataSource, "test_table", 1L, 10L);
//        List<List<String>> rowsList = rows.toList((o) -> {
//            if (o instanceof Integer) {
//                return "int:" + o;
//            } else if (o instanceof String) {
//                return "String:" + o;
//            } else if (o instanceof Long) {
//                return "long:" + o;
//            } else if (o instanceof Double) {
//                return "double:" + o;
//            } else if (o instanceof Float) {
//                return "float:" + o;
//            } else if (o instanceof Date) {
//                return "sql_date:" + o;
//            } else if (o instanceof Time) {
//                return "sql_time:" + o;
//            } else if (o instanceof Timestamp) {
//                return "sql_timestamp:" + o;
//            } else if (o instanceof java.util.Date) {
//                return "java.util.Date:" + o;
//            } else if (o instanceof LocalDateTime) {
//                return "local_date_time:" + o;
//            } else {
//                return "unKnow:" + o;
//            }
//        });
//        for (List<String> r : rowsList) {
//            System.out.println(r);
//        }
//
//    }
//}
