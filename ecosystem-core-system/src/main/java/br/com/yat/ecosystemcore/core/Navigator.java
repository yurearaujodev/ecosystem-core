package br.com.yat.ecosystemcore.core;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;

public interface Navigator {
    void navigatePara(MenuChave chave);

    void voltar();
}