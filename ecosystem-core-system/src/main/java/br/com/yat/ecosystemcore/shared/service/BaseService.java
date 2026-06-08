package br.com.yat.ecosystemcore.shared.service;

import br.com.yat.ecosystemcore.shared.context.Sessao;

public abstract class BaseService {

	protected void requireSession() {
		if (!Sessao.isActive()) {
			throw new IllegalStateException("Sessão inválida");
		}
	}

	protected void requirePermission(String permissaoChave) {
		requireSession();
		if (!Sessao.hasPermission(permissaoChave)) {
			throw new SecurityException("Acesso negado: Perfil sem permissão para " + permissaoChave);
		}
	}

	protected String tenant() {
		return Sessao.tenantId();
	}

	protected Long userId() {
		return Sessao.usuarioId();
	}

	protected Long empresaId() {
		return Sessao.empresaId();
	}
}
