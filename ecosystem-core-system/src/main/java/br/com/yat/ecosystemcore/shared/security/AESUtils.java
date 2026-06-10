package br.com.yat.ecosystemcore.shared.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.shared.exception.CryptoErrorType;
import br.com.yat.ecosystemcore.shared.exception.CryptoException;

/**
 * Classe utilitária para operações de criptografia e descriptografia utilizando <b>AES</b> no modo <b>GCM</b>.
 * <p>
 * Integra:
 * <ul>
 * <li>{@link javax.crypto.Cipher} comm transformação {@code AES/GCM/NoPadding}.</li>
 * <li>{@link SecureRandom} para geração de IVs e chaves seguras.</li>
 * <li>{@link SensitiveData} para limpeza segura de dados sensíveis da memória.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class AESUtils {

	private static final Logger logger = LoggerFactory.getLogger(AESUtils.class);

	public static final String AES_ALGORITHM = "AES";
	private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";

	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int GCM_IV_LENGTH_BYTES = 12;

	public static final int DEFAULT_KEY_SIZE_BITS = 256;

	private static final SecureRandom SECURE_RANDOM = createSecureRandom();

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private AESUtils() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Gera uma chave AES segura
	 * 
	 * @param keySizeBits tamanho da chave em bits (128, 192, 256)
	 * @return chave AES gerada
	 * @throws CryptoException se ocorrer falha na geração da chave
	 */
	public static SecretKey generateKey(int keySizeBits) {
		validateKeySize(keySizeBits);

		try {
			KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
			keyGen.init(keySizeBits, SECURE_RANDOM);
			return keyGen.generateKey();

		} catch (Exception e) {
			logger.error("Falha ao gerar chaves AES de ({} bits)", keySizeBits, e);
			throw new CryptoException(CryptoErrorType.KEY_INVALID, "Falha ao gerar chave AES ", e);
		}
	}

	/**
	 * Criptografa dados em bytes utilizando AES-GCM.
	 * 
	 * @param plaintext dados em texto puro
	 * @param key chave AES válida
	 * @return vetor de bytes contendo IV + ciphertext
	 * @throws CryptoException se ocorrer falha na criptografia
	 */
	public static byte[] encrypt(byte[] plaintext, SecretKey key) {
		validateInputs(plaintext, key);

		byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
		SECURE_RANDOM.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

			byte[] ciphertext = cipher.doFinal(plaintext);

			return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
		} catch (Exception e) {
			logger.error("Falha na criptografia AES-GCM", e);
			throw new CryptoException(CryptoErrorType.ENCRYPTION_FAILED, e);
		} finally {
			SensitiveData.safeClear(iv);
			SensitiveData.safeClear(plaintext);
		}
	}

	/**
	 * Criptografa uma string utilizando AES-GCM.
	 * 
	 * @param plaintext texto puro
	 * @param key chave AES válida
	 * @return vetor de bytes contendo IV + ciphertext
	 */
	public static byte[] encrypt(String plaintext, SecretKey key) {
		Objects.requireNonNull(plaintext, "plaintext não pode ser nulo.");
		return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key);
	}

	/**
	 * Criptografa um array de caracteres utilizando AES-GCM.
	 * <p>
	 * Converte os caracteres para UTF-8 antes da criptografia.
	 * </p>
	 * 
	 * @param plaintext texto puro em caracteres
	 * @param key chave AES válida
	 * @return vetor de bytes contendo IV + ciphertext
	 */
	public static byte[] encrypt(char[] plaintext, SecretKey key) {
		Objects.requireNonNull(plaintext, "plaintext não pode ser nulo");

		ByteBuffer byteBuffer = null;
		byte[] bytes = null;

		try {
			byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(plaintext));
			bytes = new byte[byteBuffer.remaining()];
			byteBuffer.get(bytes);

			return encrypt(bytes, key);
		} finally {
			if (byteBuffer != null && byteBuffer.hasArray()) {
				Arrays.fill(byteBuffer.array(), (byte) 0);
			}
			SensitiveData.safeClear(bytes);
			SensitiveData.safeClear(plaintext);
		}

	}

	/**
	 * Descriptograda dados em bytes utilizando AES-GCM.
	 * 
	 * @param ivAndCiphertext vetor de bytes contendo IV + ciphertext
	 * @param key chave AES válida
	 * @return dados descriptografados em bytes
	 * @throws CryptoException se ocorrer falha na autenticação ou na descriptografia
	 */
	public static byte[] decrypt(byte[] ivAndCiphertext, SecretKey key) {
		validateInputs(ivAndCiphertext, key);
		validateCiphertextLength(ivAndCiphertext);

		byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, GCM_IV_LENGTH_BYTES);
		byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, GCM_IV_LENGTH_BYTES, ivAndCiphertext.length);

		try {
			Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

			return cipher.doFinal(ciphertext);
		} catch (AEADBadTagException e) {
			logger.warn("Falha de autenticação GCM (dados adulterados ou chave incorreta)");
			throw new CryptoException(CryptoErrorType.DATA_CORRUPTED, e);
		} catch (Exception e) {
			logger.error("Falha na descriptografia AES-GCM", e);
			throw new CryptoException(CryptoErrorType.DECRYPTION_FAILED, e);
		} finally {
			SensitiveData.safeClear(iv);
			SensitiveData.safeClear(ciphertext);
		}
	}

	/**
	 * Descriptografa dados e reforma como string UTF-8
	 * 
	 * @param encrypted vetor de bytes contendo IV + ciphertext
	 * @param key chave AES válida
	 * @return texto puro em string
	 */
	public static String decryptToString(byte[] encrypted, SecretKey key) {
		return new String(decrypt(encrypted, key), StandardCharsets.UTF_8);
	}

	/**
	 * Descriptografa dados e reforma como array de caracteres.
	 * 
	 * @param encrypted vetor de bytes contendo IV + ciphertext
	 * @param key chave AES válida
	 * @return texto puro em caracteres
	 */
	public static char[] decryptToChars(byte[] encrypted, SecretKey key) {
		byte[] decrypted = decrypt(encrypted, key);

		try {
			CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(decrypted));
			char[] chars = new char[charBuffer.remaining()];
			charBuffer.get(chars);
			return chars;

		} finally {
			SensitiveData.safeClear(decrypted);
		}

	}

	/**
	 * Valida se o tamanho da chave é suportado (128, 192, 256 bits).
	 * 
	 * @param keySizeBits tamanho da chave em bits
	 * @throws IllegalArgumentException se o tamanho for inválido
	 */
	private static void validateKeySize(int keySizeBits) {
		if (keySizeBits != 128 && keySizeBits != 192 && keySizeBits != 256) {
			throw new IllegalArgumentException("Tamanho de chave AES inválido. Use 128, 192 ou 256 bits.");
		}
	}

	/**
	 * Valida entradas de dados e chave
	 * 
	 * @param data dados a serem validados
	 * @param key chave AES
	 * @throws IllegalArgumentException se os dados ou chave forem nulos ou inválidos
	 */
	private static void validateInputs(byte[] data, SecretKey key) {
		Objects.requireNonNull(data, "Dados não pode ser nulos.");
		Objects.requireNonNull(key, "Chave não pode ser nula.");

		if (!"AES".equalsIgnoreCase(key.getAlgorithm())) {
			throw new IllegalArgumentException("Chave inválida: algoritmo deve ser AES");
		}
	}

	/**
	 * Valida se o ciphertext possui tamnaho mínimo esperado (IV + Tag)
	 * 
	 * @param data vetor de bytes contendo IV + ciphertext
	 * @throws CryptoException se o tamanho for inválido
	 */
	private static void validateCiphertextLength(byte[] data) {
		int minLength = GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8);
		if (data.length < minLength) {
			throw new CryptoException(CryptoErrorType.DATA_CORRUPTED);
		}
	}

	/**
	 * Cria uma instância segura de {@link SecureRandom}
	 * 
	 * @return instância de {@link SecureRandom}
	 */
	private static SecureRandom createSecureRandom() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch (Exception e) {
			logger.warn("SecureRandom forte indisponivel, usando fallback padrao");
			return new SecureRandom();
		}
	}

}
