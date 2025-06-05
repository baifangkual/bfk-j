package io.github.baifangkual.jlib.core.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * System Clock
 * <p>
 * 利用ScheduledExecutorService实现高并发场景下System.currentTimeMillis()的性能问题的优化.
 *
 * @author lry
 * @deprecated 20250524：该更新系统时间的精度还没有直接系统调用高（流汗黄豆），周期约15ms...
 */
@Deprecated
public enum SystemClock {
    INSTANCE(1);
    private final long period;
    private final AtomicLong nowTime;
    private boolean started = false;
    private ScheduledExecutorService executorService;
    private final boolean logThreadId = true;

    SystemClock(long period) {
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
            if (logThreadId) {
                System.out.println("SystemClock initialize " + thread.getName() + ", " + thread.getId());
            }
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.out.println("thread: " + t.getName() + " caught " + e);
            });
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
        executorService.scheduleAtFixedRate(() -> {
                    long current = System.currentTimeMillis();
                    nowTime.set(current);
                    if (logThreadId) {
                        System.out.println("Schedule update timeMillis to: " + current);
                    }
                },
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