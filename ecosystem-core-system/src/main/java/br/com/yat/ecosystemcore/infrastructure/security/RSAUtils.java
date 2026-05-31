package br.com.yat.ecosystemcore.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;

/**
 * Classe utilitária para operações de criptografia assimétrica utilizando <b>RSA</b>.
 * <p>
 * Integra:
 * <ul>
 * <li>{@link java.security.KeyPairGenerator} para geração de pares de chave RSA.</li>
 * <li>{@link Signature} com algoritmo {@code RSASSA-PSS} e parâmetros SHA-256/MGF1.</li>
 * <li>{@link SecureRandom} para geração segura de chaves e assinaturas.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class RSAUtils {
	private static final Logger logger = LoggerFactory.getLogger(RSAUtils.class);

	public static final String RSA_ALGORITHM = "RSA";
	private static final String RSA_PSS_ALGORITHM = "RSASSA-PSS";
	private static final PSSParameterSpec PSS_PARAMS = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
			32, 1);

	private static final SecureRandom SECURE_RANDOM = createSecureRandom();

	public static final int DEFAULT_RSA_KEY_SIZE = 3072;

	private static final int MIN_RSA_KEY_SIZE = 2048;
	private static final int MAX_SIGN_DATA_BYTES = 64 * 1024;

	/**
	 * Construtor privado para evitar instanciação
	 */
	private RSAUtils() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Gera um par de chaves RSA
	 * 
	 * @param keySizeBits tamanho da chave em bits (mínimo 2048, recomendado 3072 ou superior)
	 * @return par de chaves RSA ({@link KeyPair})
	 * @throws CryptoException se ocorrer falha na geração das chaves
	 */
	public static KeyPair generateKeyPair(int keySizeBits) {
		validateKeySize(keySizeBits);

		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
			kpg.initialize(keySizeBits, SECURE_RANDOM);
			return kpg.generateKeyPair();

		} catch (GeneralSecurityException e) {
			logger.error("Erro ao gerar par de chaves RSA ({} bits)", keySizeBits, e);
			throw new CryptoException(CryptoErrorType.KEY_INVALID, "Falha ao gerar par de chaves RSA", e);
		}
	}

	/**
	 * Assina dados utilizando RSA-PSS com SHA-256
	 * 
	 * @param data dados a serem assinados
	 * @param privateKey chave privada RSA
	 * @return assinatura de bytes
	 * @throws CryptoException se ocorrer falha na assinatura
	 */
	public static byte[] sign(byte[] data, PrivateKey privateKey) {
		Objects.requireNonNull(data, "data não pode ser nula");
		Objects.requireNonNull(privateKey, "privateKey não pode ser nula");

		validateRSAKey(privateKey);
		validateSignDataSize(data);

		try {
			Signature signature = Signature.getInstance(RSA_PSS_ALGORITHM);
			signature.setParameter(PSS_PARAMS);
			signature.initSign(privateKey, SECURE_RANDOM);
			signature.update(data);

			return signature.sign();

		} catch (GeneralSecurityException e) {
			logger.error("Falha na assinatura RSA-PSS", e);
			throw new CryptoException(CryptoErrorType.SIGNATURE_FAILED, "Falha ao assinar com RSA-PSS", e);
		}
	}

	/**
	 * Assina uma string utilizando RSA-PSS com SHA-256
	 * 
	 * @param data texto a ser assinado
	 * @param privateKey chave privada RSA
	 * @return assinatura de bytes
	 */
	public static byte[] sign(String data, PrivateKey privateKey) {
		Objects.requireNonNull(data, "data não pode ser nula");
		return sign(data.getBytes(StandardCharsets.UTF_8), privateKey);
	}

	/**
	 * Verifica uma assinatura RSA-PSS
	 * 
	 * @param data dados originais
	 * @param signatureBytes assinatura em bytes
	 * @param publicKey chave pública RSA
	 * @return ({@code true} se a assinatura for válida, {@code false} caso contrário
	 * @throws CryptoException se ocorrer falha técnica na verificação
	 */
	public static boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey) {
		Objects.requireNonNull(data, "data não pode ser nula");
		Objects.requireNonNull(signatureBytes, "signature não pode ser nula");
		Objects.requireNonNull(publicKey, "publicKey não pode ser nula");

		validateRSAKey(publicKey);

		try {
			Signature signature = Signature.getInstance(RSA_PSS_ALGORITHM);
			signature.setParameter(PSS_PARAMS);
			signature.initVerify(publicKey);
			signature.update(data);

			return signature.verify(signatureBytes);
		} catch (GeneralSecurityException e) {
			logger.error("Erro técnico na verificação RSA-PSS", e);
			throw new CryptoException(CryptoErrorType.SIGNATURE_FAILED, "Falha técnica ao verificar assinatura RSA-PSS",
					e);
		}
	}

	/**
	 * Verifica uma assinatura RSA-PSS de uma string.
	 * 
	 * @param data texto original
	 * @param signature assinatura em bytes
	 * @param publicKey cahve pública RSA
	 * @return {@code true} se a assinatura for válida, {@code false} caso contrário
	 */
	public static boolean verify(String data, byte[] signature, PublicKey publicKey) {
		Objects.requireNonNull(data, "data não pode ser nula");
		return verify(data.getBytes(StandardCharsets.UTF_8), signature, publicKey);
	}

	/**
	 * Valida se o tamanho da chave RSA é suportado.
	 * 
	 * @param keySizeBits tamanho da chave em bits
	 * @throws IllegalArgumentException se o tamanho for menor que 2048 bits
	 */
	private static void validateKeySize(int keySizeBits) {
		if (keySizeBits < MIN_RSA_KEY_SIZE) {
			throw new IllegalArgumentException("Tamanho mínimo de chave RSA é " + MIN_RSA_KEY_SIZE + " bits");
		}
	}

	/**
	 * Valida se os dados não excedem o tamanho máximo permitido para assinatura.
	 * 
	 * @param data dados a serem assinados
	 * @throws CryptoException se os dados forem muito grandes
	 */
	private static void validateSignDataSize(byte[] data) {
		if (data.length > MAX_SIGN_DATA_BYTES) {
			throw new CryptoException(CryptoErrorType.INTERNAL_ERROR,
					"Dados muito grandes para assinatura RSA (" + data.length + " bytes)");
		}
	}

	/**
	 * Valida se a chave fornecida é RSA.
	 * 
	 * @param key chave a ser validada
	 * @throws IllegalArgumentException se a chave não for RSA
	 */
	private static void validateRSAKey(Key key) {
		if (!"RSA".equalsIgnoreCase(key.getAlgorithm())) {
			throw new IllegalArgumentException(
					"Chave inválida: algoritmo deve ser RSA, mais encontrado: " + key.getAlgorithm());
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
