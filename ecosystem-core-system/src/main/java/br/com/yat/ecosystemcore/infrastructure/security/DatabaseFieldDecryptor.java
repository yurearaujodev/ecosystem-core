package br.com.yat.ecosystemcore.infrastructure.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;


public final class DatabaseFieldDecryptor {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFieldDecryptor.class);
    private static final Path CONFIG_DIR = Paths.get("config", "database");
    private static final Path GLOBAL_MASTER_KEY_FILE = CONFIG_DIR.resolve("master.key");

    private DatabaseFieldDecryptor() {
        throw new AssertionError("Classe Utilitária não deve ser instanciada");
    }
       
    public static String decryptToString(String encryptedB64, String fieldName) {
        Objects.requireNonNull(encryptedB64, fieldName + " criptografado não pode ser nulo");

        if (encryptedB64.isBlank()) {
            throw new CryptoException(CryptoErrorType.CONFIG_ERROR, fieldName + " do banco não configurado");
        }

        validateMasterKeyFile();

        byte[] encrypted = null;
        try {
            SecretKey masterKey = KeyManager.loadAES(GLOBAL_MASTER_KEY_FILE);
            encrypted = Base64.getDecoder().decode(encryptedB64.trim());
            String value = AESUtils.decryptToString(encrypted, masterKey);

            logger.debug("{} do banco descriptografado com sucesso", fieldName);
            return value;
        } catch (Exception e) {
            logger.error("Falha ao descriptografar {} do banco", fieldName, e);
            throw new CryptoException(CryptoErrorType.DECRYPTION_FAILED,
                    "Falha ao descriptografar " + fieldName + " do banco", e);
        } finally {
            SensitiveData.safeClear(encrypted);
        }
    }

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
