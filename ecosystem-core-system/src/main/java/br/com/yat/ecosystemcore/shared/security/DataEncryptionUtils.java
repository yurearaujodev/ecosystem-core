package br.com.yat.ecosystemcore.shared.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.shared.exception.CryptoErrorType;
import br.com.yat.ecosystemcore.shared.exception.CryptoException;
import br.com.yat.ecosystemcore.shared.util.FileManager;

/**
 * Classe utilitária para gerenciamento de dados criptografados com chaves AES.
 * <p>
 * Responsável por:
 * <ul>
 * <li>Gerar e salvar chaves AES em arquivos.</li>
 * <li>Criptografar e salvar dados associados a um identificador.</li>
 * <li>Carregar e descriptografar dados.</li>
 * <li>Validar integridade e existência de dados e chaves.</li>
 * </ul>
 * <p>
 * 
 * <p>Integra:
 * <ul>
 * <li>{@link KeyManager} para gerenciamento de chaves AES.</li>
 * <li>{@link DataManager} para criptografia e persitência de dados.</li>
 * <li>{@link FileManager} para operações de I/O.</li>
 * <li>{@link AESUtils} para validação de dados criptografados.</li>
 * <li>{@link SensitiveData} para limpeza segura de dados sensíveis.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class DataEncryptionUtils {

	private static final Logger logger = LoggerFactory.getLogger(DataEncryptionUtils.class);
	private static final Path SECURE_DATA_DIR = Paths.get("config", "secure_data");
	private static final String KEY_EXT = ".key";
	private static final String DATA_EXT = ".dat";
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+");

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private DataEncryptionUtils() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Gera uma chave AES e salva em arquivo associado ao identificador.
	 * Se já existir, carrega a chave existente.
	 * 
	 * @param identifier identificador único dos dados
	 * @return chave AES gerada ou carregada
	 * @throws CryptoException se ocorrer falha ao salvar ou carregar a chave
	 */
	public static SecretKey generateAndSaveKey(String identifier) {
		validateIdentifier(identifier);
		ensureSecureDirectory();

		Path keyFile = keyPath(identifier);

		if (Files.exists(keyFile)) {
			logger.debug("Chave já existe para {}, carregando...", identifier);
			return KeyManager.loadAES(keyFile);
		}

		logger.warn("Chave inexistente para {}. Gerando nova.", identifier);
		return KeyManager.generateAndSaveAES(keyFile);
	}

	/**
	 * Carrega uma chave AES associada ao identificador.
	 * 
	 * @param identifier identificador único dos dados
	 * @return chave AES carregada
	 * @throws CryptoException se a chave não for encontrada ou inválida
	 */
	public static SecretKey loadKey(String identifier) {
		validateIdentifier(identifier);
		ensureSecureDirectory();

		Path keyFile = keyPath(identifier);
		if (!Files.exists(keyFile)) {
			throw new CryptoException(CryptoErrorType.KEY_NOT_FOUND, "Chave não encontrada para: " + identifier);
		}
		return KeyManager.loadAES(keyFile);

	}

	/**
	 * Criptografa dados em texto e salva em arquivo associado ao identificador.
	 * 
	 * @param identifier identificador único dos dados
	 * @param data texto puro a ser criptografado
	 * @throws CryptoException se ocorrer falha ao salvar os dados
	 */
	public static void encryptAndSave(String identifier, String data) {
		validateIdentifier(identifier);
		Objects.requireNonNull(data, "data não pode ser nulo");
		ensureSecureDirectory();

		SecretKey key = loadKey(identifier);
		Path dataFile = dataPath(identifier);

		DataManager.encryptAndSave(dataFile, data, key);
		logger.debug("Dados criptografados salvos: {}", identifier);
	}

	/**
	 * Carrega e descriptografa dados associados ao identificador.
	 * 
	 * @param identifier identificador único dos dados
	 * @return texto puro descriptografado
	 * @throws CryptoException se os dados estiverem corrompidos ou incompletos
	 */
	public static String decryptAndLoad(String identifier) {
		validateIdentifier(identifier);
		ensureSecureDirectory();

		Path keyFile = keyPath(identifier);
		Path dataFile = dataPath(identifier);

		if (!isDataValidInternal(keyFile, dataFile)) {
			throw new CryptoException(CryptoErrorType.DATA_CORRUPTED,
					"Dados corrompidos ou incompletos para: " + identifier);
		}

		SecretKey key = loadKey(identifier);
		return DataManager.decryptAndLoad(dataFile, key);

	}

	/**
	 * Verifica se os dados associados ao identificador são válidos.
	 * 
	 * @param identifier identificador único dos dados
	 * @return {@code true} se os dados forem válidos, {@code false} caso contrário
	 */
	public static boolean isDataValid(String identifier) {
		validateIdentifier(identifier);
		ensureSecureDirectory();

		return isDataValidInternal(keyPath(identifier), dataPath(identifier));
	}

	/**
	 * Verifica se existem chave e dados completos associados ao identificador.
	 * 
	 * @param identifier identificador único dos dados
	 * @return {@code true} se ambos existirem,{@code false} caso contrário
	 */
	public static boolean hasCompleteData(String identifier) {
		validateIdentifier(identifier);
		ensureSecureDirectory();

		return Files.exists(keyPath(identifier)) && Files.exists(dataPath(identifier));

	}

	/**
	 * Valida internamente se chave e dados são consistentes e não corrompidos.
	 * 
	 * @param keyFile arquivo da chave
	 * @param dataFile arquivo dos dados
	 * @return {@code true} se os dados forem válidos,{@code false} caso contrário
	 */
	private static boolean isDataValidInternal(Path keyFile, Path dataFile) {
		if (!Files.exists(keyFile) || !Files.exists(dataFile)) {
			return false;
		}

		byte[] encrypted = null;
		try {
			SecretKey key = KeyManager.loadAES(keyFile);
			encrypted = FileManager.load(dataFile);

			AESUtils.decrypt(encrypted, key);
			return true;

		} catch (Exception e) {
			logger.warn("Falha na validação dos dados criptografados: {}", e.getMessage());
			return false;
		} finally {
			if (encrypted != null) {
				SensitiveData.safeClear(encrypted);
			}
		}

	}

	/**
	 * Garante que o diretório seguro existe e possui permissão de escrita.
	 * 
	 * @throws CryptoException se o diretório não for válido ou não puder ser criado
	 */
	private static void ensureSecureDirectory() {
		try {

			if (Files.exists(SECURE_DATA_DIR)) {
				if (!Files.isDirectory(SECURE_DATA_DIR) || !Files.isWritable(SECURE_DATA_DIR)) {
					throw new CryptoException(CryptoErrorType.CONFIG_ERROR,
							"Diretório seguro inválido ou sem permissão: " + SECURE_DATA_DIR);
				}

				return;
			}
			Files.createDirectories(SECURE_DATA_DIR);

		} catch (Exception e) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Falha ao preparar diretório seguro", e);
		}
	}

	/**
	 * Retorna o caminho do arquivo de chave associado ao identificador.
	 * 
	 * @param identifier identificador único
	 * @return caminho do arquivo de chave
	 */
	private static Path keyPath(String identifier) {
		return SECURE_DATA_DIR.resolve(identifier + KEY_EXT);
	}

	/**
	 * Retorna o caminho do arquivo de dados associado ao identificador.
	 * 
	 * @param identifier identificador único
	 * @return caminho do arquivo de dados
	 */
	private static Path dataPath(String identifier) {
		return SECURE_DATA_DIR.resolve(identifier + DATA_EXT);
	}

	/**
	 * Valida se o identificador é válido (não nulo, não vazio e sem caracteres inválidos).
	 * 
	 * @param identifier identificador único
	 * @throws CryptoException se o identificador for inválido
	 */
	private static void validateIdentifier(String identifier) {
		Objects.requireNonNull(identifier, "identifier não pode ser nulo");

		if (identifier.isBlank()) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "identificador não pode ser vazio");
		}

		if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "identificador contém caracteres inválidos");
		}
	}

}
