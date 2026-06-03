package br.com.yat.ecosystemcore.application.usuario;

public final class UseCaseFactory {

    private UseCaseFactory() {}

    public static AutenticacaoUseCase autenticacao() {
        return new AutenticacaoUseCase(new BCryptPasswordEncoder());
    }
}