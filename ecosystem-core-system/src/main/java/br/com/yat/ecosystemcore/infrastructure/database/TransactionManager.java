package br.com.yat.ecosystemcore.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gerencia transações JDBC por thread, vinculando uma conexão do pool ao escopo
 * de {@link #executeInTransaction(TransactionalOperation)}.
 * <p>
 * Fora de uma transação ativa, {@link #getConnection()} delega ao pool e o
 * chamador deve fechar a conexão (try-with-resources). Dentro de uma transação,
 * retorna a conexão transacional — não deve ser fechada pelo chamador.
 * </p>
 */
public final class TransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    private static final ThreadLocal<TransactionContext> transactionContext = new ThreadLocal<>();

    private TransactionManager() {
        throw new AssertionError("Classe utilitária não deve ser instanciada");
    }

    /**
     * Fornece a conexão da transação ativa na thread atual ou abre uma nova do pool.
     */
    public static Connection getConnection() throws SQLException {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null && isUsable(ctx.connection)) {
            return ctx.connection;
        }
        return ConnectionFactory.getConnection();
    }

    /**
     * Executa um bloco garantindo atomicidade (commit ou rollback).
     * Chamadas aninhadas na mesma thread reutilizam a conexão sem novo begin/commit.
     */
    public static void executeInTransaction(TransactionalOperation operation) throws SQLException {
        TransactionContext existing = transactionContext.get();
        if (existing != null) {
            if (isUsable(existing.connection)) {
                executeNested(existing, operation);
                return;
            }
            transactionContext.remove();
            logger.warn("Contexto transacional obsoleto removido da thread atual.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            transactionContext.set(new TransactionContext(conn));
            try {
                ConnectionFactory.beginTransaction(conn);
                operation.execute(conn);
                ConnectionFactory.commitTransaction(conn);
                logger.debug("Transação commitada com sucesso.");
            } catch (Exception e) {
                logger.error("Erro detectado durante a transação. Executando rollback.", e);
                safeRollback(conn);
                throw toSqlException(e);
            } finally {
                transactionContext.remove();
            }
        }
    }

    private static void executeNested(TransactionContext ctx, TransactionalOperation operation)
            throws SQLException {
        try {
            operation.execute(ctx.connection);
        } catch (Exception e) {
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
        if (conn == null) {
            return false;
        }
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

        TransactionContext(Connection connection) {
            this.connection = connection;
        }
    }

    @FunctionalInterface
    public interface TransactionalOperation {
        void execute(Connection conn) throws Exception;
    }
}
