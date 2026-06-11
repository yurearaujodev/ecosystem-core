package br.com.yat.ecosystemcore.application.tenant;

import br.com.yat.ecosystemcore.application.tenant.dto.OnboardingTenantCommand;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Pessoa;
import br.com.yat.ecosystemcore.modules.cadastro.repository.PessoaRepository;
import br.com.yat.ecosystemcore.modules.seguranca.repository.PerfilRepository;
import br.com.yat.ecosystemcore.modules.tenant.entity.Tenant;
import br.com.yat.ecosystemcore.modules.tenant.repository.TenantRepository;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Empresa;
import br.com.yat.ecosystemcore.modules.cadastro.repository.EmpresaRepository;
import br.com.yat.ecosystemcore.modules.usuario.entity.Usuario;
import br.com.yat.ecosystemcore.modules.usuario.repository.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.service.PasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class OnboardingTenantUseCase {

    private static final String TENANT_GLOBAL = "00000000-0000-0000-0000-000000000000";
    public static final Long USUARIO_SISTEMA_ID = 0L;

    private final TenantRepository tenantRepository;
    private final EmpresaRepository empresaRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final EmpresaUsuarioRepository empresaUsuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public OnboardingTenantUseCase(TenantRepository tenantRepository, EmpresaRepository empresaRepository,
                                   PessoaRepository pessoaRepository, UsuarioRepository usuarioRepository, 
                                   PerfilRepository perfilRepository, EmpresaUsuarioRepository empresaUsuarioRepository, 
                                   PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.empresaRepository = empresaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.empresaUsuarioRepository = empresaUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void executar(OnboardingTenantCommand command) throws Exception {
        // 💡 Correção 1: Adicionado o "return null;" ao fim do bloco lambda para satisfazer o TransactionalSupplier<Void>
        TransactionManager.executeInTransaction(conn -> {
            
            // 1. Força o ID Global caso seja a instalação inicial
            String tenantId = "escopo_global".equalsIgnoreCase(command.nomeConta()) ? TENANT_GLOBAL : UUID.randomUUID().toString();

            // 2. Tabela: tenant (Bloco 2)
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            tenant.setNomeConta(command.nomeConta());
            tenant.setPlano(command.plano());
            tenant.setStatus("ACTIVE");
            tenantRepository.insert(conn, tenant);

            // 3. Tabela: tenant_config (Bloco 2) - Salva o fuso e limite customizados da tela
            salvarTenantConfig(conn, tenantId, command);

            // 4. Módulos e Menus (Bloco 5) - Associa as cargas do Seeder a este Tenant
            vincularModulosEMenusDoSistema(conn, tenantId);

            // 5. Cadastros Core (Bloco 3) - Mapeia os dados customizados informados na interface
            Long empresaId = empresaRepository.insert(conn, mapearEmpresa(command, tenantId));
            Long pessoaId = pessoaRepository.insert(conn, mapearPessoa(command, tenantId));
            Long usuarioId = usuarioRepository.insert(conn, mapearUsuario(command, tenantId, pessoaId, empresaId),USUARIO_SISTEMA_ID);

            // 6. Tabela: usuario_seguranca_config (Bloco 6) - Mapeia as permissões de acesso finas
            salvarSegurancaConfig(conn, usuarioId, tenantId, command);

            // 7. Tabelas: perfil e empresa_usuario (Bloco 4 RBAC)
            Long perfilId = perfilRepository.criarPerfilAdminSeNecessario(conn, tenantId,USUARIO_SISTEMA_ID);
            concederPermissoesAoPerfil(conn, tenantId, perfilId);
            empresaUsuarioRepository.vincular(conn, tenantId, empresaId, usuarioId, perfilId);
            
            return null; 
        });
    }

    private void salvarTenantConfig(Connection conn, String tenantId, OnboardingTenantCommand cmd) throws SQLException {
        String sql = """
            INSERT INTO tenant_config (tenant_id, timezone, moeda, idioma, limite_usuarios)
            VALUES (?, ?, 'BRL', 'pt-BR', ?)
            ON DUPLICATE KEY UPDATE timezone=VALUES(timezone), limite_usuarios=VALUES(limite_usuarios)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            stmt.setString(2, cmd.timezone());
            stmt.setInt(3, cmd.limiteUsuarios());
            stmt.executeUpdate();
        }
    }

    private void salvarSegurancaConfig(Connection conn, Long usuarioId, String tenantId, OnboardingTenantCommand cmd) throws SQLException {
        String sql = """
            INSERT INTO usuario_seguranca_config (usuario_id, tenant_id, requer_nova_senha, aceita_acesso_fora_empresa, permitir_multiplas_sessoes)
            VALUES (?, ?, 0, ?, ?)
            ON DUPLICATE KEY UPDATE aceita_acesso_fora_empresa=VALUES(aceita_acesso_fora_empresa), permitir_multiplas_sessoes=VALUES(permitir_multiplas_sessoes)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, usuarioId);
            stmt.setString(2, tenantId);
            stmt.setInt(3, cmd.aceitaAcessoForaEmpresa() ? 1 : 0);
            stmt.setInt(4, cmd.permitirMultiplasSessoes() ? 1 : 0);
            stmt.executeUpdate();
        }
    }

    private void vincularModulosEMenusDoSistema(Connection conn, String tenantId) throws SQLException {
        String sqlModulos = "INSERT IGNORE INTO tenant_modulo (tenant_id, modulo_id, ativo) SELECT ?, id, 1 FROM modulo_sistema";
        String sqlMenus = "INSERT IGNORE INTO tenant_menu (tenant_id, menu_id, ativo) SELECT ?, id, 1 FROM menu_sistema";
        try (PreparedStatement stmtM = conn.prepareStatement(sqlModulos); PreparedStatement stmtN = conn.prepareStatement(sqlMenus)) {
            stmtM.setString(1, tenantId); stmtM.executeUpdate();
            stmtN.setString(1, tenantId); stmtN.executeUpdate();
        }
    }

    private void concederPermissoesAoPerfil(Connection conn, String tenantId, Long perfilId) throws SQLException {
        String sql = "INSERT IGNORE INTO perfil_permissao (perfil_id, permissao_id) SELECT ?, id FROM permissao WHERE tenant_id = '00000000-0000-0000-0000-000000000000'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, perfilId);
            stmt.executeUpdate();
        }
    }

    private Empresa mapearEmpresa(OnboardingTenantCommand cmd, String tenantId) {
        Empresa e = new Empresa();
        e.setTenantId(tenantId);
        e.setUuidPublico(UUID.randomUUID().toString());
        e.setRazaoSocial(cmd.razaoSocial());
        e.setNomeFantasia(cmd.nomeFantasia());
        e.setCnpj(cmd.cnpj().replaceAll("\\D", ""));
        e.setInscricaoEstadual(cmd.inscricaoEstadual());
        e.setTelefone(cmd.telefoneEmpresa());
        e.setLogradouro(cmd.logradouro());
        e.setCidade(cmd.cidade());
        e.setEstado(cmd.estado());
        e.setCep(cmd.cep().replaceAll("\\D", ""));
        e.setAtivo(true);
        return e;
    }

    private Pessoa mapearPessoa(OnboardingTenantCommand cmd, String tenantId) {
        Pessoa p = new Pessoa();
        p.setTenantId(tenantId);
        p.setUuidPublico(UUID.randomUUID().toString());
        p.setTipo("FISICA");
        p.setNomeRazao(cmd.nomeAdmin());
        p.setCpfCnpj(cmd.cpfAdmin().replaceAll("\\D", ""));
        p.setTelefone(cmd.telefoneAdmin());
        p.setAtivo(true);
        return p;
    }

    private Usuario mapearUsuario(OnboardingTenantCommand cmd, String tenantId, Long pessoaId, Long empresaId) {
        Usuario u = new Usuario();
        u.setTenantId(tenantId);
        u.setUuidPublico(UUID.randomUUID().toString());
        u.setPessoaId(pessoaId);
        u.setEmpresaPadraoId(empresaId);
        u.setEmail(cmd.emailAdmin().trim().toLowerCase());
        
        // 💡 Correção 2: Convertemos a String do Command em char[] no exato momento do encode
        // para se adequar à nova interface PasswordEncoder segura.
        char[] senhaChars = cmd.senhaAdmin().toCharArray();
        u.setSenhaHash(passwordEncoder.encode(senhaChars));
        
        u.setStatus("ACTIVE");
        return u;
    }
}