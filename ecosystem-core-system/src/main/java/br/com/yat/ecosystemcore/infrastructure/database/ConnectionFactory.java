package br.com.yat.ecosystemcore.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Classe utilitária responsável por fornecer conexões ao banco de dados
 * e delegar operações de trannsação ao {@link ConnectionPoolManager}.
 * <p>
 * Integra:
 * <ul>
 * <li>{@link ConnectionPoolManager} para gerenciamento de conexões e transações.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class ConnectionFactory {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private ConnectionFactory() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Obtém uma conexão ativa do pool de conexões
	 * 
	 * @return instância de {@link Connection}
	 * @throws SQLException se o pool não estiver inicializado ou ocorrer erro ao obter conexão
	 */
	public static Connection getConnection() throws SQLException {
		return ConnectionPoolManager.getConnection();
	}

	/**
	 * Verifica status atual do banco de dados.
	 * 
	 * @return instância de {@link ConnectionPoolManager.DatabaseStatus} indicando disponibilidade e mensagem
	 */
	public static DatabaseStatus reloadAndCheck() {
		ConnectionPoolManager.reloadDataSource();
		return ConnectionPoolManager.checkStatus();
	}

	/**
	 * Inicia uma transação desabilitando o auto-commit.
	 * 
	 * @param conn conexão ativa
	 * @throws SQLException se ocorrer erro ao configurar a transação
	 * @throws NullPointerException se {@code conn} for nula
	 */
	public static void beginTransaction(Connection conn) throws SQLException {
		validateConnection(conn);
		ConnectionPoolManager.beginTransaction(conn);
	}

	/**
	 * Realiza commit da transação e restaura o auto-commit
	 * 
	 * @param conn conexão ativa
	 * @throws SQLException se ocorrer erro ao realizar commit
	 * @throws NullPointerException se {@code conn} for nula
	 */
	public static void commitTransaction(Connection conn) throws SQLException {
		validateConnection(conn);
		ConnectionPoolManager.commitTransaction(conn);
	}

	/**
	 * Realiza rollback da transação e restaura o auto-commit.
	 * <p>
	 * Caso a conexão seja nula, o rollback é ignorado e um aviso é registrado.
	 * </p>
	 * 
	 * @param conn conexão ativa (pode ser nula)
	 * @throws SQLException se ocorrer erro ao realizar rollback
	 */
	public static void rollbackTransaction(Connection conn) throws SQLException {
		if (conn == null) {
			logger.warn("Rollback ignorado: conexão nula");
			return;
		}
		ConnectionPoolManager.rollbackTransaction(conn);
	}

	/**
	 * Fecha o pool de conexões de forma graceful.
	 */
	public static void shutdown() {
		ConnectionPoolManager.shutdown();
	}

	/**
	 * Valida se a conexão não é nula.
	 * 
	 * @param conn conexão ativa
	 * @throws NullPointerException se {@code conn} for nula
	 */
	private static void validateConnection(Connection conn) {
		Objects.requireNonNull(conn, "Conexão não pode ser nula");
	}

}
