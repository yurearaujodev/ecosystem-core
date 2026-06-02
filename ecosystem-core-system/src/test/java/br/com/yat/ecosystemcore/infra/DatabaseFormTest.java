package br.com.yat.ecosystemcore.infra;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.domain.dto.DatabaseForm;

@DisplayName("Unidade - DatabaseForm")
class DatabaseFormTest {

    @Test
    @DisplayName("Deve reter exatamente os dados passados na criação da estrutura")
    void deveManterDadosImutaveis() {
        DatabaseForm form = new DatabaseForm("10.0.0.1", "3306", "sistema", "usr_yat");

        assertEquals("10.0.0.1", form.ip());
        assertEquals("3306", form.porta());
        assertEquals("sistema", form.banco());
        assertEquals("usr_yat", form.usuario());
    }
}
