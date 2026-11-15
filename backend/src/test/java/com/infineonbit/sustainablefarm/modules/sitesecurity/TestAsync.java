package com.infineonbit.sustainablefarm.modules.sitesecurity;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Lets a test start only once the module's background work from earlier tests has finished. */
public final class TestAsync {

    private TestAsync() {
    }

    public static void drain(ThreadPoolTaskExecutor... executors) {
        long deadline = System.currentTimeMillis() + 10_000;
        for (ThreadPoolTaskExecutor executor : executors) {
            while (System.currentTimeMillis() < deadline
                    && (executor.getActiveCount() > 0 || !executor.getThreadPoolExecutor().getQueue().isEmpty())) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
