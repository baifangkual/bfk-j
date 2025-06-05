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
// * create time 2024/7/12
// */
//public class PostgresSqlConnTest {
//
//    private static final String showDb = """
//            SELECT datname
//            FROM pg_database
//            WHERE datistemplate = false;""";
//
//    @Test
//    public void testShowDatabase() {
//        /*
//          psql 没有提供 show database语句的实现，应该是因为show database不是sql92/99标准？
//          应使用 SELECT datname FROM pg_database
//          jdbc:postgresql://localhost:5432/postgres
//         */
//        String url = "jdbc:postgresql://localhost:32768/test_db";
//        Properties prop = PropertiesMapConverter.convert(Map.of(
//                "user", "postgres",
//                "password", "123456"
//        ));
//        printResult(url, prop, showDb);
//    }
//
//    @Test
//    public void testShowTablesOnDatabaseDomain() {
//        String url = "jdbc:postgresql://localhost:32768/test_db";
//        Properties prop = PropertiesMapConverter.convert(Map.of(
//                "user", "postgres",
//                "password", "123456",
//                "currentSchema", "test_schema"
//        ));
//
//        final String pubPr = """
//                SELECT table_name FROM
//                information_schema.tables
//                WHERE table_schema = 'public'
//                AND table_type = 'BASE TABLE'
//                """;
//        final String allPr = """
//                SELECT table_name FROM
//                information_schema.tables
//                WHERE table_type = 'BASE TABLE'
//                """;
//        final String allPr2 = """
//                SELECT table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE'
//                AND table_catalog = 'test_db' AND table_schema = 'test_schema'""";
//        printResult(url, prop, allPr2);
//    }
//
//    @Test
//    public void testPsqlDatasourceImpl() {
//
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(32768)
//                .setDbName("test_db")
//                .setSchema("test_schema")
//                .setUser("postgres")
//                .setPasswd("123456")
//                .setDsType(DSType.POSTGRESQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//
//        boolean b = dataSource.tryCheckConnection();
//        System.out.println(STF.f("check conn: {}", b));
//        Connection conn = dataSource.tryGetConnection().orElseThrow(IllegalStateException::new);
//
//        final String allPr2 = """
//                  SELECT table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE'
//                """;
//        printResult(conn, allPr2);
//
//
//    }
//
//    @Test
//    public void testPsqlSelectTableNamesAndComment() throws SQLException {
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(32768)
//                .setDbName("test_db")
//                .setSchema("test_schema")
//                .setUser("postgres")
//                .setPasswd("123456")
//                .setDsType(DSType.POSTGRESQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
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
////            ResultSet tables = metaData.getTables("test_db", "test_schema", null, null);
//            ResultSet testTable = metaData.getColumns("test_db", "test_schema", "test_table", null);
//
//
//            Tup2<List<String>, List<Object[]>> tup2 = ResultSetConverter.metaAndRows(testTable);
//            List<String> left = tup2.left();
//            List<Object[]> right = tup2.right();
//            System.out.println(STF.f("colNames: {}", left));
//            for (Object[] row : right) {
//                System.out.println(STF.f("row: {}", Arrays.toString(row)));
//            }
//            testTable.close();
//
//        }
//    }
//
//    @Test
//    public void testUsePsqlDbFunc(){
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(32768)
//                .setDbName("test_db")
//                .setSchema("test_schema")
//                .setUser("postgres")
//                .setPasswd("123456")
//                .setDsType(DSType.POSTGRESQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//        DataSource dataSource = DataSourceCreators.create(config);
//        List<Table.Meta> metas = dataSource.tablesMeta();
//        System.out.println("tableMeta:");
//        metas.forEach(System.out::println);
//        MetaProvider metaProvider = dataSource.getMetaProvider();
//        System.out.println("columnMeta:");
//        try (Connection conn = dataSource.tryGetConnection().orElseThrow(IllegalStateException::new)) {
//            List<Table.ColumnMeta> testTable = metaProvider.columnsMeta(conn, config, "test_table");
//            testTable.forEach(System.out::println);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Test
//    public void testFindAllColumnFromTable(){
//        Config config = new ConnectionConfig()
//                .setHost("localhost")
//                .setPort(32768)
//                .setDbName("test_db")
//                .setSchema("test_schema")
//                .setUser("postgres")
//                .setPasswd("123456")
//                .setDsType(DSType.POSTGRESQL)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//        DataSource dataSource = DataSourceCreators.create(config);
//        try (Connection conn = dataSource.tryGetConnection().orElseThrow(IllegalStateException::new)) {
//            Table.Rows rows = dataSource.getMetaProvider().tableData(conn, config, "test_table", 1L, 10L);
//            rows.forEach((r) -> System.out.println(Arrays.toString(r)));
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//
//
//
//    }
//
//
//
//
//    public static void printResult(String url, Properties prop, String execOneQuerySql) {
//        try (final Connection connection = DriverManager.getConnection(url, prop)) {
//            printResult(connection, execOneQuerySql);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static void printResult(final Connection conn, String execOneQuerySql) {
//        try (Statement st = conn.createStatement();
//             ResultSet rs = st.executeQuery(execOneQuerySql)) {
//            ResultSetMetaData metaData = rs.getMetaData();
//            int columnCount = metaData.getColumnCount();
//            System.out.println(STF.f("columnCount: {}", columnCount));
//
//            String[] colNames = new String[columnCount];
//            for (int i = 0; i < columnCount; i++) {
//                colNames[i] = metaData.getColumnName(i + 1);
//            }
//            System.out.println(STF.f("columnNames: {}", Arrays.toString(colNames)));
//            while (rs.next()) {
//                Object[] row = new Object[columnCount];
//                for (int i = 0; i < columnCount; i++) {
//                    row[i] = rs.getObject(i + 1);
//                }
//                System.out.println(Arrays.toString(row));
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//}
