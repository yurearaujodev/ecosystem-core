package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.infrastructure.database.HikariConfigBuilder;
import br.com.yat.ecosystemcore.infrastructure.security.CryptoException;

@DisplayName("Unidade - HikariConfigBuilder")
class HikariConfigBuilderTest {

    @Test
    @DisplayName("Deve rejeitar mapa de propriedades nulo")
    void deveValidarPropertiesNulo() {
        assertThrows(NullPointerException.class, () -> {
            HikariConfigBuilder.buildConfig(null);
        });
    }

    @Test
    @DisplayName("Deve lançar CryptoException se as propriedades mínimas do banco estiverem ausentes")
    void deveLancarErroFaltaCamposObrigatorios() {
        Properties props = new Properties();
        props.setProperty("db.url", "link-banco");
        // Faltando user, password e driver

        assertThrows(CryptoException.class, () -> {
            HikariConfigBuilder.buildConfig(props);
        });
    }
}
