package br.com.yat.ecosystemcore.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.domain.entity.DatabaseCredentials;
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
	 * Realiza um teste de conexão isolado em memória utilizando os dados informados,
	 * sem alterar o DataSource principal da aplicação ou persistir configurações.
	 * * @param dto Dados de configuração temporários para teste
	 * @return instância de {@link DatabaseStatus} indicando o resultado do teste
	 */
//	public static DatabaseStatus testRawConnection(DatabaseConfig config, DatabaseCredentials credentials) {
//		return ConnectionPoolManager.testTemporaryConnection(config, credentials);
//	}
//	
//	public static DatabaseStatus testRawConnection(DatabaseConfig config, DatabaseCredentials credentials) {
//	    Objects.requireNonNull(config, "Configuração do banco não pode ser nula");
//	    Objects.requireNonNull(credentials, "Credenciais do banco não podem ser nulas");
//	    
//	    final DatabaseStatus[] resultado = new DatabaseStatus[1];
//	    
//	    try {
//	        credentials.executarComSenha(senhaAtiva -> {
//	            java.util.Properties tempProps = new java.util.Properties();
//	            tempProps.setProperty("db.url", config.gerarJdbcUrl());
//	            tempProps.setProperty("db.user", config.usuario());
//	            tempProps.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
//	            
//	            if (senhaAtiva != null && senhaAtiva.length > 0) {
//	                tempProps.setProperty("db.password", new String(senhaAtiva));
//	            }
//
//	            tempProps.setProperty("db.connectionTimeout", "4000");
//	            tempProps.setProperty("db.validationTimeout", "2000");
//	            tempProps.setProperty("db.poolSize", "1"); 
//
//	            // Criamos o HikariConfig temporário usando o Builder
//	            try (com.zaxxer.hikari.HikariDataSource tempDs = new com.zaxxer.hikari.HikariDataSource(HikariConfigBuilder.buildConfig(tempProps))) {
//	                try (Connection conn = tempDs.getConnection()) {
//	                    if (conn.isValid(2)) {
//	                        resultado[0] = DatabaseStatus.ok();
//	                    } else {
//	                        resultado[0] = DatabaseStatus.error("Conexão inválida no teste.");
//	                    }
//	                }
//	            } catch (Exception e) {
//	                String msg = e.getMessage().toLowerCase();
//	                // Nota: Como ERRO_MESSAGES está encapsulado na outra classe, tratamos o erro diretamente ou simplificamos aqui:
//	                if (msg.contains("access denied") || msg.contains("autenticação")) {
//	                    resultado[0] = DatabaseStatus.error("Usuário ou senha incorretos.");
//	                } else if (msg.contains("connect") || msg.contains("connection refused")) {
//	                    resultado[0] = DatabaseStatus.error("MySQL não está rodando.");
//	                } else if (msg.contains("unknown database")) {
//	                    resultado[0] = DatabaseStatus.error("Banco de dados não encontrado.");
//	                } else {
//	                    resultado[0] = DatabaseStatus.error("Falha na conexão de teste: " + e.getMessage());
//	                }
//	            }
//	        });
//	        
//	        return resultado[0] != null ? resultado[0] : DatabaseStatus.error("Teste não foi executado corretamente.");
//	        
//	    } catch (Exception e) {
//	        return DatabaseStatus.error("Erro crítico ao processar credenciais: " + e.getMessage());
//	    }
//	}

	public static DatabaseStatus testRawConnection(DatabaseConfig config, DatabaseCredentials credentials) {
	    Objects.requireNonNull(config, "Configuração do banco não pode ser nula");
	    Objects.requireNonNull(credentials, "Credenciais do banco não podem ser nulas");
	    
	    final DatabaseStatus[] resultado = new DatabaseStatus[1];
	    
	    try {
	        credentials.executarComSenha(senhaAtiva -> {
	            // Instanciamos a configuração nativa do HikariCP sem passar pelo seu Builder customizado
	            com.zaxxer.hikari.HikariConfig hikariConfig = new com.zaxxer.hikari.HikariConfig();
	            
	            // Injetamos as propriedades limpas e descriptografadas que vieram da UI
	            hikariConfig.setJdbcUrl(config.gerarJdbcUrl());
	            hikariConfig.setUsername(config.usuario());
	            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
	            
	            if (senhaAtiva != null && senhaAtiva.length > 0) {
	                hikariConfig.setPassword(new String(senhaAtiva));
	            }

	            // Timeouts curtos para que a resposta na UI seja rápida se o banco estiver fora do ar
	            hikariConfig.setConnectionTimeout(4000);
	            hikariConfig.setValidationTimeout(2000);
	            hikariConfig.setMaximumPoolSize(1); 

	            // Criamos o DataSource temporário isolado de toda a infraestrutura
	            try (com.zaxxer.hikari.HikariDataSource tempDs = new com.zaxxer.hikari.HikariDataSource(hikariConfig)) {
	                try (Connection conn = tempDs.getConnection()) {
	                    if (conn.isValid(2)) {
	                        resultado[0] = DatabaseStatus.ok();
	                    } else {
	                        resultado[0] = DatabaseStatus.error("Conexão inválida no teste.");
	                    }
	                }
	            } catch (Exception e) {
	                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
	                if (msg.contains("access denied") || msg.contains("autenticação")) {
	                    resultado[0] = DatabaseStatus.error("Usuário ou senha incorretos.");
	                } else if (msg.contains("connect") || msg.contains("connection refused")) {
	                    resultado[0] = DatabaseStatus.error("MySQL não está rodando.");
	                } else if (msg.contains("unknown database")) {
	                    resultado[0] = DatabaseStatus.error("Banco de dados não encontrado.");
	                } else {
	                    resultado[0] = DatabaseStatus.error("Falha na conexão de teste: " + e.getMessage());
	                }
	            }
	        });
	        
	        return resultado[0] != null ? resultado[0] : DatabaseStatus.error("Teste não foi executado corretamente.");
	        
	    } catch (Exception e) {
	        return DatabaseStatus.error("Erro crítico ao processar credenciais: " + e.getMessage());
	    }
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
