package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.infrastructure.database.DatabaseStatus;

@DisplayName("Unidade - DatabaseStatus")
class DatabaseStatusTest {

    @Test
    @DisplayName("Deve construir status OK com valores padrão de sucesso")
    void deveConstruirStatusOk() {
        DatabaseStatus status = DatabaseStatus.ok();
        
        assertTrue(status.available());
        assertEquals("BANCO DE DADOS CONECTADO!", status.message());
        assertEquals("POOL ATIVO E SAUDÁVEL", status.details());
    }

    @Test
    @DisplayName("Deve construir status de Erro com a mensagem customizada fornecida")
    void deveConstruirStatusErro() {
        String msgErro = "MySQL não está rodando.";
        DatabaseStatus status = DatabaseStatus.error(msgErro);
        
        assertFalse(status.available());
        assertEquals(msgErro, status.message());
        assertEquals("INDISPONÍVEL", status.details());
    }
}