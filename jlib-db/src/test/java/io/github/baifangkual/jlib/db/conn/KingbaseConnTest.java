//package io.github.baifangkual.jlib.db.conn;
//
//import com.btrc.datacenter.common.core.config.Config;
//import com.btrc.datacenter.common.datasource.entities.ConnectionConfig;
//import com.btrc.datacenter.common.datasource.entities.Table;
//import com.btrc.datacenter.common.datasource.enums.DSType;
//import com.btrc.datacenter.common.datasource.enums.URLType;
//import com.btrc.datacenter.common.datasource.trait.CloseableDataSource;
//import com.btrc.datacenter.common.datasource.trait.MetaProvider;
//import com.btrc.datacenter.common.datasource.utils.DataSourceCreators;
//import org.junit.jupiter.api.Test;
//
//import java.sql.Connection;
//import java.util.List;
//
///**
// * {@code @FileName} com.btrc.datacenter.common.datasource.conn.KingbaseConnTest
// * {@code @Author} lenovo/No PR
// * {@code @Create} 2024/8/30 16:44
// * {@code @Description} TODO
// */
//public class KingbaseConnTest {
//
//    @Test
//    public void testKingBaseConn(){
//        Config config = new ConnectionConfig()
//                .setHost("192.168.4.52")
//                .setPort(54321)
//                .setDbName("test")
//                .setUser("system")
//                .setPasswd("system")
//                .setSchema("public")
//                .setDsType(DSType.KINGBASE)
//                .setUrlType(URLType.JDBC_DEFAULT).toConfig();
////        DataSource dataSource = DataSourceCreators.create(config);
//        CloseableDataSource dataSource = DataSourceCreators.createConnPool(config);
//        ;
//        try {
//            Connection connection = dataSource.getConnection();
//            dataSource.checkConnection();
//            MetaProvider metaProvider = dataSource.getMetaProvider();
//            List<Table.Meta> metas = metaProvider.tablesMeta(connection, config);
//            metas.forEach(System.out::println);
//            List<Table.ColumnMeta> columnMetas = metaProvider.columnsMeta(dataSource, "seatunnel_table_1");
//            columnMetas.forEach(System.out::println);
//            System.out.println(connection);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//}
