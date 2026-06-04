package br.com.yat.ecosystemcore.application.tenant;

import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.perfil.PerfilRepository;
import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
import br.com.yat.ecosystemcore.repository.usuario.PessoaRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;

public final class OnboardingTenantUseCaseFactory {

    private OnboardingTenantUseCaseFactory() {
    }

    public static OnboardingTenantUseCase create() {
        return new OnboardingTenantUseCase(
                new TenantRepository(),
                new EmpresaRepository(),
                new PessoaRepository(),
                new UsuarioRepository(),
                new PerfilRepository(),
                new EmpresaUsuarioRepository(),
                new BCryptPasswordEncoder() // Mantém a implementação concreta do encoder
        );
    }
}