package br.com.yat.ecosystemcore.application.menu.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.yat.ecosystemcore.modules.navegacao.entity.MenuChave;

/**
 * Visão agregada pronta para a UI (módulo + item de menu permitido).
 * {@code acaoComando} corresponde a {@code menu_sistema.acao_comando} (nome do {@link MenuChave}).
 */
public record MenuPermitidoDTO(
        String moduloNome,
        String moduloIcone,
        int moduloOrdem,
        Long menuSistemaId,
        String menuNome,
        String acaoComando,
        int menuOrdem
) {

    public Optional<MenuChave> resolverChaveNavegacao() {
        if (acaoComando == null || acaoComando.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(MenuChave.valueOf(acaoComando));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static List<MenuPermitidoDTO> ordenarParaExibicao(List<MenuPermitidoDTO> itens) {
        return itens.stream()
                .sorted(Comparator
                        .comparingInt(MenuPermitidoDTO::moduloOrdem)
                        .thenComparingInt(MenuPermitidoDTO::menuOrdem)
                        .thenComparing(MenuPermitidoDTO::menuNome))
                .toList();
    }
}
