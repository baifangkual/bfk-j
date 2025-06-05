//package io.github.baifangkual.jlib.db.conn;
//
//import com.btrc.datacenter.common.core.config.Config;
//import com.btrc.datacenter.common.datasource.constants.ConnConfOptions;
//import com.btrc.datacenter.common.datasource.entities.Table;
//import com.btrc.datacenter.common.datasource.enums.DSType;
//import com.btrc.datacenter.common.datasource.enums.URLType;
//import com.btrc.datacenter.common.datasource.trait.DataSource;
//import com.btrc.datacenter.common.datasource.trait.MetaProvider;
//import com.btrc.datacenter.common.datasource.util.DataSourceCreators;
//import com.btrc.datacenter.common.datasource.util.DefaultMetaSupport;
//import com.btrc.datacenter.common.datasource.util.PropertiesMapConverter;
//import org.junit.jupiter.api.Test;
//
//import java.sql.*;
//import java.util.*;
//
///**
// * @author baifangkual
// * create time 2024/10/23
// */
//public class OracleConnTest {
//
//    /*
//     * jdbc:oracle:thin:@localhost:1521/xepdb1 sid
//     * jdbc:oracle:thin:@localhost:1521/orcl serviceName
//     */
//
//    private static final String host = "192.168.4.52";
//    private static int port = 1521;
//    private static URLType urlType = URLType.JDBC_ORACLE_SERVICE_NAME;
//    private static String servername = "helowin";
//    private static String username = "admin";
//    private static String password = "admin";
//
//    private static String puser = "user";
//    private static String passwd = "password";
//    private static Map<String, String> up = Map.of(puser, username, passwd, password);
//    private static Properties convert = PropertiesMapConverter.convert(up);
//
//    private static final Config oracleConf = Config.of()
//            .set(ConnConfOptions.USER, username)
//            .set(ConnConfOptions.PASSWD, password)
//            .set(ConnConfOptions.HOST, host)
//            .set(ConnConfOptions.DB, servername)
//            .set(ConnConfOptions.SCHEMA, username)
//            .set(ConnConfOptions.DS_TYPE, DSType.ORACLE);
//
//
//    private static void printRows(ResultSet rs) throws SQLException {
//        try (rs) {
//            List<String> columnNames = new ArrayList<>();
//            ResultSetMetaData read = rs.getMetaData();
//            int columnCount = read.getColumnCount();
//            for (int i = 0; i < columnCount; i++) {
//                String columnName = read.getColumnName(i + 1);
//                columnNames.add(columnName);
//            }
//            List<Object[]> rows = new ArrayList<>();
//            while (rs.next()) {
//                Object[] row = new Object[columnCount];
//                for (int i = 0; i < columnCount; i++) {
//                    Object object = rs.getObject(i + 1);
//                    row[i] = object;
//                }
//                rows.add(row);
//            }
//            System.out.println(columnNames);
//            rows.forEach(row -> System.out.println(Arrays.toString(row)));
//        }
//    }
//
//    @Test
//    public void testOraclePageQuery() throws Exception {
//        DataSource dataSource = DataSourceCreators.create(oracleConf);
//        MetaProvider metaProvider = dataSource.getMetaProvider();
//        Table.Rows rows = metaProvider.tableData(dataSource, "test_no_schema", 1L, 3L);
//        rows.getRows().forEach(row -> System.out.println(Arrays.toString(row)));
//    }
//
//    @Test
//    public void testOracleDBTableColumnMetaGet() throws Exception {
//        DataSource ds = DataSourceCreators.create(oracleConf);
//        List<Table.ColumnMeta> columnMetas = ds.getMetaProvider().columnsMeta(ds, "test_no_schema");
//        columnMetas.forEach(System.out::println);
//    }
//
//
//    @Test
//    public void testCoracleDatasourceCheck() throws Exception {
//        DataSource dataSource = DataSourceCreators.create(oracleConf);
//        dataSource.checkConnection();
//    }
//
//
//    @Test
//    public void testOracleTableColumnMetaDATA() throws Exception {
//        try (
//                Connection conn = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin", convert);
//        ) {
//            DatabaseMetaData metaData = conn.getMetaData();
//            ResultSet columns = metaData.getColumns(null, "ADMIN", "test_no_schema", null);
//            printRows(columns);
//        }
//    }
//
//
//    @Test
//    public void testOracleMetaDATA() throws Exception {
//        try (
//                Connection conn = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin", convert);
//        ) {
//            DatabaseMetaData metaData = conn.getMetaData();
//            ResultSet rs = metaData.getTables(null, "ADMIN", null, new String[]{"TABLE"});
//            printRows(rs);
//        }
//    }
//
//
//    @Test
//    public void testOracleMetaGet() throws Exception {
//        try (
//                Connection conn = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin", convert);
//        ) {
//            List<Table.Meta> metas = DefaultMetaSupport.tablesMeta(conn, null, "ADMIN");
//            metas.forEach(System.out::println);
//        }
//    }
//
//    @Test
//    public void testOracleConn() throws SQLException {
//        try (
//                Connection connection = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin", convert);
//                Statement statement = connection.createStatement();
//                ResultSet resultSet = statement.executeQuery("SELECT SYSDATE FROM DUAL");
//        ) {
//            while (resultSet.next()) {
//                System.out.println(resultSet.getString(1));
//            }
//        }
//    }
//
//    @Test
//    public void testOracleConnFromSelect1() throws SQLException {
//        try (
//                Connection connection = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin", convert);
//                Statement statement = connection.createStatement();
//                ResultSet resultSet = statement.executeQuery("SELECT 1 FROM DUAL");
//        ) {
//            while (resultSet.next()) {
//                System.out.println(resultSet.getString(1));
//            }
//        }
//    }
//
//    @Test
//    public void testOracleConnFromUpcastUserName() throws SQLException {
//        try (
//                Connection connection = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin",
//                                PropertiesMapConverter.convert(
//                                        Map.of(
//                                                puser, "ADMIN",
//                                                passwd, "admin"
//                                        )
//                                ));
//                Statement statement = connection.createStatement();
//                ResultSet resultSet = statement.executeQuery("SELECT 1 FROM DUAL");
//        ) {
//            while (resultSet.next()) {
//                System.out.println(resultSet.getString(1));
//            }
//        }
//    }
//
//    @Test
//    public void testOracleConnDropTableIfExists() throws SQLException {
//
//        try (
//                Connection connection = DriverManager
//                        .getConnection("jdbc:oracle:thin:@192.168.4.52:1521/helowin", convert);
//                Statement statement = connection.createStatement();) {
//            boolean r = statement.execute(
//                    """
//                            BEGIN
//                                EXECUTE IMMEDIATE 'DROP TABLE "ADMIN"."tEst_No_schema7" ';
//                            EXCEPTION
//                                WHEN OTHERS THEN
//                                    IF SQLCODE != -942 THEN
//                                        RAISE;
//                                    END IF;
//                            END;
//                            """
//            );
//            System.out.println(r);
//        }
//    }
//
//    @Test
//    public void testOracleDSMetaTables() throws Exception {
//        DataSource ds = DataSourceCreators.create(oracleConf);
//        MetaProvider mp = ds.getMetaProvider();
//        List<Table.Meta> metas = mp.tablesMeta(ds);
//        metas.forEach(System.out::println);
//    }
//}
