package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.domain.entity.DatabaseCredentials;

@DisplayName("Unidade - DatabaseCredentials")
class DatabaseCredentialsTest {

    @Test
    @DisplayName("Deve limpar o array original recebido externamente imediatamente no construtor")
    void deveApagarArrayOriginal() {
        char[] senhaOriginal = {'s', 'e', 'n', 'h', 'a', '1'};
        
        // O construtor clona internamente e limpa o array de entrada
        DatabaseCredentials credentials = new DatabaseCredentials(senhaOriginal);

        assertEquals('\0', senhaOriginal[0]);
        assertEquals('\0', senhaOriginal[1]);
        credentials.close();
    }

    @Test
    @DisplayName("Deve estourar IllegalStateException se tentar ler credencial fechada/destruída")
    void deveImpedirUsoAposClose() {
        char[] senha = {'1', '2', '3'};
        DatabaseCredentials credentials = new DatabaseCredentials(senha);
        
        credentials.close(); // Destrói a credencial

        assertThrows(IllegalStateException.class, () -> 
            credentials.executarComSenha(senhaAtiva -> { /* noop */ })
        );
    }
}
