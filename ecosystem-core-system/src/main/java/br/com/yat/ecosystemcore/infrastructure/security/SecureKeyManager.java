package br.com.yat.ecosystemcore.infrastructure.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;

/**
 * Classe utilitária para gerenciamento seguro de chaves criptográficas associadas a documentos (CPF/CNPJ).
 * <p>
 * Responsável por:
 * <ul>
 * <li>Gerar e salvar chave mestre AES e par de chave RSA.</li>
 * <li>Persistir chaves em formato PEM (pública e privada criptografada).</li>
 * <li>Carregar e validar chaves existentes.</li>
 * </ul>
 * </p>
 * 
 * <p>Integra:
 * <ul>
 * <li>{@link KeyManager} para geração e carregamento de chaves.</li>
 * <li>{@link AESUtils} para criptografia da chave privada RSA.</li>
 * <li>{@link RSAUtils} para assinatura/verificação.</li>
 * <li>{@link FileManager} para persitência em arquivos.</li>
 * <li>{@link SensitiveData} para limpeza segura de dados sensíveis.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class SecureKeyManager {

	private static final Logger logger = LoggerFactory.getLogger(SecureKeyManager.class);
	private static final Path LICENCA_DIR = Paths.get("config", "license");

	private static final String MASTER_KEY_FILE = "master.key";
	private static final String RSA_PUBLIC_FILE = "rsa_public.pem";
	private static final String RSA_PRIVATE_FILE = "rsa_private.pem";

	private static final SecureRandom SECURE_RANDOM = createSecureRandom();

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private SecureKeyManager() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Gera e salva chaves AES e RSA associadas a um documento.
	 * Se já existirem chaves válidas, carrega as existentes.
	 * 
	 * @param document CPF (11 dígitos) ou CNPJ (14 dígitos)
	 * @return par de chaves RSA
	 * @throws CryptoException se ocorrer falha ao gerar ou salvar chaves
	 */
	public static KeyPair generateAndSaveKeys(String document) {
		String cleanDoc = normalizeDocument(document);
		Path dir = LICENCA_DIR.resolve(cleanDoc);
		ensureDirectory(dir);

		if (isKeyPairValidInternal(cleanDoc, dir)) {
			logger.debug("Chaves válidas já existem para: {}, carregando...", cleanDoc);
			return loadKeys(document);
		}
		logger.warn("Chaves inválidas ou incompletas para: {}. Regenerando todas as chaves.", cleanDoc);
		cleanupCorruptedFiles(dir);

		return generateAndSaveKeysInternal(cleanDoc, dir);
	}

	/**
	 * Carrega chaves AES e RSA associadas a um documento.
	 * 
	 * @param document CPF e CNPJ
	 * @return par de chaves RSA
	 * @throws CryptoException se as chaves não forem encontradas ou forem inválidas
	 */
	public static KeyPair loadKeys(String document) {
		String cleanDoc = normalizeDocument(document);
		Path dir = LICENCA_DIR.resolve(cleanDoc);

		if (!hasCompleteKeyPair(dir)) {
			throw new CryptoException(CryptoErrorType.KEY_NOT_FOUND, "Chaves incompletas para: " + cleanDoc);
		}

		SecretKey masterKey = KeyManager.loadAES(dir.resolve(MASTER_KEY_FILE));

		PublicKey pubKey = loadPublicKey(dir);
		PrivateKey privKey = loadPrivateKey(dir, masterKey);

		return new KeyPair(pubKey, privKey);
	}

	/**
	 * Verifica se o par de chaves associado ao documento é válido.
	 * 
	 * @param document CPF e CNPJ
	 * @return {@code true} se válido, {@code false} caso contrário
	 */
	public static boolean isKeyPairValid(String document) {
		String cleanDoc = normalizeDocument(document);
		return isKeyPairValidInternal(cleanDoc, LICENCA_DIR.resolve(cleanDoc));
	}

	/**
	 * Gera e salva internamente chave mestre AES e par RSA.
	 * 
	 * @param cleanDoc documento normalizado (somete dígitos)
	 * @param dir diretório de armazenamento
	 * @return par de chaves RSA
	 */
	private static KeyPair generateAndSaveKeysInternal(String cleanDoc, Path dir) {
		SecretKey masterKey = KeyManager.generateAndSaveAES(dir.resolve(MASTER_KEY_FILE));
		KeyPair rsaPair = KeyManager.generateRSA(RSAUtils.DEFAULT_RSA_KEY_SIZE);

		byte[] privBytes = rsaPair.getPrivate().getEncoded();
		byte[] encryptedPriv = AESUtils.encrypt(privBytes, masterKey);

		try {
			FileManager.saveText(dir.resolve(RSA_PRIVATE_FILE),
					wrapPem("ENCRYPTED PRIVATE KEY", Base64.getEncoder().encodeToString(encryptedPriv)));

			FileManager.saveText(dir.resolve(RSA_PUBLIC_FILE),
					wrapPem("PUBLIC KEY", Base64.getEncoder().encodeToString(rsaPair.getPublic().getEncoded())));

			logger.info("Chaves geradas com sucesso para: {}", cleanDoc);
			return rsaPair;
		} finally {
			SensitiveData.safeClear(privBytes);
			SensitiveData.safeClear(encryptedPriv);
		}
	}

	/**
	 * Carrega chave pública RSA de arquivo PEM.
	 * 
	 * @param dir diretório de armazenamento
	 * @return chave pública RSA
	 * @throws CryptoException se ocorrer falha ao carregar
	 */
	private static PublicKey loadPublicKey(Path dir) {
		try {
			String pubPem = FileManager.loadText(dir.resolve(RSA_PUBLIC_FILE));
			byte[] pubDer = Base64.getDecoder().decode(extractPem(pubPem, "PUBLIC KEY"));
			return KeyFactory.getInstance(RSAUtils.RSA_ALGORITHM).generatePublic(new X509EncodedKeySpec(pubDer));
		} catch (Exception e) {
			throw new CryptoException(CryptoErrorType.KEY_INVALID, "Falha ao carregar chave pública", e);
		}
	}

	/**
	 * Carrega chave privada RSA criptografada, descriptografando com chave mestre AES.
	 * 
	 * @param dir diretório de armazenamento
	 * @param masterKey chave AES mestre
	 * @return chave privada RSA
	 * @throws CryptoException se ocorrer falha ao carregar
	 */
	private static PrivateKey loadPrivateKey(Path dir, SecretKey masterKey) {
		byte[] decrypted = null;
		try {
			String privPem = FileManager.loadText(dir.resolve(RSA_PRIVATE_FILE));
			byte[] encrypted = Base64.getDecoder().decode(extractPem(privPem, "ENCRYPTED PRIVATE KEY"));
			decrypted = AESUtils.decrypt(encrypted, masterKey);
			return KeyFactory.getInstance(RSAUtils.RSA_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(decrypted));
		} catch (Exception e) {
			throw new CryptoException(CryptoErrorType.KEY_INVALID, "Falha ao carregar chave privada", e);
		} finally {
			if (decrypted != null) {
				SensitiveData.safeClear(decrypted);
			}
		}
	}

	/**
	 * Verifica se o par de chaves é válido internamente.
	 * 
	 * @param cleanDoc documento normalizado
	 * @param dir diretório de armazenamento
	 * @return {@code true} se válido, {@code false} caso contrário
	 */
	private static boolean isKeyPairValidInternal(String cleanDoc, Path dir) {
		try {
			if (!hasCompleteKeyPair(dir)) {
				return false;
			}

			SecretKey masterKey = KeyManager.loadAES(dir.resolve(MASTER_KEY_FILE));

			PublicKey pubKey = loadPublicKey(dir);
			PrivateKey privKey = loadPrivateKey(dir, masterKey);

			return areKeysConsistent(pubKey, privKey);

		} catch (Exception e) {
			logger.warn("Chaves CORROMPIDAS para: {}: {}", cleanDoc, e.getMessage());
			return false;
		}
	}

	/**
	 * Verifica consistência entre chave pública e privada RSA.
	 * 
	 * @param pubKey chave pública
	 * @param privKey chave privada
	 * @return {@code true} se consistente,{@code false} caso contrário
	 */
	private static boolean areKeysConsistent(PublicKey pubKey, PrivateKey privKey) {
		try {
			byte[] testData = new byte[32];
			SECURE_RANDOM.nextBytes(testData);
			byte[] signature = RSAUtils.sign(testData, privKey);
			return RSAUtils.verify(testData, signature, pubKey);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Verifica se existem todos os arquivos de chave necessários.
	 * 
	 * @param dir diretório de armazenamento
	 * @return {@code true} se completo, {@code false} caso contrário
	 */
	private static boolean hasCompleteKeyPair(Path dir) {
		return Files.exists(dir.resolve(MASTER_KEY_FILE)) && Files.exists(dir.resolve(RSA_PUBLIC_FILE))
				&& Files.exists(dir.resolve(RSA_PRIVATE_FILE));
	}

	/**
	 * Remove arquivos de chaves corrompidas.
	 * 
	 * @param dir diretório de armazenamento
	 * @throws CryptoException se ocorrer falha na limpeza
	 */
	private static void cleanupCorruptedFiles(Path dir) {
		try {
			Files.deleteIfExists(dir.resolve(MASTER_KEY_FILE));
			Files.deleteIfExists(dir.resolve(RSA_PUBLIC_FILE));
			Files.deleteIfExists(dir.resolve(RSA_PRIVATE_FILE));

		} catch (Exception e) {
			throw new CryptoException(CryptoErrorType.DATA_CORRUPTED, "Falha ao limpar chaves corrompidas", e);
		}
	}

	/**
	 * Garante que o diretório existe e possui permissão de escrita.
	 * 
	 * @param dir diretório de armazenamento
	 * @throws CryptoException se não houber permissão ou falha na criação
	 */
	private static void ensureDirectory(Path dir) {
		try {
			Files.createDirectories(dir);
			if (!Files.isWritable(dir)) {
				throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Sem permissão de escrita em: " + dir);
			}

		} catch (Exception e) {
			throw new CryptoException(CryptoErrorType.CONFIG_ERROR, "Falha ao preparar diretório de licença", e);
		}
	}

	/**
	 * Normaliza documento (CPF ou CNPJ), mantendo apensas dígitos.
	 * 
	 * @param document documento original
	 * @return documento normalizado
	 * @throws CryptoException se o documento for inválido
	 */
	private static String normalizeDocument(String document) {
		Objects.requireNonNull(document, "Documento não pode ser nulo");

		String clean = document.replaceAll("\\D", "");
		if (clean.matches("\\d{11}") || clean.matches("\\d{14}"))
			return clean;
		throw new CryptoException(CryptoErrorType.KEY_INVALID,"Documento inválido: use CPF (11 dígitos) ou CNPJ(14 dígitos)");
	}

	/**
	 * Constrói conteúdo PEM com cabeçalho, rodapé e base64.
	 * 
	 * @param type tipo da chave (ex.: PUBLIC KEY)
	 * @param base64 conteúdo codificado
	 * @return string PEM formatada
	 */
	private static String wrapPem(String type, String base64) {
		StringBuilder sb = new StringBuilder();
		sb.append("-----BEGIN ").append(type).append("-----\n");

		for (int i = 0; i < base64.length(); i += 64) {
			sb.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
		}

		sb.append("-----END ").append(type).append("-----\n");
		return sb.toString();

	}

	/**
	 * Extrai conteúdo base64 de um PEM.
	 * 
	 * @param pem conteúdo PEM
	 * @param type tipo da chave
	 * @return conteúdo base64 extraído
	 * @throws CryptoException se o PEM or inválido ou corrompido
	 */
	private static String extractPem(String pem, String type) {
		String header = "-----BEGIN " + type + "-----";
		String footer = "-----END " + type + "-----";

		int start = pem.indexOf(header);
		int end = pem.indexOf(footer);

		if (start < 0 || end <= start) {
			throw new CryptoException(CryptoErrorType.DATA_CORRUPTED, "PEM inválido ou corrompido: " + type);
		}

		return pem.substring(start + header.length(), end).replaceAll("\\s", "").trim();
	}

	/**
	 * Cria instância segura de {@link SecureRandom}
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
