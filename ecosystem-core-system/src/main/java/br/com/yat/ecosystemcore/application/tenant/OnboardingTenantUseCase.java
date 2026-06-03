package br.com.yat.ecosystemcore.application.tenant;

import br.com.yat.ecosystemcore.application.tenant.dto.OnboardingTenantCommand;
import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.perfil.PerfilRepository;
import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
import br.com.yat.ecosystemcore.repository.usuario.PessoaRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioSegurancaConfigRepository;

import java.util.UUID;

public class OnboardingTenantUseCase {

    private final TenantRepository tenantRepository;
    private final EmpresaRepository empresaRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final EmpresaUsuarioRepository empresaUsuarioRepository;
    private final UsuarioSegurancaConfigRepository segurancaRepository;
    private final PasswordEncoder passwordEncoder;

    public OnboardingTenantUseCase(TenantRepository tenantRepository, EmpresaRepository empresaRepository,
            PessoaRepository pessoaRepository, UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
            EmpresaUsuarioRepository empresaUsuarioRepository, UsuarioSegurancaConfigRepository segurancaRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.empresaRepository = empresaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.empresaUsuarioRepository = empresaUsuarioRepository;
        this.segurancaRepository = segurancaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void executar(OnboardingTenantCommand command) throws Exception {
        TransactionManager.executeInTransaction(conn -> {
            String tenantId = UUID.randomUUID().toString();

            Tenant tenant = criarTenant(command, tenantId);
            tenantRepository.insert(conn, tenant);

            Long empresaId = empresaRepository.insert(conn, criarEmpresa(command, tenantId));

            Long pessoaId = pessoaRepository.insert(conn, criarPessoa(command, tenantId));

            Long usuarioId = usuarioRepository.insert(conn, criarUsuario(command, tenantId, pessoaId, empresaId));

            segurancaRepository.inserirPadrao(conn, usuarioId, tenantId);

            Long perfilId = perfilRepository.criarPerfilAdminSeNecessario(conn, tenantId);

            empresaUsuarioRepository.vincular(conn, tenantId, empresaId, usuarioId, perfilId);
        });
    }

    private Tenant criarTenant(OnboardingTenantCommand command, String tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setNomeConta(command.nomeConta());
        tenant.setPlano(command.plano());
        tenant.setStatus("ACTIVE");
        return tenant;
    }

    private Empresa criarEmpresa(OnboardingTenantCommand command, String tenantId) {
        Empresa empresa = new Empresa();
        empresa.setTenantId(tenantId);
        empresa.setUuidPublico(UUID.randomUUID().toString());
        empresa.setRazaoSocial(command.razaoSocial());
        empresa.setNomeFantasia(command.nomeFantasia());
        empresa.setCnpj(somenteDigitos(command.cnpj()));
        empresa.setTelefone(command.telefoneEmpresa());
        empresa.setCidade(command.cidade());
        empresa.setEstado(command.estado());
        empresa.setAtivo(true);
        return empresa;
    }

    private Pessoa criarPessoa(OnboardingTenantCommand command, String tenantId) {
        Pessoa pessoa = new Pessoa();
        pessoa.setTenantId(tenantId);
        pessoa.setUuidPublico(UUID.randomUUID().toString());
        pessoa.setTipo("FISICA");
        pessoa.setNomeRazao(command.nomeAdmin());
        pessoa.setCpfCnpj(somenteDigitos(command.cpfAdmin()));
        pessoa.setTelefone(command.telefoneAdmin());
        return pessoa;
    }

    private Usuario criarUsuario(OnboardingTenantCommand command, String tenantId, Long pessoaId, Long empresaId) {
        Usuario usuario = new Usuario();
        usuario.setTenantId(tenantId);
        usuario.setUuidPublico(UUID.randomUUID().toString());
        usuario.setPessoaId(pessoaId);
        usuario.setEmpresaPadraoId(empresaId);
        usuario.setEmail(command.emailAdmin().trim().toLowerCase());
        usuario.setSenhaHash(passwordEncoder.encode(command.senhaAdmin()));
        usuario.setStatus("ACTIVE");
        return usuario;
    }

    private static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }
}
