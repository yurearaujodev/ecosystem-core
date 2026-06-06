package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Usuario;

public final class Sessao {
    private static SessionRepository sessionRepository;

    private Sessao() {}

    public static void init(SessionRepository repo) {
        sessionRepository = repo;
    }

    public static Usuario usuario() { return SessionScope.usuario(); }
    public static Tenant tenant() { return SessionScope.tenant(); }
    public static Empresa empresa() { return SessionScope.empresa(); }
    
    // 🛠️ CORRIGIDO: Sintaxe ajustada para fechar o método corretamente
    public static boolean isActive() { 
        return SessionScope.isActive(); 
    }
    
    public static String sessionId() {
        SessionContext ctx = SessionScope.get();
        return ctx != null ? ctx.getSessionId() : null;
    }

    public static void logout() {
        SessionContext ctx = SessionScope.get();
        if (ctx != null && sessionRepository != null) {
            sessionRepository.revokeSession(ctx.getSessionId());
        }
        SessionScope.close();
    }
}