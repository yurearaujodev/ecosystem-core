package br.com.yat.ecosystemcore.shared.security;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.shared.exception.CryptoErrorType;
import br.com.yat.ecosystemcore.shared.exception.CryptoException;
import br.com.yat.ecosystemcore.shared.util.FileManager;

/**
 * Classe utilitária para gerenciamento de chaves criptográficas.
 * <p>
 * Integra:
 * <ul>
 * <li>{@link AESUtils} para geração de chaves simétricas AES.</li>
 * <li>{@link RSAUtils} para geração de pares de chaves RSA.</li>
 * <li>{@link FileManager} para persistência de chaves em arquivos.</li>
 * <li>{@link SensitiveData} para limpeza segura de dados sensíveis da memória.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class KeyManager {

	private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private KeyManager() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Gera uma chave AES e salva em arquivo.
	 * 
	 * @param file caminho do arquivo destino
	 * @return chave AES gerada
	 * @throws CryptoException se ocorrer falha ao salvar a chave
	 */
	public static SecretKey generateAndSaveAES(Path file) {
		Objects.requireNonNull(file, "file não pode ser nula");

		SecretKey key = AESUtils.generateKey(AESUtils.DEFAULT_KEY_SIZE_BITS);
		byte[] encoded = key.getEncoded();

		try {
			FileManager.save(file, encoded);
			logger.debug("Chave AES gerada e salva com sucesso: {}", file);
			return key;
		} catch (CryptoException e) {
			logger.error("Falha ao salvar chave AES: {}", file, e);
			throw e;
		} finally {
			SensitiveData.safeClear(encoded);
		}
	}

	/**
	 * Carrega uma chave AES de arquivo.
	 * 
	 * @param file caminho do arquivo origem
	 * @return chave AES carregada
	 * @throws CryptoException se ocorrer falha ao carregar ou validar a chave
	 */
	public static SecretKey loadAES(Path file) {
		Objects.requireNonNull(file, "file não pode ser nula");

		byte[] encoded = FileManager.load(file);

		try {
			validateAESKey(encoded);
			SecretKey key = new SecretKeySpec(encoded, AESUtils.AES_ALGORITHM);
			logger.debug("Chave AES carregada com sucesso: {}", file);
			return key;
		} catch (CryptoException e) {
			logger.error("Falha ao carregar chave AES: {}", file, e);
			throw e;
		} finally {
			SensitiveData.safeClear(encoded);
		}
	}

	/**
	 * Gera um par de chaves RSA.
	 * 
	 * @param sizeBits tamanho da chave em bits (mínimo 2048, recomendado 3072 ou superior)
	 * @return par de chaves RSA ({@link KeyPair}
	 * @throws CryptoException se ocorrer falha na geração das chaves
	 */
	public static KeyPair generateRSA(int sizeBits) {
		KeyPair pair = RSAUtils.generateKeyPair(sizeBits);
		logger.debug("Par de chaves RSA gerado ({} bits)", sizeBits);
		return pair;
	}

	/**
	 * Valida se os dados representam uma chave AES válida.
	 * 
	 * @param encoded chave em bytes
	 * @throws CryptoException se a chave for nula ou tiver tamanho inválido
	 */
	private static void validateAESKey(byte[] encoded) {
		if (encoded == null) {
			throw new CryptoException(CryptoErrorType.KEY_INVALID, "Chave AES inválida: dados nulos");
		}

		int length = encoded.length;
		if (length != 16 && length != 24 && length != 32) {
			throw new CryptoException(CryptoErrorType.KEY_INVALID,
					"Tamanho inválido de chave AES: " + length + " bytes");
		}
	}

}
