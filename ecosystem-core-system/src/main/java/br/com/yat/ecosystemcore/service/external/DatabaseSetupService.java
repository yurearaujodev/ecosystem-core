package br.com.yat.ecosystemcore.service.external;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.shared.database.DatabaseConfig;
import br.com.yat.ecosystemcore.shared.database.DatabaseCredentials;
import br.com.yat.ecosystemcore.shared.exception.CryptoException;
import br.com.yat.ecosystemcore.shared.security.AESUtils;
import br.com.yat.ecosystemcore.shared.security.KeyManager;
import br.com.yat.ecosystemcore.shared.security.SensitiveData;
import br.com.yat.ecosystemcore.shared.util.DatabaseConfigLoader;
import br.com.yat.ecosystemcore.shared.util.FileManager;

public class DatabaseSetupService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetupService.class);
    private static final Path CONFIG_DIR = Paths.get("config", "database");
    private static final Path DB_CONFIG_FILE = CONFIG_DIR.resolve("db.properties");
    private static final Path GLOBAL_MASTER_KEY_FILE = CONFIG_DIR.resolve("master.key");

    public void saveConfiguration(DatabaseConfig config, DatabaseCredentials credentials) {
        try {
            Files.createDirectories(CONFIG_DIR);
            SecretKey masterKey = loadOrCreateMasterKey();

            credentials.executarComSenha(senhaAtiva -> {
                byte[] encryptedPassword = AESUtils.encrypt(senhaAtiva, masterKey);
                
                // PROBLEMA 2 FIXED: Modificado de String literal para StandardCharsets.UTF_8
                byte[] encryptedUrl = AESUtils.encrypt(config.gerarJdbcUrl().getBytes(StandardCharsets.UTF_8), masterKey);
                byte[] encryptedUser = AESUtils.encrypt(config.usuario().getBytes(StandardCharsets.UTF_8), masterKey);
                    
                String encodedPassword = Base64.getEncoder().encodeToString(encryptedPassword);
                String encodedUrl = Base64.getEncoder().encodeToString(encryptedUrl);
                String encodedUser = Base64.getEncoder().encodeToString(encryptedUser);

                Properties props = buildProperties(encodedUrl, encodedUser, encodedPassword);
                DatabaseConfigLoader.validateRequiredProperties(props);

                FileManager.saveText(DB_CONFIG_FILE, toPropertiesString(props));
                
                SensitiveData.safeClear(encryptedPassword);
                SensitiveData.safeClear(encryptedUrl);
                SensitiveData.safeClear(encryptedUser);
            });

            logger.info("Configuração de banco de dados salva com sucesso.");
        } catch (Exception e) {
            logger.error("Erro crítico ao salvar configuração: {}", e.getMessage());
            throw new CryptoException("Erro técnico ao gravar configurações", e);
        }
    }
    
    public DatabaseConfig carregarConfiguracao() {
        if (!Files.exists(DB_CONFIG_FILE)) return null;

        try (BufferedReader reader = Files.newBufferedReader(DB_CONFIG_FILE, StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            SecretKey masterKey = loadOrCreateMasterKey();

            String rawUrl = AESUtils.decryptToString(Base64.getDecoder().decode(props.getProperty("db.url")), masterKey);
            String rawUser = AESUtils.decryptToString(Base64.getDecoder().decode(props.getProperty("db.user")), masterKey);

            return DatabaseConfig.reconstruirDoArquivo(rawUrl, rawUser);
        } catch (Exception e) {
            logger.error("Falha ao carregar configuração do banco", e);
            throw new CryptoException("Erro técnico ao ler configurações", e);
        }
    }

    public DatabaseCredentials carregarCredenciaisExistentes() {
        if (!Files.exists(DB_CONFIG_FILE)) return null;
        
        char[] passwordChars = null;
        try (BufferedReader reader = Files.newBufferedReader(DB_CONFIG_FILE, StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            SecretKey masterKey = loadOrCreateMasterKey();

            // PROBLEMA 2 FIXED: Usando decryptToChars para evitar String imutável na heap
            passwordChars = AESUtils.decryptToChars(Base64.getDecoder().decode(props.getProperty("db.password")), masterKey);
            
            DatabaseCredentials credentials = new DatabaseCredentials(passwordChars);
            return credentials;
        } catch (Exception e) {
            throw new CryptoException("Não foi possível carregar a credencial existente para reaproveitamento.", e);
        } finally {
            // Garante o overwrite imediato do array temporário usado na extração
            if (passwordChars != null) {
                SensitiveData.safeClear(passwordChars);
            }
        }
    }

    private static SecretKey loadOrCreateMasterKey() {
        return Files.exists(GLOBAL_MASTER_KEY_FILE) 
            ? KeyManager.loadAES(GLOBAL_MASTER_KEY_FILE) : KeyManager.generateAndSaveAES(GLOBAL_MASTER_KEY_FILE);
    }

    private static Properties buildProperties(String encodedUrl, String encodedUser, String encodedPassword) {
        Properties props = new Properties();
        props.setProperty("db.url", encodedUrl.trim());
        props.setProperty("db.user", encodedUser.trim());
        props.setProperty("db.password", encodedPassword.trim());
        props.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        return props;
    }

    private static String toPropertiesString(Properties props) {
        StringBuilder sb = new StringBuilder();
        sb.append("# GERADO AUTOMATICAMENTE\n");
        props.stringPropertyNames().stream().sorted()
                .forEach(key -> sb.append(key).append('=').append(props.getProperty(key)).append('\n'));
        return sb.toString();
    }
}

