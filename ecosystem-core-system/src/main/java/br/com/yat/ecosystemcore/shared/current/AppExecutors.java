package br.com.yat.ecosystemcore.shared.current;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

public final class AppExecutors {

    private static final Logger logger = LoggerFactory.getLogger(AppExecutors.class);

    private static final ExecutorService DATABASE_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread(AppExecutors::shutdown, "shutdown-executors-hook")
        );
    }

    private AppExecutors() {}

    public static ExecutorService getDatabaseExecutor() {
        return DATABASE_EXECUTOR;
    }

    public static void execute(Runnable task) {
        DATABASE_EXECUTOR.execute(
            ContextAwareExecutor.wrap(task)
        );
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                logger.error("Erro assíncrono não tratado", e);
                throw e;
            }
        }, DATABASE_EXECUTOR);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(
            ContextAwareExecutor.wrap(() -> {
                try {
                    runnable.run();
                 } catch (Throwable e) {
                    logger.error("Erro assíncrono não tratado", e);
                    throw new RuntimeException(e);
                }
            }),
            DATABASE_EXECUTOR
        );
    }

    public static void shutdown() {
        if (DATABASE_EXECUTOR.isShutdown()) return;

        DATABASE_EXECUTOR.shutdown();

        try {
            if (!DATABASE_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                DATABASE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            DATABASE_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warn("Interrupção durante encerramento do executor", e);
        }
    }
}
//package br.com.yat.ecosystemcore.infrastructure.concurrent;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public final class AppExecutors {
//    
//    private static final Logger logger = LoggerFactory.getLogger(AppExecutors.class);
//    private static final AtomicInteger threadNumber = new AtomicInteger(1);
//    private static final int DATABASE_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
//
//    private static final ExecutorService DATABASE_EXECUTOR =
//        Executors.newFixedThreadPool(
//                DATABASE_POOL_SIZE,
//                runnable -> {
//                    Thread thread = new Thread(runnable);
//                    thread.setDaemon(true);
//                    thread.setName("db-pool-thread-" + threadNumber.getAndIncrement());
//                    
//                    // ⚡ O SEGREDO AQUI: Captura qualquer erro não tratado dentro da thread daemon
//                    // e força a exibição imediata no console através do Log do sistema.
//                    thread.setUncaughtExceptionHandler((t, e) -> 
//                        logger.error("EXCEÇÃO NÃO TRATADA na Thread Assíncrona [{}]: ", t.getName(), e)
//                    );
//                    
//                    return thread;
//                });
//
//    static {
//        Runtime.getRuntime().addShutdownHook(new Thread(AppExecutors::shutdown, "shutdown-executors-hook"));
//    }
//
//    private AppExecutors() {}
//
//    public static ExecutorService getDatabaseExecutor() {
//        return DATABASE_EXECUTOR;
//    }
//
//    public static void shutdown() {
//        if (!DATABASE_EXECUTOR.isShutdown()) {
//            DATABASE_EXECUTOR.shutdown();
//            try {
//                if (!DATABASE_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
//                    DATABASE_EXECUTOR.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                DATABASE_EXECUTOR.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//}