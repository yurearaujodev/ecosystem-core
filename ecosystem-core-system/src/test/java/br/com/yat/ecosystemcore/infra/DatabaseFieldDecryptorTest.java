package br.com.yat.ecosystemcore.infra;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.yat.ecosystemcore.infrastructure.security.CryptoException;
import br.com.yat.ecosystemcore.infrastructure.security.DatabaseFieldDecryptor;

@DisplayName("Unidade - DatabaseFieldDecryptor")
class DatabaseFieldDecryptorTest {

    @Test
    @DisplayName("Deve lançar NullPointerException se o valor criptografado recebido for nulo")
    void deveValidarParametrosNulos() {
        assertThrows(NullPointerException.class, () -> {
            DatabaseFieldDecryptor.decryptToString(null, "User");
        });
    }

    @Test
    @DisplayName("Deve lançar CryptoException se a string base64 enviada estiver vazia")
    void deveLancarErroSeEstiverEmBranco() {
        assertThrows(CryptoException.class, () -> {
            DatabaseFieldDecryptor.decryptToString("   ", "URL");
        });
    }

    @Test
    @DisplayName("Deve lançar CryptoException por falta de chave mestra se rodar sem o arquivo na pasta correta")
    void deveLancarErroSeNaoAcharChaveMestra() {
        // Se a chave não existir no caminho físico, deve estourar KEY_NOT_FOUND ou CONFIG_ERROR
        assertThrows(CryptoException.class, () -> {
            DatabaseFieldDecryptor.decryptToString("bXktZW5jcnlwdGVkLXN0cmluZw==", "User");
        });
    }
}
