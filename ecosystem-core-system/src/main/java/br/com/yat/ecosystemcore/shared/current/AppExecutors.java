package br.com.yat.ecosystemcore.shared.current;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
//import java.util.function.Supplier;

public final class AppExecutors {

	private static final Logger logger = LoggerFactory.getLogger(AppExecutors.class);

	private static final ExecutorService DATABASE_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(AppExecutors::shutdown, "shutdown-executors-hook"));
	}

	private AppExecutors() {
	}

	@FunctionalInterface
	public interface CheckedSupplier<T> {
		T get() throws Exception;
	}

	@FunctionalInterface
	public interface CheckedRunnable {
		void run() throws Exception;
	}

	public static ExecutorService getDatabaseExecutor() {
		return DATABASE_EXECUTOR;
	}

	public static void execute(Runnable task) {
		DATABASE_EXECUTOR.execute(ContextAwareExecutor.wrap(task));
	}

	public static <T> CompletableFuture<T> supplyAsync(CheckedSupplier<T> supplier) {
		return CompletableFuture.supplyAsync(ContextAwareExecutor.wrapSupplier(() -> {
			try {
				return supplier.get();
			} catch (Throwable e) {
				logger.error("Erro assíncrono não tratado", e);
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}), DATABASE_EXECUTOR);
	}

	public static CompletableFuture<Void> runAsync(CheckedRunnable runnable) {
		return CompletableFuture.runAsync(ContextAwareExecutor.wrap(() -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				logger.error("Erro assíncrono não tratado", e);
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}), DATABASE_EXECUTOR);
	}

	public static void shutdown() {
		if (DATABASE_EXECUTOR.isShutdown())
			return;

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