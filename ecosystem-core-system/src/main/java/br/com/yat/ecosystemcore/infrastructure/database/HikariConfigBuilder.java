package br.com.yat.ecosystemcore.infrastructure.database;

import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import br.com.yat.ecosystemcore.configuration.DatabaseConfigLoader;
import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;
import br.com.yat.ecosystemcore.infrastructure.security.CryptoException;
import br.com.yat.ecosystemcore.infrastructure.security.DatabaseFieldDecryptor;
import br.com.yat.ecosystemcore.infrastructure.security.DatabasePasswordDecryptor;
import br.com.yat.ecosystemcore.infrastructure.security.SensitiveData;

/**
 * Classe utilitária para construção de configurações do pool de conexões
 * HikariCP.
 * <p>
 * Esta classe utiliza a biblioteca <b>HikariCP</b>
 * ({@code com.zaxxer.hikari.HikariConfig}) para gerenciamento de conexões,
 * integrando com:
 * <ul>
 * <li>{@link DatabasePasswordDecryptor} para descriptografar a senha do
 * banco.</li>
 * <li>{@link SensitiveData} para limpeza segura da senha em memória.
 * <li>
 * <li><b>SLF4J</b> para logging</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Não deve ser instanciada.
 * </p>
 */
public final class HikariConfigBuilder {
	private static final Logger logger = LoggerFactory.getLogger(HikariConfigBuilder.class);

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private HikariConfigBuilder() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Constrói uma configuração HikariCP a partir das propriedades fornecidas.
	 * <p>
	 * Valida propriedades obrigatórias, carrega o driver JDBC, descriptografa a
	 * senha e aplica otimizações específicas para MYSQL/MariaDB.
	 * </p>
	 * 
	 * @param props propriedades de configuração do banco
	 * @return instância de {@link HikariConfig} configurada
	 * @throws CryptoException se alguma propriedade obrigatória estiver ausente ou
	 *                         inválida
	 */
	public static HikariConfig buildConfig(Properties props) {
		Objects.requireNonNull(props, "Properties não pode ser nulo");

		validateRequired(props);

		HikariConfig config = new HikariConfig();

		String user = DatabaseFieldDecryptor.decryptToString(props.getProperty("db.user"),"User");

		String jdbcUrl = DatabaseFieldDecryptor.decryptToString(props.getProperty("db.url"),"URL");
		String driverClass = props.getProperty("db.driver");

		config.setJdbcUrl(jdbcUrl);
		config.setUsername(user);
		config.setDriverClassName(driverClass);

		loadJdbcDriver(driverClass);

		char[] passwordChars = null;
		try {
			passwordChars = DatabasePasswordDecryptor.decryptPassword(props.getProperty("db.password"));

			config.setPassword(new String(passwordChars));
		} finally {
			if (passwordChars != null) {
				SensitiveData.safeClear(passwordChars);
			}
		}
		config.setPoolName(props.getProperty("db.poolName", "YAT-HikariPool"));

		configurePool(config, props);

		if (jdbcUrl.contains("mysql") || jdbcUrl.contains("mariadb")) {
			configureMysqlOptimizations(config);
		}

		configureLocalizationAndSsl(config, props);

		logger.info("HIkariConfig criado com sucesso para URL: {}", config.getJdbcUrl());
		return config;

	}

	/**
	 * Configura parâmetros do pool de conexões como tamanho máximo, mínimo de de
	 * conexões e timeouts
	 * 
	 * @param config instância de {@link HikariConfig} a ser configurada
	 * @param props  propriedades de configuração
	 */
	private static void configurePool(HikariConfig config, Properties props) {
		config.setMaximumPoolSize(DatabaseConfigLoader.getIntProperty(props, "db.poolSize", 5));
		config.setMinimumIdle(DatabaseConfigLoader.getIntProperty(props, "db.minIdle", 1));
		config.setConnectionTimeout(DatabaseConfigLoader.getLongProperty(props, "db.connectionTimeout", 300_000L));
		config.setIdleTimeout(DatabaseConfigLoader.getLongProperty(props, "db.idleTimeout", 30_000L));
		config.setMaxLifetime(DatabaseConfigLoader.getLongProperty(props, "db.maxLifeTime", 1_200_000L));
		config.setLeakDetectionThreshold(
				DatabaseConfigLoader.getLongProperty(props, "db.leakDetectionThreshold", 60_000L));
		config.setValidationTimeout(DatabaseConfigLoader.getLongProperty(props, "db.validationTimeout", 5_000L));
	}

	/**
	 * Configura otimizações específicas para MySQL/MariaDB, como cache de prepared
	 * statements.
	 * 
	 * @param config instância de {@link HikariConfig} a ser configurada
	 */
	private static void configureMysqlOptimizations(HikariConfig config) {
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "300");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.addDataSourceProperty("useLocalSessionState", "true");
		config.addDataSourceProperty("rewriteBatchedStatements", "true");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");
		config.addDataSourceProperty("cacheServerConfiguration", "true");
		config.addDataSourceProperty("elideSetAutoCommits", "true");
		config.addDataSourceProperty("maintainTimeStats", "false");
	}

	/**
	 * Configura propriedades de localização e SSL.
	 * 
	 * @param config instância de {@link HikariConfig} a ser configurada
	 * @param props  propriedades de configuração
	 */
	private static void configureLocalizationAndSsl(HikariConfig config, Properties props) {
		config.addDataSourceProperty("serverTimezone", props.getProperty("db.serverTimezone", "America/Sao_Paulo"));
		config.addDataSourceProperty("characterEncoding", props.getProperty("db.characterEncoding", "UTF-8"));
		config.addDataSourceProperty("useUnicode", props.getProperty("db.useUnicode", "true"));

		String useSSL = props.getProperty("db.useSSL", "false");
		config.addDataSourceProperty("useSSL", useSSL);
		if ("false".equalsIgnoreCase(useSSL)) {
			config.addDataSourceProperty("allowPublicKeyRetrieval",
					props.getProperty("db.allowPublicKeyRetrieval", "true"));
		}
	}

	/**
	 * Valida se todas as propriedades obrigatórias estão presentes.
	 * 
	 * @param props propriedade de configuração
	 * @throws CryptoException se alguma propriedade obrigatória estiver ausente
	 */
	private static void validateRequired(Properties props) {
		String[] required = { "db.url", "db.user", "db.password", "db.driver" };
		for (String key : required) {
			if (props.getProperty(key) == null || props.getProperty(key).isBlank()) {
				throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Propriedade obrigatória ausente: " + key);
			}
		}
	}

	/**
	 * Carrega dinamicamente o driver JDBC informado.
	 * 
	 * @param driverClass nome da classe do driver JDBC
	 * @throws CryptoException se o driver não for encontrado
	 */
	private static void loadJdbcDriver(String driverClass) {
		try {
			Class.forName(driverClass);
			logger.debug("Driver JDBC carregado: {}", driverClass);
		} catch (ClassNotFoundException e) {
			logger.error("Driver JDBC não encontrado: {}", driverClass, e);
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Driver JDBC não encontrado: " + driverClass);
		}
	}

}
