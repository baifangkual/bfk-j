package io.github.baifangkual.jlib.db.conn;
import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.*;
import io.github.baifangkual.jlib.db.util.PropMapc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author baifangkual
 * create time 2024/7/12
 */
@SuppressWarnings("GrazieInspection")
public class PostgresSqlConnTest {

    private static final String showDb = """
            SELECT datname
            FROM pg_database
            WHERE datistemplate = false;""";

    @Test
    public void testShowDatabase() {
        /*
          psql 没有提供 show database语句的实现，应该是因为show database不是sql92/99标准？
          应使用 SELECT datname FROM pg_database
          jdbc:postgresql://localhost:5432/postgres
         */
//        String url = "jdbc:postgresql://localhost:32768/test_db";
//        Properties prop = PropMapc.convert(Map.of(
//                "user", "postgres",
//                "password", "123456"
//        ));
//        printResult(url, prop, showDb);
    }

    @Test
    public void testShowTablesOnDatabaseDomain() {
//        String url = "jdbc:postgresql://localhost:32768/test_db";
//        Properties prop = PropMapc.convert(Map.of(
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
    }

    private static Cfg pi5Cfg(){
        return Cfg.newCfg()
                .set(DBCCfgOptions.user, "postgres")
                .set(DBCCfgOptions.passwd, "long321")
                .set(DBCCfgOptions.host, "bfk-pi5.local")
                .set(DBCCfgOptions.db, "postgres")
                .set(DBCCfgOptions.schema, "public")
                .set(DBCCfgOptions.type, DBType.POSTGRESQL);
    }

    @Test
    public void testPsqlDatasourceImpl() {
//        DBC dbc = DBCFactory.build(pi5Cfg());
//        boolean b = dbc.testConn();
//        System.out.println(Stf.f("check conn: {}", b));
//        Connection conn = dbc.tryGetConn().unwrap();
//
//        final String allPr2 = """
//                  SELECT table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE'
//                """;
//        printResult(conn, allPr2);
    }

    @Test
    public void testUsePsqlDbFunc(){
//        DBC dbc = DBCFactory.build(pi5Cfg());
//        List<Table.Meta> metas = dbc.tablesMeta();
//        metas.forEach(System.out::println);
//        List<Table.ColumnMeta> testTableMeta = dbc.columnsMeta("test_table");
//        testTableMeta.forEach(System.out::println);
    }

    @Test
    public void testFindAllColumnFromTable(){
//        DBC dbc = DBCFactory.build(pi5Cfg());
//        Table.Rows testTable = dbc.tableData("test_table");
//        testTable.forEach(r -> System.out.println(Arrays.toString(r)));
    }

    public static void printResult(String url, Properties prop, String execOneQuerySql) {
        try (final Connection connection = DriverManager.getConnection(url, prop)) {
            printResult(connection, execOneQuerySql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printResult(final Connection conn, String execOneQuerySql) {
        //noinspection SqlSourceToSinkFlow
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(execOneQuerySql)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            System.out.println(Stf.f("columnCount: {}", columnCount));

            String[] colNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                colNames[i] = metaData.getColumnName(i + 1);
            }
            System.out.println(Stf.f("columnNames: {}", Arrays.toString(colNames)));
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                System.out.println(Arrays.toString(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
