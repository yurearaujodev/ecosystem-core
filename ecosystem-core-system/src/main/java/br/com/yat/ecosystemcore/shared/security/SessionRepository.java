package br.com.yat.ecosystemcore.shared.security;

public interface SessionRepository {
    void revokeSession(String sessionId);
}