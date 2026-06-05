package br.com.yat.ecosystemcore.infrastructure.database;

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

    /**
     * ⚡ AGORA RETORNA VALOR! Executa um bloco garantindo atomicidade (commit ou rollback).
     */
    public static <T> T executeInTransaction(TransactionalSupplier<T> operation) throws SQLException {
        TransactionContext existing = transactionContext.get();
        
        // Se já existe uma transação rodando na thread atual (Aninhamento)
        if (existing != null) {
            if (isUsable(existing.connection)) {
                return executeNested(existing, operation);
            }
            transactionContext.remove();
            logger.warn("Contexto transacional obsoleto removido da thread atual.");
        }

        // Nova transação (Raiz)
        try (Connection conn = ConnectionFactory.getConnection()) {
            TransactionContext newCtx = new TransactionContext(conn);
            transactionContext.set(newCtx);
            
            try {
                ConnectionFactory.beginTransaction(conn);
                
                T result = operation.execute(conn);
                
                // 🛡️ SUPORTE A ROLLBACK ONLY: Mesmo que a exceção externa seja capturada,
                // se qualquer bloco interno marcou como rollbackOnly, nós dropamos a transação inteira.
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

    /**
     * ⚡ NOVO MÉTODO: Executa um bloco transacional que NÃO possui retorno (void).
     * Ele reaproveita toda a lógica do método principal escondendo o 'return null' dentro da infraestrutura.
     */
    public static void executeInTransaction(TransactionalRunnable operation) throws SQLException {
        executeInTransaction(conn -> {
            operation.execute(conn);
            return null;
        });
    }

    private static <T> T executeNested(TransactionContext ctx, TransactionalSupplier<T> operation) throws SQLException {
        try {
            return operation.execute(ctx.connection);
        } catch (Exception e) {
            // 🚨 Se a operação aninhada falhar, marca o contexto inteiro para a morte
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
        if (conn == null) return false;
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

    // 📋 Contexto interno expandido com a flag de segurança
    private static final class TransactionContext {
        final Connection connection;
        boolean rollbackOnly = false; // Flag protetora estilo Spring

        TransactionContext(Connection connection) {
            this.connection = connection;
        }
    }

    /**
     * Nova interface funcional que substitui o antigo Runnable/Operation, 
     * agora permitindo retornos de DTOs ou Entidades diretamente.
     */
    @FunctionalInterface
    public interface TransactionalSupplier<T> {
        T execute(Connection conn) throws Exception;
    }

    /**
     * ⚡ NOVA INTERFACE: Representa uma operação transacional sem retorno.
     */
    @FunctionalInterface
    public interface TransactionalRunnable {
        void execute(Connection conn) throws Exception;
    }
}