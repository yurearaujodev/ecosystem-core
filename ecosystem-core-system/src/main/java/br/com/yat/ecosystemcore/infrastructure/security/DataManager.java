package br.com.yat.ecosystemcore.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;

/**
 * Classe utilitária para gerenciamento de dados criptografados em arquivos.
 * <p>
 * Integra:
 * <ul>
 * <li>{@link AESUtils} para criptografia e descriptografia com AES-GCM.</li>
 * <li>{@link FileManager} para opeções de leitura e escrita em arquivos.</li>
 * <li>{@link SensitiveData} para limpeza segura de dados sensíveis da memória.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class DataManager {

	private static final Logger logger = LoggerFactory.getLogger(DataManager.class);

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private DataManager() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Criptografa uma string e salva em arquivo.
	 * 
	 * @param file caminho do arquivo destino
	 * @param data texto puro a ser criptografado
	 * @param key chave AES utilizando na criptografia
	 * @throws CryptoException se ocorrer falha ao salvar os dados
	 */
	public static void encryptAndSave(Path file, String data, SecretKey key) {
		Objects.requireNonNull(file, "file não pode ser nula");
		Objects.requireNonNull(data, "data não pode ser nula");
		Objects.requireNonNull(key, "key não pode ser nula");

		encryptAndSave(file, data.getBytes(StandardCharsets.UTF_8), key);
	}

	/**
	 * Carrega e descriptografa dados de um arquivo, retornando como string.
	 * 
	 * @param file caminho do arquivo origem
	 * @param key chave AES utilizada na descriptografia
	 * @return texto puro em string
	 * @throws CryptoException se ocorrer falha ao carregar ou descriptografar
	 */
	public static String decryptAndLoad(Path file, SecretKey key) {
		Objects.requireNonNull(file, "file não pode ser nula");
		Objects.requireNonNull(key, "key não pode ser nula");

		byte[] decrypted = decryptAndLoadBytes(file, key);
		try {
			return new String(decrypted, StandardCharsets.UTF_8);
		} finally {
			SensitiveData.safeClear(decrypted);
		}
	}

	/**
	 * Criptografa dados em bytes e salva em arquivo.
	 * 
	 * @param file caminho do arquivo destino
	 * @param data dados em bytes a serem criptografados
	 * @param key chave AES utilizanda na criptografia
	 * @throws CryptoException se ocorrer falha ao salvar os dados
	 */
	public static void encryptAndSave(Path file, byte[] data, SecretKey key) {
		Objects.requireNonNull(file, "file não pode ser nula");
		Objects.requireNonNull(data, "data não pode ser nula");
		Objects.requireNonNull(key, "key não pode ser nula");

		byte[] encrypted = AESUtils.encrypt(data, key);
		try {
			FileManager.save(file, encrypted);
			logger.debug("Dados criptografados salvos: {}", file);
		} catch (CryptoException e) {
			logger.error("Falha ao salvar dados criptografados: {}", file);
			throw e;
		} catch (Exception e) {
			logger.error("Erro inesperado ao salvar dados criptografados: {}", file, e);
			throw new CryptoException(CryptoErrorType.IO_ERROR, "Erro inesperado ao salvar dados criptografados", e);
		} finally {
			SensitiveData.safeClear(encrypted);
		}
	}

	/**
	 * Carrega e descriptografa dados de um arquivo, retornando como bytes.
	 * 
	 * @param file caminho do arquivo origem
	 * @param key chave AES utilizada na descriptografia
	 * @return dados descriptografados em bytes
	 * @throws CryptoException se ocorrer falha ao carregar ou descriptografar
	 */
	public static byte[] decryptAndLoadBytes(Path file, SecretKey key) {
		Objects.requireNonNull(file, "file não pode ser nula");
		Objects.requireNonNull(key, "key não pode ser nula");

		byte[] encrypted = null;

		try {
			encrypted = FileManager.load(file);
			byte[] decrypted = AESUtils.decrypt(encrypted, key);
			logger.debug("Dados descriptografados carregados com sucesso: {}", file);
			return decrypted;
		} catch (CryptoException e) {
			logger.error("Falha ao carregar ou descriptografar {}", file, e);
			throw e;
		} catch (Exception e) {
			logger.error("Erro inesperado ao carregar ou descriptografar dados: {}", file, e);
			throw new CryptoException(CryptoErrorType.INTERNAL_ERROR,
					"Erro inesperado ao carregar ou descriptografar dados", e);
		} finally {
			SensitiveData.safeClear(encrypted);

		}
	}

}
