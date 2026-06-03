package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;

/**
 * Contexto mínimo do usuário para filtragem futura de menus via {@code permissao_menu}.
 */
public record MenuUsuarioContext(
        String tenantId,
        Long usuarioId
) {

    public static MenuUsuarioContext fromSession() {
        Usuario usuario = SessionManager.getUsuarioLogado();
        if (usuario == null) {
            return new MenuUsuarioContext(null, null);
        }
        return new MenuUsuarioContext(usuario.getTenantId(), usuario.getId());
    }
}
