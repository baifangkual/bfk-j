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
//import com.btrc.datacenter.common.datasource.util.DataSourceCreators;
//import com.btrc.datacenter.common.datasource.util.ResultSetConverter;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.ResultSet;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * @author baifangkual
// * create time 2024/7/23
// */
//public class HiveConnTest {
//
//
//    @Test
//    public void testHiveConn(){
//
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.51")
//                .setPort(10000)
//                .setDbName("seatunnel")
//                .setDsType(DSType.HIVE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        boolean b = dataSource.tryCheckConnection();
//        Assertions.assertTrue(b);
//    }
//
//    @Test
//    public void testMetaGet() throws Exception {
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.51")
//                .setPort(10000)
//                .setSchema("seatunnel")
//                .setDsType(DSType.HIVE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//
//        try (Connection conn = dataSource.getConnection()) {
//            // this get tables
//            DatabaseMetaData metaData = conn.getMetaData();
////            ResultSet tables = metaData.getTables(null, null, null, null);
//            ResultSet testTable = metaData.getColumns(null, "seatunnel", "example_table", null);
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
//    public void testMeteTableMeta(){
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.51")
//                .setPort(10000)
//                .setSchema("seatunnel")
//                .setDsType(DSType.HIVE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        List<Table.Meta> metas = dataSource.tablesMeta();
//        metas.forEach(System.out::println);
//    }
//
//    @Test
//    public void testMeteTableMetaCol(){
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.51")
//                .setPort(10000)
//                .setSchema("seatunnel")
//                .setDsType(DSType.HIVE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        List<Table.ColumnMeta> emp = dataSource.getMetaProvider().columnsMeta(dataSource, "example_table");
//        emp.forEach(System.out::println);
//    }
//
//    @Test
//    public void testTableDateGet(){
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.51")
//                .setPort(10000)
//                .setSchema("information_schema")
//                .setDsType(DSType.HIVE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        Table.Rows rows = dataSource.getMetaProvider().tableData(dataSource, "tables", 1L, 10L);
//        rows.toDisplayList().forEach(System.out::println);
//
//    }
//
//    @Test
//    public void testDeleteHiveTable(){
//
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.51")
//                .setPort(10000)
//                .setDbName("seatunnel")
//                .setDsType(DSType.HIVE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
//
//        DataSource dataSource = DataSourceCreators.create(config);
//        dataSource.getMetaProvider().delTable(dataSource,"seatunnel_table_6");
//    }
//
//}
