package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Usuario;

public final class SessionScope {
    private static SessionContext current;
    private static SessionValidator validator;

    private SessionScope() {}

    public static void init(SessionValidator sessionValidator) {
        validator = sessionValidator;
    }

    public static synchronized void open(SessionContext session) {
        current = session;
    }

    public static synchronized void close() {
        current = null;
    }

    public static synchronized SessionContext get() {
        if (current == null) return null;

        if (validator != null && !validator.isSessionValid(current.getSessionId())) {
            current = null;
            return null;
        }

        if (current.isExpired()) {
            current = null;
            return null;
        }

        return current;
    }

    public static synchronized boolean isActive() {
        return get() != null;
    }

    public static Usuario usuario() {
        SessionContext ctx = get();
        return ctx != null ? ctx.getUsuario() : null;
    }

    public static Tenant tenant() {
        SessionContext ctx = get();
        return ctx != null ? ctx.getTenant() : null;
    }

    public static Empresa empresa() {
        SessionContext ctx = get();
        return ctx != null ? ctx.getEmpresaAtiva() : null;
    }

    public static synchronized void trocarEmpresa(Empresa novaEmpresa) {
        SessionContext ctx = get();
        if (ctx == null) return;

        if (validator != null) {
            boolean permitido = validator.usuarioPodeAcessarEmpresa(
                    ctx.getUsuario().getId(),
                    novaEmpresa.getId()
            );

            if (!permitido) {
                throw new SecurityException("Usuário não possui acesso a essa empresa");
            }
        }

        current = new SessionContext(
                ctx.getUsuario(),
                ctx.getTenant(),
                novaEmpresa,
                ctx.getSessionId(),
                ctx.getExpiresAt(),
                ctx.getRevokedAt(),
                ctx.getRefreshToken()
        );
    }
}
