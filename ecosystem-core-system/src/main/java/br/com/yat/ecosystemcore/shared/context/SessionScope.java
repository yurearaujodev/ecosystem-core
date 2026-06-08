package br.com.yat.ecosystemcore.shared.context;

import br.com.yat.ecosystemcore.shared.security.SessionValidator;
import br.com.yat.ecosystemcore.shared.security.UserContextProvider;

public final class SessionScope {

	private static final ThreadLocal<SessionContext> CURRENT = new ThreadLocal<>();
	private static SessionValidator validator;

	private SessionScope() {
	}

	public static void init(SessionValidator sessionValidator) {
		validator = sessionValidator;
	}

	public static void open(SessionContext session) {
		if (session == null) {
			throw new IllegalStateException("SessionContext não pode ser null");
		}
		CURRENT.set(session);
	}

	public static void close() {
		CURRENT.remove();
	}

	public static SessionContext get() {
		SessionContext ctx = CURRENT.get();
		if (ctx == null)
			return null;

		if (ctx.isExpired() || ctx.isRevoked()) {
			CURRENT.remove();
			return null;
		}

		if (validator != null && !validator.isSessionValid(ctx.getSessionId())) {
			CURRENT.remove();
			return null;
		}

		return ctx;
	}

	public static SessionContext getRaw() {
		return CURRENT.get();
	}

	public static boolean isActive() {
		return get() != null;
	}

	public static void setEmpresa(Long empresaId) {
		SessionContext ctx = get();
		if (ctx == null) {
			throw new SecurityException("Sessão inválida");
		}

		if (validator != null && !validator.usuarioPodeAcessarEmpresa(ctx.getUsuarioId(), empresaId)) {
			throw new SecurityException("Acesso negado à empresa");
		}

		ctx.setEmpresaAtivaId(empresaId);

		UserContextProvider.invalidate(ctx.getTenantId(), ctx.getUsuarioId(), empresaId);
	}

	public static Long usuarioId() {
		SessionContext ctx = get();
		return ctx != null ? ctx.getUsuarioId() : null;
	}

	public static String tenantId() {
		SessionContext ctx = get();
		return ctx != null ? ctx.getTenantId() : null;
	}

	public static Long empresaId() {
		SessionContext ctx = get();
		return ctx != null ? ctx.getEmpresaAtivaId() : null;
	}
}