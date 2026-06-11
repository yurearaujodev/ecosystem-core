package br.com.yat.ecosystemcore.application.tenant;

import br.com.yat.ecosystemcore.modules.cadastro.repository.PessoaRepository;
import br.com.yat.ecosystemcore.modules.seguranca.repository.PerfilRepository;
import br.com.yat.ecosystemcore.modules.tenant.repository.TenantRepository;
import br.com.yat.ecosystemcore.modules.cadastro.repository.EmpresaRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.service.BCryptPasswordEncoder;

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