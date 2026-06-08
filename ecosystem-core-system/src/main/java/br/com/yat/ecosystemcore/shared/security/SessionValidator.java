package br.com.yat.ecosystemcore.shared.security;

public interface SessionValidator {
    boolean isSessionValid(String sessionId);
    boolean usuarioPodeAcessarEmpresa(Long usuarioId, Long empresaId);
}