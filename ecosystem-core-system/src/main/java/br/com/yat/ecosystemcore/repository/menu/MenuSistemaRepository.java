package br.com.yat.ecosystemcore.repository.menu;

import br.com.yat.ecosystemcore.application.menu.MenuUsuarioContext;
import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de persistência para menu dinâmico.
 * <p>
 * Implementação JDBC futura deve consultar {@code modulo_sistema}, {@code menu_sistema} e
 * {@code permissao_menu}, respeitando o perfil/permissões do {@link MenuUsuarioContext}.
 * </p>
 */
public interface MenuSistemaRepository {

    List<MenuPermitidoDTO> listarMenusPermitidos(Connection conn, MenuUsuarioContext context) throws SQLException;
}
