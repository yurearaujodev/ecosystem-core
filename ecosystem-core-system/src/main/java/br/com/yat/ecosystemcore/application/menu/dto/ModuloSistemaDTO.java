package br.com.yat.ecosystemcore.application.menu.dto;

/**
 * Projeção de {@code modulo_sistema} para montagem do menu.
 */
public record ModuloSistemaDTO(
        Long id,
        String uuidPublico,
        String nome,
        String icone,
        int ordem,
        boolean ativo
) {
}
