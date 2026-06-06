package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.security.SessionScope;

/**
 * Contexto mínimo do usuário para filtragem futura de menus via {@code permissao_menu}.
 */
public record MenuUsuarioContext(
        String tenantId,
        Long usuarioId
) {

    public static MenuUsuarioContext fromSession() {
        // 🔒 ATUALIZADO: Agora usa o SessionScope estável da nova arquitetura
        if (!SessionScope.isActive() || SessionScope.usuario() == null) {
            // Retorna zerado temporariamente para evitar quebras se a tela for inicializada precocemente
            return new MenuUsuarioContext(null, null);
        }
        
        Usuario usuario = SessionScope.usuario();
        String tenantId = SessionScope.tenant() != null ? SessionScope.tenant().getId() : usuario.getTenantId();
        
        return new MenuUsuarioContext(tenantId, usuario.getId());
    }
}