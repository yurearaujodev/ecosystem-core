package br.com.yat.ecosystemcore.modules.navegacao.service;

import br.com.yat.ecosystemcore.modules.navegacao.entity.MenuChave;

public record NavigationEvent(MenuChave chave, String modulo, String nome) {
}
