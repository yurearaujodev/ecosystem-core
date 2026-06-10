package br.com.yat.ecosystemcore.infra;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.shared.exception.CryptoException;
import br.com.yat.ecosystemcore.shared.security.DatabasePasswordDecryptor;

@DisplayName("Unidade - DatabasePasswordDecryptor")
class DatabasePasswordDecryptorTest {

    @Test
    @DisplayName("Deve disparar NullPointerException se tentar descriptografar senha nula")
    void deveValidarSenhaNula() {
        assertThrows(NullPointerException.class, () -> {
            DatabasePasswordDecryptor.decryptPassword(null);
        });
    }

    @Test
    @DisplayName("Deve barrar senhas gigantescas que tentem estourar buffer ou memória")
    void deveRejeitarSenhaAcimaDoTamanhoMaximo() {
        String senhaGigante = "A".repeat(4097);
        
        assertThrows(CryptoException.class, () -> {
            DatabasePasswordDecryptor.decryptPassword(senhaGigante);
        });
    }
}
