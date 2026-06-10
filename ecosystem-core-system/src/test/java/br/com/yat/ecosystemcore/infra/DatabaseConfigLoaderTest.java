package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.shared.exception.CryptoException;
import br.com.yat.ecosystemcore.shared.util.DatabaseConfigLoader;

@DisplayName("Unidade - DatabaseConfigLoader")
class DatabaseConfigLoaderTest {

    @Test
    @DisplayName("Deve aceitar e validar perfeitamente quando todas as propriedades requeridas existem")
    void deveValidarPropriedadesComSucesso() {
        Properties props = new Properties();
        props.setProperty("db.url", "any_url");
        props.setProperty("db.user", "any_user");
        props.setProperty("db.password", "any_password");
        props.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");

        assertDoesNotThrow(() -> DatabaseConfigLoader.validateRequiredProperties(props));
    }

    @Test
    @DisplayName("Deve lançar CryptoException se alguma propriedade obrigatória estiver ausente ou em branco")
    void deveLancarExceptionQuandoPropriedadeAusente() {
        Properties props = new Properties();
        props.setProperty("db.url", "any_url");
        props.setProperty("db.user", " "); // Em branco
        props.setProperty("db.password", "any_password");
        // db.driver ausente completamente

        assertThrows(CryptoException.class, () -> {
            DatabaseConfigLoader.validateRequiredProperties(props);
        });
    }

    @Test
    @DisplayName("Deve retornar o valor numérico correto ou usar o default se a propriedade for inválida")
    void deveRetornarPropriedadesNumericasCorretamente() {
        Properties props = new Properties();
        props.setProperty("db.poolSize", "10");
        props.setProperty("db.connectionTimeout", "invalid_number");

        int poolSize = DatabaseConfigLoader.getIntProperty(props, "db.poolSize", 5);
        long timeout = DatabaseConfigLoader.getLongProperty(props, "db.connectionTimeout", 30000L);
        int defaultVal = DatabaseConfigLoader.getIntProperty(props, "db.nonExistent", 99);

        assertEquals(10, poolSize);
        assertEquals(30000L, timeout); // Pegou o default por falha no parse
        assertEquals(99, defaultVal);  // Pegou o default porque não existe
    }
}
