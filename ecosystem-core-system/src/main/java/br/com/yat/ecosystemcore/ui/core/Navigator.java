package br.com.yat.ecosystemcore.ui.core;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;

public interface Navigator {
    void navigatePara(MenuChave chave);

    default void voltar() {
        // futuro: stack de navegação
    }
}