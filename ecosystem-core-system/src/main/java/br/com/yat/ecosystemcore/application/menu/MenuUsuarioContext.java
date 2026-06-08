package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.shared.context.SessionContext;
import br.com.yat.ecosystemcore.shared.context.SessionScope;

/**
 * Contexto mínimo do usuário para filtragem futura de menus via
 * {@code permissao_menu}.
 */
public record MenuUsuarioContext(String tenantId, Long usuarioId) {

	public boolean valido() {
		return tenantId != null && usuarioId != null;
	}

	public static MenuUsuarioContext fromSession() {

		SessionContext ctx = SessionScope.get();

		if (ctx == null) {
			return null;
		}

		return new MenuUsuarioContext(ctx.getTenantId(), ctx.getUsuarioId());
	}
}