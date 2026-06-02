package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.exception.DatabaseValidationException;

@DisplayName("Unidade - DatabaseConfig")
class DatabaseConfigTest {

    @Test
    @DisplayName("Deve criar configuração válida a partir do formulário")
    void deveCriarConfiguracaoValida() {
        DatabaseConfig config = DatabaseConfig.criarAPartirDeFormulario(
            "127.0.0.1", "3306", "yat_db", "root"
        );

        assertNotNull(config);
        assertEquals("127.0.0.1", config.ip());
        assertEquals("3306", config.porta());
        assertEquals("jdbc:mysql://127.0.0.1:3306/yat_db", config.gerarJdbcUrl());
    }

    @Test
    @DisplayName("Deve falhar se alguma propriedade obrigatória for nula")
    void deveLancarExceptionParaCamposNulos() {
        assertThrows(NullPointerException.class, () -> 
            DatabaseConfig.criarAPartirDeFormulario(null, "3306", "db", "root")
        );
    }

    @Test
    @DisplayName("Deve barrar porta fora do range permitido (1-65535)")
    void deveValidarRangeDaPorta() {
        assertThrows(DatabaseValidationException.class, () -> 
            DatabaseConfig.criarAPartirDeFormulario("localhost", "65536", "db", "root")
        );

        assertThrows(DatabaseValidationException.class, () -> 
            DatabaseConfig.criarAPartirDeFormulario("localhost", "-1", "db", "root")
        );
    }

    @Test
    @DisplayName("Deve reconstruir corretamente os dados lidos de uma URL JDBC limpa")
    void deveReconstruirDoArquivoComSucesso() {
        String urlDoArquivo = "jdbc:mysql://192.168.1.50:3306/producao_db";
        DatabaseConfig config = DatabaseConfig.reconstruirDoArquivo(urlDoArquivo, "admin");

        assertEquals("192.168.1.50", config.ip());
        assertEquals("3306", config.porta());
        assertEquals("producao_db", config.banco());
    }
}
