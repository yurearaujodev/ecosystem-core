package br.com.yat.ecosystemcore.shared.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.shared.exception.CryptoErrorType;
import br.com.yat.ecosystemcore.shared.exception.CryptoException;


/**
 * Classe utilitária responsável por descriptografar a senha do banco de dados
 * <p>
 * Utiliza:
 * <ul>
 * <li><b>Base64</b> para decodificação da senha criptografada.</li>
 * <li><b>AES</b> via {@link AESUtils} e {@link KeyManager} para
 * descriptografia</li>
 * <li>{@link SensitiveData} para limpeza segura de dados sensíveis da
 * memória.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Não deve ser instanciada.
 * </p>
 */
public final class DatabasePasswordDecryptor {
	private static final Logger logger = LoggerFactory.getLogger(DatabasePasswordDecryptor.class);
	private static final Path CONFIG_DIR = Paths.get("config", "database");
	private static final Path GLOBAL_MASTER_KEY_FILE = CONFIG_DIR.resolve("master.key");
	private static final int MAX_ENCRYPTED_PASSWORD_LENGTH = 4096;

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private DatabasePasswordDecryptor() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Descriptografia a senha do banco de dados.
	 * <p>
	 * Decodifica a senha em Base64 e utiliza AES com chave mestra para obter a
	 * senha em texto puro.
	 * </p>
	 * 
	 * @param encryptedB64 senha criptografada em Base64
	 * @return senha em texto puro como array de caracteres
	 * @throws CryptoException se houver falha na validação ou na descriptografia
	 */
	public static char[] decryptPassword(String encryptedB64) {
		Objects.requireNonNull(encryptedB64, "Senha criptografada não pode ser nula");

		if (encryptedB64.isBlank()) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Senha do banco não configurada");
		}

		if (encryptedB64.length() > MAX_ENCRYPTED_PASSWORD_LENGTH) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Senha criptografada excede o tamanho permitido");
		}

		validateMasterKeyFile();

		byte[] encrypted = null;
		char[] passwordChars = null;

		try {
			SecretKey masterKey = KeyManager.loadAES(GLOBAL_MASTER_KEY_FILE);

			encrypted = Base64.getDecoder().decode(encryptedB64.trim());
			passwordChars = AESUtils.decryptToChars(encrypted, masterKey);

			logger.debug("Senha do banco descriptografada com sucesso");
			return passwordChars;
		} catch (IllegalArgumentException e) {
			logger.error("Senha não está em Base64 válido", e);
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Senha criptografada inválida (Base64)", e);

		} catch (CryptoException e) {
			throw e;

		} catch (Exception e) {
			logger.error("Falha ao descriptografar senha do banco", e);
			throw new CryptoException(CryptoErrorType.DECRYPTION_FAILED, "Falha ao descriptografar senha do banco", e);
		} finally {
			if (encrypted != null) {
				SensitiveData.safeClear(encrypted);
			}

		}

	}

	/**
	 * Valida se o arquivo de chave mestra existe e possui permissões adequadas.
	 * 
	 * @throws CryptoException se o arquivo estiver ausente, inválido ou sem
	 *                         permissão de leitura
	 */
	private static void validateMasterKeyFile() {
		if (!Files.exists(GLOBAL_MASTER_KEY_FILE)) {
			throw new CryptoException(CryptoErrorType.KEY_NOT_FOUND, "Chave mestra do banco não encontrada");
		}

		if (!Files.isRegularFile(GLOBAL_MASTER_KEY_FILE)) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR,
					"Arquivo de chave mestra inválido: " + GLOBAL_MASTER_KEY_FILE);
		}

		if (!Files.isReadable(GLOBAL_MASTER_KEY_FILE)) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR,
					"Sem permissão de leitura da chave mestra do banco");
		}
	}
}
