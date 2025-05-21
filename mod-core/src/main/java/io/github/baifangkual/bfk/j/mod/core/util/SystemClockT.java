package io.github.baifangkual.bfk.j.mod.core.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * System Clock
 * <p>
 * 利用ScheduledExecutorService实现高并发场景下System.currentTimeMillis()的性能问题的优化.
 *
 * @author lry
 */
public enum SystemClockT {
    INSTANCE(1);
    private final long period;
    private final AtomicLong nowTime;
    private boolean started = false;
    private ScheduledExecutorService executorService;

    SystemClockT(long period) {
        this.period = period;
        this.nowTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * The initialize scheduled executor service
     */
    public void initialize() {
        if (started) {
            return;
        }
        this.executorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "system-clock");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> nowTime.set(System.currentTimeMillis()),
                this.period, this.period, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
        started = true;
    }

    /**
     * The get current time milliseconds
     *
     * @return long time
     */
    public long currentTimeMillis() {
        return started ? nowTime.get() : System.currentTimeMillis();
    }

    /**
     * The destroy of executor service
     */
    public void destroy() {
        System.out.println("SystemClock destroy.");
        if (executorService != null) {
            executorService.shutdown();
        }
    }

}