package br.com.yat.ecosystemcore.shared.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import br.com.yat.ecosystemcore.shared.util.DatabaseConfigLoader;

/**
 * Classe utilitária responsável por gerenciar o pool de conexões do banco de
 * dados utilizando <b>HikariCP</b>.
 * <p>
 * Integra:
 * <ul>
 * <li><b>HikariCP</b> ({@link HikariDataSource}) para gerenciamento de
 * conexões.</li>
 * <li>{@link DatabaseConfigLoader} e {@link HikariConfigBuilder} para carregar
 * e aplicar configurações.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Não deve ser instanciada.
 * </p>
 */
final class ConnectionPoolManager {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);

	private static volatile HikariDataSource dataSource;
	private static final Object RELOAD_LOCK = new Object();
	private static final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Construtor privara para evitar instanciação.
	 */
	private ConnectionPoolManager() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	static {
		try {
			initialize();
		} catch (Exception e) {
			logger.error("Falha crítica na inicialização do pool de conexões!", e);
			throw e;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(ConnectionPoolManager::shutdown));
	}

	/**
	 * Inicializa o pool de conexões, carregando configurações e testando conexões
	 * se configurado.
	 * <p>
	 * Caso ocorra falha crítica e o modo fail-fast esteja habilitado, lança
	 * {@link IllegalArgumentException}.
	 * </p>
	 */
	private static void initialize() {
		if (initialized.compareAndSet(false, true)) {
			try {
				reloadDataSource();

				if (shouldTestPoolOnStartup()) {
					testAllConnection();
				}

				logger.info("Pool HikariCp inicializado com sucesso.");
			} catch (Exception e) {
				logger.error("Não foi possivel inicializar o pool de conexões.", e);
				if (isFailFast()) {
					throw new IllegalStateException("Falha crítica ao iniciar banco", e);
				}
			}
		}
	}

	/**
	 * Verifica se o pool deve ser testado na inicialização.
	 * 
	 * @return {@code true} se o teste estiver habilitado, {@code false} caso
	 *         contrário
	 */
	private static boolean shouldTestPoolOnStartup() {
		try {
			var props = DatabaseConfigLoader.loadConfig();
			return Boolean.parseBoolean(props.getProperty("db.testPoolOnStartup", "false"));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Verifica se aplicação deve falhar rapidamente em caso de erro crítico.
	 * 
	 * @return {@code true} se o modo fail-fast estiver habilitado, {@code false}
	 *         caso contrário
	 */
	private static boolean isFailFast() {
		try {
			var props = DatabaseConfigLoader.loadConfig();
			return Boolean.parseBoolean(props.getProperty("db.failFast", "true"));
		} catch (Exception e) {
			return true;
		}
	}

	/**
	 * Testa múltiplas conexões do pool para validar sua integridade.
	 * <p>
	 * Obtém até 3 conexões e verifica se são válidas.
	 * </p>
	 */
	private static void testAllConnection() {
		HikariDataSource ds = dataSource;
		if (ds == null) {
			logger.warn("Pool de conexões não disponível para teste.");
			return;
		}

		int total = Math.min(3, ds.getMaximumPoolSize());
		int success = 0;

		for (int i = 0; i < total; i++) {
			try (Connection conn = ds.getConnection()) {
				if (conn.isValid(2)) {
					success++;
				}
			} catch (Exception e) {
				logger.error("Falha ao testar conexão {}/{}: {}", i + 1, total, e.getMessage());
			}
		}
		logger.info("Teste de pool finalizado: {}/{} conexões válidas.", success, total);
	}
	
	static DatabaseStatus testTemporaryConnection(DatabaseConfig config, DatabaseCredentials credentials) {
		Objects.requireNonNull(config, "Configuração do banco não pode ser nula");
		Objects.requireNonNull(credentials, "Credenciais do banco não podem ser nulas");
		
		// Criamos um array de um elemento para conseguir extrair o resultado de dentro do escopo lambda
		final DatabaseStatus[] resultado = new DatabaseStatus[1];
		
		try {
			// A mágica da segurança acontece aqui:
			credentials.executarComSenha(senhaAtiva -> {
				
				// 1. Cria propriedades em memória simulando o arquivo db.properties
				Properties tempProps = new Properties();
				tempProps.setProperty("db.url", config.gerarJdbcUrl());
				tempProps.setProperty("db.user", config.usuario());
				tempProps.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
				
				// A String da senha é materializada no último milissegundo possível e fica restrita a este bloco
				if (senhaAtiva != null && senhaAtiva.length > 0) {
					tempProps.setProperty("db.password", new String(senhaAtiva));
				}

				// Propriedades de timeout para o teste falhar rápido
				tempProps.setProperty("db.connectionTimeout", "4000");
				tempProps.setProperty("db.validationTimeout", "2000");
				tempProps.setProperty("db.poolSize", "1"); 

				// 2. Instancia o DataSource temporário e valida a conexão
				try (HikariDataSource tempDs = new HikariDataSource(HikariConfigBuilder.buildConfig(tempProps))) {
					try (Connection conn = tempDs.getConnection()) {
						if (conn.isValid(2)) {
							resultado[0] = DatabaseStatus.ok(); // Sucesso
						} else {
							resultado[0] = DatabaseStatus.error("Conexão inválida no teste.");
						}
					}
				} catch (Exception e) {
					String msg = e.getMessage().toLowerCase();
					for (var entry : ERRO_MESSAGES.entrySet()) {
						if (entry.getKey().test(msg)) {
							resultado[0] = DatabaseStatus.error(entry.getValue());
							return;
						}
					}
					resultado[0] = DatabaseStatus.error("Falha na conexão de teste: " + e.getMessage());
				}
			});
			
			return resultado[0] != null ? resultado[0] : DatabaseStatus.error("Teste não foi executado corretamente.");
			
		} catch (Exception e) {
			return DatabaseStatus.error("Erro crítico ao processar credenciais: " + e.getMessage());
		}
	}

	/**
	 * Obtém uma conexão do pool.
	 * 
	 * @return instância de {@link Connection}
	 * @throws SQLException se o pool não estiver inicializado ou estiver fechado
	 */
	static Connection getConnection() throws SQLException {
		HikariDataSource ds = dataSource;
		if (ds == null || ds.isClosed()) {
			throw new SQLException("Pool de conexões não inicializado ou fechado. Verifique a configuração e logs.");
		}
		return ds.getConnection();
	}

	/**
	 * Verifica o status atual do banco de dados.
	 * 
	 * @return instância de {@link DatabaseStatus} indicando disponibilidade e
	 *         mensagem
	 */
	static DatabaseStatus checkStatus() {
		try (Connection conn = getConnection()) {
			return conn.isValid(2) ? DatabaseStatus.ok() : DatabaseStatus.error("Conexão inválida.");
		} catch (SQLException e) {
			String msg = e.getMessage().toLowerCase();
			for (var entry : ERRO_MESSAGES.entrySet()) {
				if (entry.getKey().test(msg)) {
					return DatabaseStatus.error(entry.getValue());
				}
			}
			return DatabaseStatus.error("Erro no banco: " + e.getMessage());
		}
	}

	/**
	 * Inicia uma transação desabilitando o auto-commit.
	 * 
	 * @param conn conexão ativa
	 * @throws SQLException se ocorrer erro ao configurar a transação
	 */
	static void beginTransaction(Connection conn) throws SQLException {
		Objects.requireNonNull(conn, "Conexão nula!");
		conn.setAutoCommit(false);
	}

	/**
	 * Realiza commit da transação e restaura o auto-commit.
	 * 
	 * @param conn conexão ativa
	 * @throws SQLException se ocorrer erro ao realizar commit
	 */
	static void commitTransaction(Connection conn) throws SQLException {
		Objects.requireNonNull(conn, "Conexão nula!");
		try {
			if (!conn.isClosed() && !conn.getAutoCommit()) {
				conn.commit();
			}
		} finally {
			if (!conn.isClosed()) {
				conn.setAutoCommit(true);
			}
		}
	}

	/**
	 * Realiza rollback da transação e restaura o auto-commit
	 * 
	 * @param conn conexão ativa (pode ser nula)
	 * @throws SQLException se ocorrer erro ao realizar rollback
	 */
	static void rollbackTransaction(Connection conn) throws SQLException {
		if (conn == null) {
			logger.warn("Rollback ignorado: conexão nula");
			return;
		}
		try {
			if (!conn.isClosed() && !conn.getAutoCommit()) {
				conn.rollback();
			}
		} finally {
			if (!conn.isClosed()) {
				conn.setAutoCommit(true);
			}
		}
	}

	/**
	 * Fecha o pool de conexões de forma graceful.
	 */
	static void shutdown() {
		synchronized (RELOAD_LOCK) {
			if (dataSource != null && !dataSource.isClosed()) {
				logger.info("Iniciando shutdown graceful do pool de conexões...");
				dataSource.close();
				logger.info("Pool de conexões fechado com sucesso!");
			}
		}
	}

	/**
	 * Recarrega a configuração do pool de conexões, substituindo o pool antigo.
	 * <p>
	 * Caso ocorra falha, mantém o pool anterior ativo
	 * </p>
	 */
	static void reloadDataSource() {
		synchronized (RELOAD_LOCK) {
			HikariDataSource newDs = null;
			try {
				var props = DatabaseConfigLoader.loadConfig();
				var config = HikariConfigBuilder.buildConfig(props);

				newDs = new HikariDataSource(config);

				validationConnection(newDs);

				HikariDataSource oldDs = dataSource;
				dataSource = newDs;

				if (oldDs != null) {
					logger.info("Fechando pool antigo...");
					oldDs.close();
				}
				logger.info("Pool HikariCP atualizado: {} conexões máximas ", newDs.getMaximumPoolSize());
			} catch (Exception e) {
				logger.error("Falha ao recarregar configuração do banco. Mantendo pool anterior (se existir).", e);
				if (newDs != null) {
					newDs.close();
				}
			}
		}
	}

	/**
	 * Valida se uma conexão obtida do pool é válida.
	 * 
	 * @param ds instância de {@link HikariDataSource}
	 * @throws SQLException se a conexão não for válida
	 */
	private static void validationConnection(HikariDataSource ds) throws SQLException {
		try (Connection conn = ds.getConnection()) {
			if (!conn.isValid(2)) {
				throw new SQLException("Falha na validação inicial da conexão");
			}
		}
	}

	private static final Map<Predicate<String>, String> ERRO_MESSAGES = Map.of(
			msg -> msg.contains("access denied") || msg.contains("autenticação"), "Usuário ou senha incorretos.",
			msg -> msg.contains("connect") || msg.contains("connection refused"), "MySQL não está rodando.",
			msg -> msg.contains("unknown database"), "Banco de dados não encontrado.");

}
