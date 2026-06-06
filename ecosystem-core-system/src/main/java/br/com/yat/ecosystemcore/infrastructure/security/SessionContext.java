package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import java.time.LocalDateTime;

public class SessionContext {
    private final Usuario usuario;
    private final Tenant tenant;
    private final Empresa empresaAtiva;
    private final String sessionId;
    private final LocalDateTime expiresAt;
    private final LocalDateTime revokedAt;
    private final String refreshToken;

    public SessionContext(Usuario usuario, Tenant tenant, Empresa empresaAtiva, String sessionId, 
                          LocalDateTime expiresAt, LocalDateTime revokedAt, String refreshToken) {
        this.usuario = usuario;
        this.tenant = tenant;
        this.empresaAtiva = empresaAtiva;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.refreshToken = refreshToken;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isValid() { return !isExpired() && !isRevoked(); }
    public boolean temEmpresa() { return empresaAtiva != null; }

    public Usuario getUsuario() { return usuario; }
    public Tenant getTenant() { return tenant; }
    public Empresa getEmpresaAtiva() { return empresaAtiva; }
    public String getSessionId() { return sessionId; }
    public String getRefreshToken() { return refreshToken; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
}
