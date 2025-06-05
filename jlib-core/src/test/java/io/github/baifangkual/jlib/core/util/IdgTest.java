package io.github.baifangkual.jlib.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.github.baifangkual.jlib.core.util.Idg.EPOCH_END;

/**
 * @author baifangkual
 * @since 2025/5/22
 */
@SuppressWarnings({"ConstantValue", "PointlessArithmeticExpression", "CommentedOutCode"})
public class IdgTest {
    @Test
    public void test1() {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sTime = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(Idg.EPOCH_BEGIN), zoneId);
        Assertions.assertTrue(sTime.isBefore(now));
        //long epochOffset = ~(-1L << (63  - MACHINE_ID_BITS - SEQUENCE_BITS));
        LocalDateTime testLenEpochOffset = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(EPOCH_END), zoneId);
        Assertions.assertTrue(testLenEpochOffset.isAfter(now));
        Assertions.assertTrue(sTime.isBefore(now));
        Assertions.assertTrue(sTime.isBefore(testLenEpochOffset));
        //System.out.println(testLenEpochOffset);
    }

    @Test
    public void test3() {
        ZoneId zoneId = ZoneId.systemDefault();
        for (int i = 0; i < 100; i++) {
            long id = Idg.longId();
            Idg.Id idObj = Idg.Id.ofLongId(id);
            //System.out.println(STF.f("systemCurrentTimeMillis: {}", System.currentTimeMillis()));
            //System.out.println(STF.f("longId: {}, IdObj: {}，toLongId: {}", id, idObj, idObj.toLongId()));
            LocalDateTime genTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(idObj.genTimeMillis()), zoneId);
            Assertions.assertTrue(genTime.isBefore(LocalDateTime.now()));
        }
    }

    record ShowBox(Idg.Id idr, String startTime, String endTime, int threadNum) {
    }

    record AsyncR(long startMils, long endMils, List<Long> genIds, int threadNum) {
    }


    @Test
    public void test4() {

        // 不跑下面的压测，因为太久了 MAN！What can I say？
        // 测试中不要使用 AtomicLong fakeSysClock 自增，否则生成的ID太多，电脑要炸了
        if (true) {
            return;
        }

        // ======== test time=========
        final int testTimeMils = 1 * 1000;
        final int collectorResultSize = 100000;

        //SystemClockT.INSTANCE.initialize(); // init
        int cupNum = Runtime.getRuntime().availableProcessors();
        ZoneId zoneId = ZoneId.systemDefault();
        System.out.println("cupNum = " + cupNum);
        System.out.println("testTimeSeconds = " + testTimeMils / 1000);
        System.out.println("start!");
        List<CompletableFuture<AsyncR>> allFutures = new ArrayList<>();
        int testCupNum = 10;
        System.out.println("testCupNum = " + testCupNum);

        Set<Integer> threadNums = new CopyOnWriteArraySet<>();

        // use sysClock
        //SystemClock.INSTANCE.initialize();
        //final Idg idg = new Idg(1L, SystemClock.INSTANCE::currentTimeMillis);
        // use fake sysClock
        //=====================================
        // DON'T USE THIS！！！ 太多了，电脑要炸了
        //AtomicLong fakeSysClock = new AtomicLong(System.currentTimeMillis());
        //final Idg idg = new Idg(1L, fakeSysClock::getAndIncrement);

        // no use sysClock
        final Idg idg = new Idg(1L, System::currentTimeMillis);

        for (int i = 0; i < testCupNum; i++) {
            CompletableFuture<AsyncR> asyncR = CompletableFuture.supplyAsync(() -> {
                Thread currentTrd = Thread.currentThread();
                long id = currentTrd.getId();
                final int threadNum = (int) id;
                threadNums.add(threadNum);
//                final SystemClockT clock = SystemClockT.INSTANCE;
                // collector result 不在线程内使用阻塞的system.out
                final List<Long> genIds = new ArrayList<>(collectorResultSize);
                // start ---
                long startMils = System.currentTimeMillis();
                while (System.currentTimeMillis() - startMils < testTimeMils) {
                    long l = idg.nextLongId();
                    genIds.add(l);
                }
                long endMils = System.currentTimeMillis();
                return new AsyncR(startMils, endMils, genIds, threadNum);
            });
            allFutures.add(asyncR);
        }

        // 主线程等待子都结束
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
        // 处理
        System.out.println("allFutures end! process result!");
        final Comparator<Idg.Id> fnSortIdR = Comparator
                .comparingLong(Idg.Id::genTimeMillis)
                .thenComparingLong(Idg.Id::sequence);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss:SSS");
        List<ShowBox> allResults = new LinkedList<>();

        Map<Integer, Integer> threadIdRefThreadGenNum = new HashMap<>();

        for (CompletableFuture<AsyncR> f : allFutures) {
            AsyncR threadR = f.join();
            long startMils = threadR.startMils;
            long endMils = threadR.endMils;
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMils), zoneId);
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMils), zoneId);
            String strStart = start.format(dtf);
            String strEnd = end.format(dtf);

            threadIdRefThreadGenNum.put(threadR.threadNum, threadR.genIds.size());

            // 都装好收集
            threadR.genIds.stream()
                    .map(Idg.Id::ofLongId)
                    .map(idr -> new ShowBox(idr, strStart, strEnd, threadR.threadNum))
                    .forEach(allResults::add);
        }

        List<ShowBox> sorts = new LinkedList<>();
        allResults.stream()
                .sorted((sb1, sb2) -> fnSortIdR.compare(sb1.idr, sb2.idr))
                .forEach(sorts::add);

        Map<Integer, AtomicInteger> threadLoopTimes = new HashMap<>();
        threadNums.forEach(i -> threadLoopTimes.put(i, new AtomicInteger(0)));
        AtomicInteger allThreadLoopTimes = new AtomicInteger(0);
        ShowBox befBox = null;
        for (ShowBox curBox : sorts) {
            Idg.Id cur = curBox.idr;
            if (befBox != null) {
                if (befBox.idr.genTimeMillis() != cur.genTimeMillis() && cur.sequence() == 0) {
                    threadLoopTimes.get(curBox.threadNum).incrementAndGet();

//                    System.out.println(STF
//                            .f("bef: {}", befBox));
//                    System.out.println(STF
//                            .f("cur: {}", curBox));
                }
            }

            if (cur.sequence() == 0) {
                allThreadLoopTimes.getAndIncrement();
            }

            befBox = curBox;
        }
        System.out.println("cupNum = " + cupNum);
        System.out.println("testTimeSeconds = " + testTimeMils / 1000);
        System.out.println("testCupNum = " + testCupNum);
        System.out.println("result count = " + allResults.size());
        allResults.clear();
        System.gc(); // help gc
        System.out.println("allThreadLoopTimes count = " + allThreadLoopTimes.get());
        threadLoopTimes.forEach((key, value) -> System.out.println(Stf
                .f("t: {}, LoopTimes count: {}", key, value.get())));

        System.out.println("========================");
        threadIdRefThreadGenNum.forEach((k, v) -> System.out.println(Stf.f("t: {}, genCount: {}", k, v)));
        System.out.println("========================");

        // 看看是否冲突
        int countSize = sorts.size();
        Set<Idg.Id> collect = sorts.stream().map(ShowBox::idr)
                .collect(Collectors.toSet());
        System.out.println(Stf.
                f("bef去重: {}, after:{}", countSize, collect.size()));
        System.out.println(Stf.f("no重复: {}", countSize == collect.size()));
        Assertions.assertEquals(countSize, collect.size());
        // cupNum = 20
        //testTimeSeconds = 2
        //testCupNum = 10
        //result count = 3895048
        //allThreadLoopTimes count = 1903
        //t: 32, LoopTimes count: 147
        //t: 33, LoopTimes count: 219
        //t: 34, LoopTimes count: 191
        //t: 35, LoopTimes count: 204
        //t: 36, LoopTimes count: 192
        //t: 37, LoopTimes count: 254
        //t: 38, LoopTimes count: 195
        //t: 39, LoopTimes count: 167
        //t: 30, LoopTimes count: 133
        //t: 31, LoopTimes count: 200
        //========================
        //t: 32, genCount: 295448
        //t: 33, genCount: 446287
        //t: 34, genCount: 410224
        //t: 35, genCount: 410960
        //t: 36, genCount: 393595
        //t: 37, genCount: 523133
        //t: 38, genCount: 388803
        //t: 39, genCount: 338418
        //t: 30, genCount: 270407
        //t: 31, genCount: 417773
        //========================
    }

    @Test
    public void testGenB62Id(){

        int loop = 1000;
        List<String> genIds = new LinkedList<>();
        for (int i = 0; i < loop; i++) {
            String s = Idg.b62Id();
            genIds.add(s);
            //System.out.println(s);
        }

        List<String> ls = genIds.stream().distinct()
                .toList();
        Assertions.assertEquals(genIds.size(), ls.size());

    }
}
