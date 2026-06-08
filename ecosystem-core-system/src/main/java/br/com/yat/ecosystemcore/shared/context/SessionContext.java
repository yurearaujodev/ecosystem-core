package br.com.yat.ecosystemcore.shared.context;

import java.time.LocalDateTime;

import br.com.yat.ecosystemcore.shared.security.UserContextProvider;

public class SessionContext {

	private final Long usuarioId;
	private final String tenantId;
	private Long empresaAtivaId;

	private final String sessionId;
	private final String refreshToken;

	private final LocalDateTime expiresAt;
	private final LocalDateTime revokedAt;

	private final UserContext userContext;

	public SessionContext(Long usuarioId, String tenantId, Long empresaAtivaId, String sessionId,
			LocalDateTime expiresAt, LocalDateTime revokedAt, String refreshToken, UserContext userContext) {
		this.usuarioId = usuarioId;
		this.tenantId = tenantId;
		this.empresaAtivaId = empresaAtivaId;
		this.sessionId = sessionId;
		this.expiresAt = expiresAt;
		this.revokedAt = revokedAt;
		this.refreshToken = refreshToken;
		this.userContext = userContext;
	}

	public boolean isExpired() {
		return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isValid() {
		return !isExpired() && !isRevoked();
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public Long getEmpresaAtivaId() {
		return empresaAtivaId;
	}

	public void setEmpresaAtivaId(Long empresaAtivaId) {
		this.empresaAtivaId = empresaAtivaId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}
	
	public UserContext getUserContext() {
	    UserContext ctx = (userContext != null) ? userContext : UserContextProvider.get(usuarioId, tenantId, empresaAtivaId);

	    if (ctx == null) {
	        throw new IllegalStateException("UserContext não disponível para usuário " + usuarioId);
	    }

	    return ctx;
	}
}