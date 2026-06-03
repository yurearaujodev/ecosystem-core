package br.com.yat.ecosystemcore.application.menu.dto;

/**
 * Projeção da tabela de vínculo {@code permissao_menu}.
 */
public record PermissaoMenuVinculoDTO(
        long permissaoId,
        long menuSistemaId
) {
}
