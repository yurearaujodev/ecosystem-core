package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.shared.util.PasswordExtractor;


@DisplayName("Unidade - PasswordExtractor")
class PasswordExtractorTest {

    @Test
    @DisplayName("Deve disparar NullPointerException se receber um componente nulo")
    void deveLancarErroSeComponenteForNulo() {
        assertThrows(NullPointerException.class, () -> PasswordExtractor.extrair(null));
    }
}
