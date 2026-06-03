package br.com.yat.ecosystemcore.infrastructure.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {
    
    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("db-pool-thread-" + threadNumber.getAndIncrement());
        return thread;
    });

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(AppExecutors::shutdown, "shutdown-executors-hook"));
    }

    private AppExecutors() {}

    public static ExecutorService getDatabaseExecutor() {
        return DATABASE_EXECUTOR;
    }

    public static void shutdown() {
        if (!DATABASE_EXECUTOR.isShutdown()) {
            DATABASE_EXECUTOR.shutdown();
            try {
                if (!DATABASE_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
                    DATABASE_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                DATABASE_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
