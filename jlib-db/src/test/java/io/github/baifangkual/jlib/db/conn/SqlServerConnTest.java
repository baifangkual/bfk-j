//package io.github.baifangkual.jlib.db.conn;
//
//import com.btrc.datacenter.common.core.config.Config;
//import com.btrc.datacenter.common.core.entities.Tup2;
//import com.btrc.datacenter.common.core.fmt.STF;
//import com.btrc.datacenter.common.datasource.entities.ConnectionConfig;
//import com.btrc.datacenter.common.datasource.entities.Table;
//import com.btrc.datacenter.common.datasource.enums.DSType;
//import com.btrc.datacenter.common.datasource.enums.URLType;
//import com.btrc.datacenter.common.datasource.trait.DataSource;
//import com.btrc.datacenter.common.datasource.trait.MetaProvider;
//import com.btrc.datacenter.common.datasource.util.DataSourceCreators;
//import com.btrc.datacenter.common.datasource.util.PropertiesMapConverter;
//import com.btrc.datacenter.common.datasource.util.ResultSetConverter;
//import org.junit.jupiter.api.Test;
//
//import java.sql.*;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
///**
// * @author baifangkual
// * create time 2024/7/19
// */
//public class SqlServerConnTest {
//
//    @Test
//    public void testSimpleGetConn() throws SQLException {
//
//        Properties prop = PropertiesMapConverter.convert(Map.of(
//                "user", "sa",
//                "password", "mssql_3a8WXG",
//                "trustServerCertificate", "true"
//
//        ));
//        Connection connection = DriverManager.getConnection("jdbc:sqlserver://47.109.100.44:1433;database=master", prop);
//
//        // this get tables
//        DatabaseMetaData metaData = connection.getMetaData();
//        ResultSet tables = metaData.getTables(null, null, null, null);
////            ResultSet testTable = metaData.getColumns("test_db", "test_schema", "test_table", null);
//
//        Tup2<List<String>, List<Object[]>> tup2 = ResultSetConverter.metaAndRows(tables);
//        List<String> left = tup2.left();
//        List<Object[]> right = tup2.right();
//        System.out.println(STF.f("colNames: {}", left));
//        for (Object[] row : right) {
//            System.out.println(STF.f("row: {}", Arrays.toString(row)));
//        }
//        tables.close();
//        connection.close();
//    }
//
//
//    @Test
//    public void testSqlServerConn() throws Exception {
//        Config config = new ConnectionConfig()
//                .setHost("47.109.100.44")
//                .setPort(1433)
//                .setDbName("master")
//                .setSchema("dbo")
//                .setUser("sa")
//                .setPasswd("mssql_3a8WXG")
//                .setDsType(DSType.SQL_SERVER)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//        DataSource dataSource = DataSourceCreators.create(config);
//
//        try (Connection conn = dataSource.getConnection()) {
//            // this get tables
//            DatabaseMetaData metaData = conn.getMetaData();
////            ResultSet tables = metaData.getTables("seatunnel", null, null, new String[]{"TABLE"});
//            ResultSet testTable = metaData.getColumns("seatunnel", "dbo", "tset_table", null);
//
//            Tup2<List<String>, List<Object[]>> tup2 = ResultSetConverter.metaAndRows(testTable);
//            List<String> left = tup2.left();
//            List<Object[]> right = tup2.right();
//            System.out.println(STF.f("colNames: {}", left));
//            for (Object[] row : right) {
//                System.out.println(STF.f("row: {}", Arrays.toString(row)));
//            }
//            testTable.close();
//        }
//
//    }
//
//    @Test
//    public void testSqlServerConnPro() throws Exception {
//
//        Config config = new ConnectionConfig()
//                .setHost("47.109.100.44")
//                .setPort(1433)
//                .setDbName("seatunnel")
//                .setSchema("dbo")
//                .setUser("sa")
//                .setPasswd("mssql_3a8WXG")
//                .setDsType(DSType.SQL_SERVER)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//        DataSource dataSource = DataSourceCreators.create(config);
//
//        List<Table.Meta> metas = dataSource.tablesMeta();
//        metas.forEach(System.out::println);
//        System.out.println("====");
//        try (Connection conn = dataSource.getConnection()) {
//            MetaProvider metaProvider = dataSource.getMetaProvider();
//            List<Table.ColumnMeta> tsetTable = metaProvider.columnsMeta(conn, dataSource.getConfig(), "tset_table");
//            tsetTable.forEach(System.out::println);
//        }
//    }
//
//    @Test
//    public void testSqlServerConnQueryTableDate() throws Exception {
//
//        Config config = new ConnectionConfig()
//                .setHost("47.109.100.44")
//                .setPort(1433)
//                .setDbName("seatunnel")
//                .setSchema("dbo")
//                .setUser("sa")
//                .setPasswd("mssql_3a8WXG")
//                .setDsType(DSType.SQL_SERVER)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//        DataSource dataSource = DataSourceCreators.create(config);
//        try (Connection connection = dataSource.getConnection()) {
//            MetaProvider metaProvider = dataSource.getMetaProvider();
//            Table.Rows rows = metaProvider.tableData(connection, dataSource.getConfig(), "tset_table", 1L, 2L);
//            rows.toDisplayList().forEach(System.out::println);
//        }
//
//    }
//}
