package br.com.yat.ecosystemcore.modules.navegacao.service;

import br.com.yat.ecosystemcore.modules.navegacao.entity.MenuChave;

public interface Navigator {
    void navigatePara(MenuChave chave);

    void voltar();
}