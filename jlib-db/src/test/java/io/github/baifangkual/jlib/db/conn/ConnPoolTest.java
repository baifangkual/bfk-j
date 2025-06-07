//package io.github.baifangkual.jlib.db.conn;
//
//import io.github.baifangkual.jlib.core.conf.Cfg;
//import io.github.baifangkual.jlib.db.DBCFactory;
//import io.github.baifangkual.jlib.db.PooledDBC;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import java.sql.Connection;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
///**
// * @author baifangkual
// * create time 2024/7/29
// */
//@Slf4j
//public class ConnPoolTest {
//
//
//    @Test
//    public void maxCreatedTest() throws Exception {
//
//        Cfg cfg = Cfg.newCfg();
//        PooledDBC connPool = DBCFactory.build(cfg).pooled();
//        CompletableFuture.runAsync(() -> {
//            try {
//                Connection c1 = connPool.getConn();
//                TimeUnit.SECONDS.sleep(5);
//                c1.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        CompletableFuture.runAsync(() -> {
//            try {
//                Connection c2 = connPool.getConn();
//                TimeUnit.SECONDS.sleep(5);
//                c2.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        CompletableFuture.runAsync(() -> {
//            try {
//                Connection c3 = connPool.getConn();
//                TimeUnit.SECONDS.sleep(5);
//                c3.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        log.info("bef thread get conn");
//        TimeUnit.SECONDS.sleep(2);
//        // await
//        log.info("main start get conn");
//        Connection c4 = connPool.getConn();
//        log.info("main end get conn");
//        TimeUnit.SECONDS.sleep(1);
//        log.info("main start close conn");
//        c4.close();
//        log.info("main end close conn");
//        TimeUnit.SECONDS.sleep(1);
//        log.info("main start close pool");
//        connPool.close();
//        log.info("main end close pool");
//
//    }
//
//
//    @Test
//    public void testGetLockOne() throws Exception {
//        Cfg cfg = Cfg.newCfg();
//        PooledDBC connPool = DBCFactory.build(cfg).pooled();
//        CompletableFuture.runAsync(() -> {
//            try {
//                log.info("Thread:{}, start get conn", Thread.currentThread().getName());
//                Connection c1 = connPool.getConn();
//                log.info("Thread:{}, end get conn", Thread.currentThread().getName());
//                log.info("Thread:{}, has conn, conn:{}", Thread.currentThread().getName(), c1);
//                log.info("Thread:{}, start sleep", Thread.currentThread().getName());
//                TimeUnit.SECONDS.sleep(5);
//                log.info("Thread:{}, end sleep", Thread.currentThread().getName());
//                log.info("Thread:{}, start close conn", Thread.currentThread().getName());
//                c1.close();
//                log.info("Thread:{}, end close conn", Thread.currentThread().getName());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        log.info("subThread start, main sleep 2sec");
//        TimeUnit.SECONDS.sleep(2);
//        log.info("Thread:{}, start get conn", Thread.currentThread().getName());
//        Connection c2 = connPool.getConn();
//        log.info("Thread:{}, end get conn, conn:{}", Thread.currentThread().getName(), c2);
//        log.info("Thread:{}, start sleep", Thread.currentThread().getName());
//        TimeUnit.SECONDS.sleep(2);
//        log.info("Thread:{}, end sleep", Thread.currentThread().getName());
//        log.info("Thread:{}, start close conn", Thread.currentThread().getName());
//        c2.close();
//        log.info("Thread:{}, end close conn", Thread.currentThread().getName());
//        TimeUnit.SECONDS.sleep(2);
//        log.info("Thread:{}, start close pool", Thread.currentThread().getName());
//        connPool.close();
//        log.info("Thread:{}, end close pool", Thread.currentThread().getName());
//    }
//
//    @Test
//    public void testGetLockOneBefClose() throws Exception {
//
//        Cfg cfg = Cfg.newCfg();
//        PooledDBC connPool = DBCFactory.build(cfg).pooled();
//
//
//        CompletableFuture.runAsync(() -> {
//            try {
//                log.info("Thread:{}, start get conn", Thread.currentThread().getName());
//                Connection c1 = connPool.getConn();
//                log.info("Thread:{}, end get conn", Thread.currentThread().getName());
//                log.info("Thread:{}, has conn, conn:{}", Thread.currentThread().getName(), c1);
//                log.info("Thread:{}, start sleep", Thread.currentThread().getName());
//                TimeUnit.SECONDS.sleep(5);
//                log.info("Thread:{}, end sleep", Thread.currentThread().getName());
//                log.info("Thread:{}, start close conn", Thread.currentThread().getName());
//                c1.close();
//                log.info("Thread:{}, end close conn", Thread.currentThread().getName());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//
//        log.info("subThread start, main sleep 2sec");
//        TimeUnit.SECONDS.sleep(2);
//        log.info("Thread:{}, end sleep", Thread.currentThread().getName());
//        log.info("Thread:{}, start close pool", Thread.currentThread().getName());
//        connPool.close();
//        log.info("Thread:{}, end close pool", Thread.currentThread().getName());
//    }
//
//
//    @Test
//    public void testReCloseConn() throws Exception {
//        Cfg cfg = Cfg.newCfg();
//        Assertions.assertThrows(IllegalStateException.class, () -> {
//            try (PooledDBC connPool = DBCFactory.build(cfg).pooled();) {
//                Connection c = connPool.getConn();
//                c.close();
//                c.close();
//            }
//        });
//
//    }
//
//    @Test
//    public void testReCloseConnPool() {
//        Cfg cfg = Cfg.newCfg();
//        Assertions.assertThrows(IllegalStateException.class, () -> {
//            PooledDBC connPool = DBCFactory.build(cfg).pooled();
//            Connection c = connPool.getConn();
//            c.close();
//            connPool.close();
//            connPool.close();
//        });
//    }
//
//
//}
