package br.com.yat.ecosystemcore.configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;
import br.com.yat.ecosystemcore.infrastructure.security.CryptoException;

/**
 * Classe utilitária responsável por carregar e validar configurações de banco de dados.
 * <p>
 * Utiliza:
 * <ul>
 * <li>{@link Properties} para leitura de arquivos de configuração.</li>
 * <li>{@link java.nio.file.Files} para manipulação de diretórios e arquivos</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * <p>Não deve ser instanciada.</p>
 */
public final class DatabaseConfigLoader {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigLoader.class);
	private static final Path CONFIG_DIR = Paths.get("config", "database");
	private static final Path DB_CONFIG_FILE = CONFIG_DIR.resolve("db.properties");

	private static final String[] REQUIRED_PROPERTIES = { "db.url", "db.user", "db.password", "db.driver" };

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private DatabaseConfigLoader() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Carrega as configurações do banco a partir do arquivo {@code db.properties}.
	 * <p>
	 * Garante que o diretório e o arquivo existem, valida propriedades obrigatórias
	 * e retorna um objeto {@link Properties}.
	 * </p>
	 * 
	 * @return objeto {@link Properties} contendo as configurações
	 * @throws CryptoException se houver falha ao carregar ou validar o arquivo
	 */
	public static Properties loadConfig() {
		ensureConfigDirectory();
		ensureConfigFile();

		try (InputStream in = Files.newInputStream(DB_CONFIG_FILE)) {
			Properties props = new Properties();
			props.load(in);
			validateRequiredProperties(props);

			logger.info("Configuração de banco carregada com sucesso");
			return props;
		} catch (CryptoException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Erro ao ler configuração do banco: {}", DB_CONFIG_FILE, e);
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Falha ao carregar configuração do banco", e);
		}
	}

	/**
	 * Garante que o diretório de configuração existe.
	 * 
	 * @throws CryptoException se não for possível criar o diretório
	 */
	private static void ensureConfigDirectory() {
		try {
			Files.createDirectories(CONFIG_DIR);
		} catch (Exception e) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR,
					"Não foi possivel criar diretório de configuração: " + CONFIG_DIR, e);
		}
	}

	/**
	 * Garante que o arquivo de configuração existe e é válido.
	 * 
	 * @throws CryptoException se o arquivo estiver ausente, inválido ou sem permissão de leitura
	 */
	private static void ensureConfigFile() {
		if (!Files.exists(DB_CONFIG_FILE)) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR,
					"Arquivo de configuraçao ausente: " + DB_CONFIG_FILE.toAbsolutePath());
		}

		if (!Files.isRegularFile(DB_CONFIG_FILE)) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR,
					"Configuração inválida (Não é arquivo)" + DB_CONFIG_FILE);
		}

		if (!Files.isReadable(DB_CONFIG_FILE)) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Sem permissão de leitura: " + DB_CONFIG_FILE);
		}
	}

	/**
	 * Valida se todas as propriedades obrigatórias estão presentes.
	 * 
	 * 
	 * @param props objeto {@link Properties} carregado
	 * @throws CryptoException se alguma propriedade obrigatória estiver ausente
	 */
	public static void validateRequiredProperties(Properties props) {
		Objects.requireNonNull(props, "Properties não pode ser nulo");

		for (String key : REQUIRED_PROPERTIES) {
			String value = props.getProperty(key);
			if (value == null || value.isBlank()) {
				throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Propriedade obrigatória ausente: " + key);
			}
		}
	}

	/**
	 * Obtém uma propriedade numérica inteira.
	 * 
	 * @param props objeto {@link Properties}
	 * @param key chave da propriedade
	 * @param defaultValue valor padrão caso a propriedade seja inválida ou ausente
	 * @return valor inteiro da propriedade ou {@code defaultValue}
	 */
	public static int getIntProperty(Properties props, String key, int defaultValue) {
		Objects.requireNonNull(props, "Properties não pode ser nulo");

		String value = props.getProperty(key);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			logger.error("Valor inválido para {}: '{}', usando {}", key, value, defaultValue);
			return defaultValue;
		}
	}

	/**
	 * Obtém uma propriedade numérica longa.
	 * 
	 * @param props objeto {@link Properties}
	 * @param key chave da propriedade
	 * @param defaultValue valor padrão caso a propriedade seja inválida ou ausente
	 * @return valor longo da propriedade ou {@code defaultValue}
	 */
	public static long getLongProperty(Properties props, String key,long defaultValue) {
		Objects.requireNonNull(props, "Properties não pode ser nulo");

		String value = props.getProperty(key);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			logger.error("Valor inválido para {}: '{}', usando {}", key, value, defaultValue);
			return defaultValue;
		}
	}
}
