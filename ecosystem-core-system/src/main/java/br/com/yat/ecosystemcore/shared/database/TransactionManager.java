package br.com.yat.ecosystemcore.shared.database;

import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactionManager {

	private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
	private static final ThreadLocal<TransactionContext> transactionContext = new ThreadLocal<>();

	private TransactionManager() {
		throw new AssertionError("Classe utilitária não deve ser instanciada");
	}

	public static Connection getConnection() throws SQLException {
		TransactionContext ctx = transactionContext.get();
		if (ctx != null && isUsable(ctx.connection)) {
			return ctx.connection;
		}
		return ConnectionFactory.getConnection();
	}

	public static <T> T executeInTransaction(TransactionalSupplier<T> operation) throws SQLException {
		TransactionContext existing = transactionContext.get();

		if (existing != null) {
			if (isUsable(existing.connection)) {
				return executeNested(existing, operation);
			}
			transactionContext.remove();
			logger.warn("Contexto transacional obsoleto removido da thread atual.");
		}

		try (Connection conn = ConnectionFactory.getConnection()) {
			TransactionContext newCtx = new TransactionContext(conn);
			transactionContext.set(newCtx);

			try {
				ConnectionFactory.beginTransaction(conn);

				T result = operation.execute(conn);

				if (newCtx.rollbackOnly) {
					logger.warn("Transação raiz marcada como 'rollbackOnly'. Executando rollback forçado.");
					safeRollback(conn);
					throw new SQLException("Transação abortada: um bloco interno falhou e marcou rollbackOnly.");
				}

				ConnectionFactory.commitTransaction(conn);
				logger.debug("Transação commitada com sucesso.");
				return result;

			} catch (Exception e) {
				logger.error("Erro detectado na transação raiz. Executando rollback.", e);
				safeRollback(conn);
				throw toSqlException(e);
			} finally {
				transactionContext.remove();
			}
		}
	}

	public static void executeVoidInTransaction(TransactionalRunnable operation) throws SQLException {
		executeInTransaction(conn -> {
			operation.execute(conn);
			return null;
		});
	}

	private static <T> T executeNested(TransactionContext ctx, TransactionalSupplier<T> operation) throws SQLException {
		try {
			return operation.execute(ctx.connection);
		} catch (Exception e) {
			ctx.rollbackOnly = true;
			throw toSqlException(e);
		}
	}

	private static void safeRollback(Connection conn) {
		try {
			ConnectionFactory.rollbackTransaction(conn);
		} catch (SQLException ex) {
			logger.error("Falha ao executar rollback da transação.", ex);
		}
	}

	private static boolean isUsable(Connection conn) {
		if (conn == null)
			return false;
		try {
			return !conn.isClosed();
		} catch (SQLException e) {
			logger.warn("Não foi possível verificar o estado da conexão transacional.", e);
			return false;
		}
	}

	private static SQLException toSqlException(Exception cause) {
		if (cause instanceof SQLException sqlEx) {
			return sqlEx;
		}
		return new SQLException("Operação desfeita devido a uma falha no processamento.", cause);
	}

	private static final class TransactionContext {
		final Connection connection;
		boolean rollbackOnly = false;

		TransactionContext(Connection connection) {
			this.connection = connection;
		}
	}

	@FunctionalInterface
	public interface TransactionalSupplier<T> {
		T execute(Connection conn) throws Exception;
	}

	@FunctionalInterface
	public interface TransactionalRunnable {
		void execute(Connection conn) throws Exception;
	}
}