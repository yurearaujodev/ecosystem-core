package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;
import br.com.yat.ecosystemcore.shared.database.DatabaseStatus;


@DisplayName("Testes de Integração do Banco de Dados e Criptografia")
class DatabaseConnectionJUnitTest {

    @BeforeAll
    static void setUpBeforeClass() {
        System.out.println("🔄 Inicializando ambiente de testes do Banco de Dados...");
        // Garante que o pool tente carregar e validar o status no início da classe de teste
        ConnectionFactory.reloadAndCheck();
    }

    @AfterAll
    static void tearDownAfterClass() {
        System.out.println("🧹 Finalizando o pool de conexões (Shutdown Graceful)...");
        ConnectionFactory.shutdown();
    }

    @Test
    @DisplayName("Deve descriptografar credenciais e validar o status do pool como ativo")
    void deveRetornarStatusDisponivelEAtivo() {
        DatabaseStatus status = ConnectionFactory.reloadAndCheck();
        
        assertNotNull(status, "O status do banco não deveria ser nulo.");
        assertTrue(status.available(), "O banco de dados deveria estar disponível e ativo.");
        assertEquals("BANCO DE DADOS CONECTADO!", status.message());
    }

    @Test
    @DisplayName("Deve conseguir capturar uma conexão válida do pool e executar uma query no MySQL")
    void deveObterConexaoValidaEExecutarQuery() {
        String sql = "SELECT 1";
        
        // O bloco try-with-resources garante que a conexão e os statements sejam devolvidos ao pool
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            assertNotNull(conn, "A conexão obtida do pool não deve ser nula.");
            assertFalse(conn.isClosed(), "A conexão com o banco deve estar aberta.");
            
            assertTrue(rs.next(), "O ResultSet deve conter pelo menos uma linha.");
            int resultado = rs.getInt(1);
            
            assertEquals(1, resultado, "O retorno da query de teste 'SELECT 1' deveria ser igual a 1.");
            
        } catch (SQLException e) {
            fail("Falha ao interagir com o banco de dados via JDBC: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Deve falhar de forma segura ao tentar iniciar uma transação com uma conexão nula")
    void deveDispararExcecaoAoIniciarTransacaoComConexaoNula() {
        // Valida se o validador interno que você colocou na ConnectionFactory está funcionando
        assertThrows(NullPointerException.class, () -> {
            ConnectionFactory.beginTransaction(null);
        }, "Deveria lançar NullPointerException se a conexão for nula.");
    }
}
