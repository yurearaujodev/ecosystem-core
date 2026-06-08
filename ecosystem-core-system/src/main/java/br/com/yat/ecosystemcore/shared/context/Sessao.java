package br.com.yat.ecosystemcore.shared.context;

import br.com.yat.ecosystemcore.shared.security.SessionRepository;

public final class Sessao {

	private static SessionRepository sessionRepository;

	private Sessao() {
	}

	public static void init(SessionRepository repo) {
		sessionRepository = repo;
	}

	public static boolean isActive() {
		return SessionScope.isActive();
	}

	public static String sessionId() {
		SessionContext ctx = SessionScope.get();
		return ctx != null ? ctx.getSessionId() : null;
	}

	public static Long usuarioId() {
		SessionContext ctx = SessionScope.get();
		return ctx != null ? ctx.getUsuarioId() : null;
	}

	public static String tenantId() {
		SessionContext ctx = SessionScope.get();
		return ctx != null ? ctx.getTenantId() : null;
	}

	public static Long empresaId() {
		SessionContext ctx = SessionScope.get();
		return ctx != null ? ctx.getEmpresaAtivaId() : null;
	}

	public static UserContext user() {
		SessionContext ctx = SessionScope.get();
		return ctx != null ? ctx.getUserContext() : null;
	}

	public static void logout() {
		SessionContext ctx = SessionScope.get();
		if (ctx != null && sessionRepository != null) {
			sessionRepository.revokeSession(ctx.getSessionId());
		}
		SessionScope.close();
	}

	public static boolean hasPermission(String chavePermissao) {
		UserContext uc = user();
		return uc != null && uc.temPermissao(chavePermissao);
	}
}