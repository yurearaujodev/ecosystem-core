package br.com.yat.ecosystemcore.ui.core;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;

public record NavigationEvent(MenuChave chave, String modulo, String nome) {
}
