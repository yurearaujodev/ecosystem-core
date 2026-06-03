package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;

import java.util.List;

/**
 * Ponto de extensão para carregamento de menus permitidos ao usuário.
 * <p>
 * Implementação atual: {@link MenuProviderStub} (dados fixos).
 * Implementação futura: JDBC via {@code modulo_sistema}, {@code menu_sistema} e
 * {@code permissao_menu}, delegando persistência a {@link br.com.yat.ecosystemcore.repository.menu.MenuSistemaRepository}.
 * </p>
 */
public interface MenuProvider {

    List<MenuPermitidoDTO> carregarMenus(MenuUsuarioContext context);
}
