package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author baifangkual
 * @since 2025/5/21
 */
@SuppressWarnings({"CommentedOutCode", "PointlessArithmeticExpression", "RedundantSuppression"})
public class RollTest {


    @Test
    public void test() {
        R<Long, RuntimeException> r10 = Roll.tryFixedLengthNumber(10);
        Assertions.assertDoesNotThrow(() -> r10.unwrap());
        R<Long, RuntimeException> r0 = Roll.tryFixedLengthNumber(0);
        Assertions.assertThrows(R.UnwrapException.class, r0::unwrap);
        R<Long, RuntimeException> r100 = Roll.tryFixedLengthNumber(100);
        Assertions.assertThrows(R.UnwrapException.class, r100::unwrap);
    }

    @Test
    public void test2() {
        for (int i = 0; i < 100; i++) {
            String num = Roll.fixedLengthNumber(i + 1);
            System.out.println(num);
            Assertions.assertEquals(num.length(), i + 1);
        }
    }

//    @Test
//    public void test3() {
//
//        // ======== test time=========
//        final int testTimeMils = 1 * 1000;
//        final int collectorResultSize = 10000;
//
//        SystemClockT.INSTANCE.initialize(); // init
//        int cupNum = Runtime.getRuntime().availableProcessors();
//        AtomicInteger cup = new AtomicInteger(0);
//        ZoneId zoneId = ZoneId.systemDefault();
//        System.out.println("cupNum = " + cupNum);
//        System.out.println("testTimeSeconds = " + testTimeMils / 1000);
//        System.out.println("start!");
//        List<CompletableFuture<AsyncR>> allFutures = new ArrayList<>();
//
//        for (int i = 0; i < cupNum; i++) {
//            CompletableFuture<AsyncR> asyncR = CompletableFuture.supplyAsync(() -> {
//                final int threadNum = cup.getAndIncrement();
//                final SystemClockT clock = SystemClockT.INSTANCE;
//                // collector result 不在线程内使用阻塞的system.out
//                final List<Long> genIds = new ArrayList<>(collectorResultSize);
//                // start ---
//                long startMils = clock.currentTimeMillis();
//                while (clock.currentTimeMillis() - startMils < testTimeMils) {
//                    long l = Roll.nextId();
//                    genIds.add(l);
//                }
//                long endMils = clock.currentTimeMillis();
//                return new AsyncR(startMils, endMils, genIds, threadNum);
//            });
//            allFutures.add(asyncR);
//        }
//
//        // 主线程等待子都结束
//        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
//        // 处理
//        System.out.println("allFutures end! process result!");
//        final Comparator<IdR> fnSortIdR = Comparator
//                .comparingLong(IdR::timeOffset)
//                // eq no need comp...
////                .thenComparingLong(IdR::centerId)
////                .thenComparingLong(IdR::machineId)
//                .thenComparingLong(IdR::sequence);
//
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss:SSS");
//        List<ShowBox> allResults = new LinkedList<>();
//
//        for (CompletableFuture<AsyncR> f : allFutures) {
//            AsyncR threadR = f.join();
//            long startMils = threadR.startMils;
//            long endMils = threadR.endMils;
//            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMils), zoneId);
//            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMils), zoneId);
//            String strStart = start.format(dtf);
//            String strEnd = end.format(dtf);
//
//            // 都装好收集
//            threadR.genIds.stream()
//                    .map(IdR::ofSId)
//                    .map(idr -> new ShowBox(idr, strStart, strEnd, threadR.threadNum))
//                    .forEach(allResults::add);
//        }
//
//
//        List<ShowBox> sorts = new LinkedList<>();
//        allResults.stream()
//                .sorted((sb1, sb2) -> fnSortIdR.compare(sb1.idr, sb2.idr))
//                .forEach(sorts::add);
//        System.out.println("result count = " + allResults.size());
//
//        allResults.clear();
//        System.gc(); // help gc
//
//        AtomicInteger counter = new AtomicInteger(0);
//        // 5秒约130多万次 太多阻塞System.out 不用这种
////        for (ShowBox box : sorts) {
////            System.out.println(STF
////                    .f("r[{}] t:{}, s:{}, e:{}, id: {}",
////                            counter.incrementAndGet(), box.threadNum, box.startTime, box.endTime, box.idr));
////        }
//
//
//        Map<Integer, AtomicInteger> threadLoopTimes = new HashMap<>();
//        IntStream.range(0, cupNum).forEach(i -> threadLoopTimes.put(i, new AtomicInteger(0)));
//        AtomicInteger allThreadLoopTimes = new AtomicInteger(0);
//        ShowBox befBox = null;
//        for (ShowBox curBox : sorts) {
//            IdR cur = curBox.idr;
//            if (befBox != null) {
//                if (befBox.idr.timeOffset != cur.timeOffset && cur.sequence == 0) {
//                    threadLoopTimes.get(curBox.threadNum).incrementAndGet();
////                    System.out.println(STF
////                            .f("bef: {}", befBox));
////                    System.out.println(STF
////                            .f("cur: {}", curBox));
//                }
//            }
//
//            if (cur.sequence == 0) {
//                allThreadLoopTimes.getAndIncrement();
//            }
//
//            befBox = curBox;
//        }
//        System.out.println("allThreadLoopTimes count = " + allThreadLoopTimes.get());
//        threadLoopTimes.forEach((key, value) -> System.out.println(STF
//                .f("t: {}, LoopTimes count: {}", key, value.get())));
//
//        // 看看是否冲突
//        int countSize = sorts.size();
//        Set<IdR> collect = sorts.stream().map(ShowBox::idr)
//                .collect(Collectors.toSet());
//        System.out.println(STF.
//                f("bef去重: {}, after:{}", countSize, collect.size()));
//        System.out.println(STF.f("no重复: {}", countSize == collect.size()));
//        Assertions.assertEquals(countSize, collect.size());

        // 共获取到 / loop次
        // 10s:
        // 修改重置序号前
        // 2033663/1986 1313791/1283 1314815/1284 3876412/3784 3235932/3160 1978368/1932
        // 3910624/3818 2656172/2593
        // 修改重置序号后
        // 2603010/2543 2334723/2281 3278850/3203 3256324/3181 2752517/2689 3286020/3210
        // 20s:
        // 修改重置序号前
        // 7618936/7437 4427720/4322 5751358/5613
        // 修改重置序号后
        // 6288389/6142 3817474/3729 3873796/3784 8190984/8000
        // 1s:
        // 修改重置序号前
        // 129023/126 130047/127 131278/128 723834/706 197632/193 197631/193 | 2005180/1957 1996668/1948 2011180/1962
        // 修改重置序号后
        // 196419/193 129923/128 129923/128 128900/127 128900/127 509456/499 | 441940/433 1972349/1929 1975420/1932
//    }


    record ShowBox(IdR idr, String startTime, String endTime, int threadNum) {
    }

    record IdR(long timeOffset, long centerId, long machineId, long sequence) {
        static IdR ofSId(long sId) {
            long sequence = (~(-1 << 10)) & sId;
            long machineId = (~(-1 << 7)) & (sId >> 10);
            long centerId = (~(-1 << 5)) & (sId >> 17);
            long timeOffset = sId >> 22;
            return new IdR(timeOffset, centerId, machineId, sequence);
        }
    }

    record AsyncR(long startMils, long endMils, List<Long> genIds, int threadNum) {
    }

}
