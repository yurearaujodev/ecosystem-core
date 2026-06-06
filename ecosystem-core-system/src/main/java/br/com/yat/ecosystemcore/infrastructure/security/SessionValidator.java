package br.com.yat.ecosystemcore.infrastructure.security;

public interface SessionValidator {
    boolean isSessionValid(String sessionId);
    boolean usuarioPodeAcessarEmpresa(Long usuarioId, Long empresaId);
}