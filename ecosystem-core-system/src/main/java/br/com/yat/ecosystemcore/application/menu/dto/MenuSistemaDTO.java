package br.com.yat.ecosystemcore.application.menu.dto;

/**
 * Projeção de {@code menu_sistema} para montagem do menu.
 */
public record MenuSistemaDTO(
        Long id,
        String uuidPublico,
        Long moduloId,
        Long menuPaiId,
        String nome,
        String acaoComando,
        int ordem,
        boolean ativo
) {
}
