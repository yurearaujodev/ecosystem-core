package br.com.yat.ecosystemcore.infrastructure.security;

public interface SessionRepository {
    void revokeSession(String sessionId);
}
