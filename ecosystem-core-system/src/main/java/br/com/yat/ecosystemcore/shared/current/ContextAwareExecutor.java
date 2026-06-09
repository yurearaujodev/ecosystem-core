package br.com.yat.ecosystemcore.shared.current;

import java.util.function.Supplier;

import br.com.yat.ecosystemcore.shared.context.SessionContext;
import br.com.yat.ecosystemcore.shared.context.SessionScope;

public final class ContextAwareExecutor {

	private ContextAwareExecutor() {
	}

	public static Runnable wrap(Runnable task) {
		SessionContext ctx = SessionScope.getRaw();

		return () -> {
			if (ctx != null) {
				SessionScope.open(ctx);
			}

			try {
				task.run();
			} finally {
				SessionScope.close();
			}
		};
	}
	
	public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
		SessionContext ctx = SessionScope.getRaw();

		return () -> {
			if (ctx != null) {
				SessionScope.open(ctx);
			}
			try {
				return supplier.get();
			} finally {
				SessionScope.close();
			}
		};
	}
}