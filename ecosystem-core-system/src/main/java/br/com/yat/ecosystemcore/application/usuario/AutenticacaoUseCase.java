package br.com.yat.ecosystemcore.application.usuario;

import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AutenticacaoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AutenticacaoUseCase.class);

    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final TenantRepository tenantRepository = new TenantRepository();
    private final EmpresaRepository empresaRepository = new EmpresaRepository();
    private final SessaoUsuarioRepository sessaoUsuarioRepository = new SessaoUsuarioRepository();
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoUseCase(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public SessaoDTO autenticar(String email, char[] senhaDisponivel, String tenantId) throws SQLException {
        try {
            SessaoDTO sessaoValida = TransactionManager.executeInTransaction(conn -> {
                
                // 🛡️ CORREÇÃO MULTI-TENANT: Busca restrita ao escopo do Tenant informado na UI
                Usuario usuario = usuarioRepository.findByEmailETenant(conn, email, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha incorretos."));

                // 🛡️ MITIGAÇÃO DE ENUMERAÇÃO DE CONTAS: Se bloqueado, lança a mesma mensagem de credenciais incorretas
                if (usuario.getBloqueadoAte() != null && usuario.getBloqueadoAte().isAfter(LocalDateTime.now())) {
                    logger.warn("Tentativa de login rejeitada por Lockout ativo. Usuário: {} | Tenant: {}", email, tenantId);
                    throw new IllegalArgumentException("Usuário ou senha incorretos.");
                }

                if (!passwordEncoder.matches(senhaDisponivel, usuario.getSenhaHash())) {
                    // 🛡️ Executa incremento atômico nativo no Banco
                    usuarioRepository.incrementarTentativasFalhas(conn, usuario.getId(), usuario.getTenantId());
                    throw new IllegalArgumentException("Usuário ou senha incorretos.");
                }

                // Reset do contador de falhas
                if (usuario.getTentativasLogin() > 0) {
                    usuarioRepository.resetControleAcesso(conn, usuario.getId(), usuario.getTenantId());
                }

                Tenant tenant = tenantRepository.findTenantPorIdSemTenantId(conn, usuario.getTenantId())
                    .orElseThrow(() -> new SQLException("Tenant escopo não encontrado no ecossistema."));

                Empresa empresa = null;
                if (usuario.getEmpresaPadraoId() != null && usuario.getEmpresaPadraoId() > 0) {
                    empresa = empresaRepository.findEmpresaPorIdSemTenantId(conn, usuario.getEmpresaPadraoId())
                        .orElse(null); 
                }

                SessaoUsuario sessao = new SessaoUsuario();
                String token64 =
        UUID.randomUUID().toString().replace("-", "")
      + UUID.randomUUID().toString().replace("-", "");

                sessao.setId(token64);
                sessao.setTenantId(usuario.getTenantId());
                sessao.setUsuarioId(usuario.getId());
                sessao.setEmpresaAtivaId(empresa != null ? empresa.getId() : null);
                sessao.setExpiraEm(LocalDateTime.now().plusHours(8));

                sessaoUsuarioRepository.insert(conn, sessao);

                return new SessaoDTO(usuario, tenant, empresa);
            });

            SessionManager.iniciarSessao(sessaoValida.getUsuario(), sessaoValida.getTenant(), sessaoValida.getEmpresa());
            return sessaoValida;

        } finally {
            // Garante a destruição física imediata dos dados da stack ao sair do UseCase
            java.util.Arrays.fill(senhaDisponivel, '\0');
        }
    }
}
//package br.com.yat.ecosystemcore.application.usuario;
//
//import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
//import br.com.yat.ecosystemcore.domain.entity.*;
//import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
//import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
//import br.com.yat.ecosystemcore.repository.empresa.EmpresaRepository;
//import br.com.yat.ecosystemcore.repository.tenant.TenantRepository;
//import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
//import br.com.yat.ecosystemcore.repository.usuario.UsuarioRepository;
//
//import java.sql.SQLException;
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//public class AutenticacaoUseCase {
//
//    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
//    private final TenantRepository tenantRepository = new TenantRepository();
//    private final EmpresaRepository empresaRepository = new EmpresaRepository();
//    private final SessaoUsuarioRepository sessaoUsuarioRepository = new SessaoUsuarioRepository();
//
//    private final PasswordEncoder passwordEncoder;
//
//    public AutenticacaoUseCase(PasswordEncoder passwordEncoder) {
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    public SessaoDTO autenticar(String email, String senhaPura) throws SQLException {
//        final SessaoDTO[] resposta = new SessaoDTO[1];
//
//        TransactionManager.executeInTransaction(conn -> {
//
//            Usuario usuario = usuarioRepository.findByEmail(conn, email)
//                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha incorretos."));
//
//            if (!passwordEncoder.matches(senhaPura, usuario.getSenhaHash())) {
//                throw new IllegalArgumentException("Usuário ou senha incorretos.");
//            }
//
//            Tenant tenant = tenantRepository.findTenantPorIdSemTenantId(conn, usuario.getTenantId())
//                .orElseThrow(() -> new SQLException("Tenant escopo não encontrado no ecossistema."));
//
//            Empresa empresa = null;
//            if (usuario.getEmpresaPadraoId() != null && usuario.getEmpresaPadraoId() > 0) {
//                empresa = empresaRepository.findEmpresaPorIdSemTenantId(conn, usuario.getEmpresaPadraoId())
//                    .orElse(null); 
//            }
//
//            SessaoUsuario sessao = new SessaoUsuario();
//            String token64 = (UUID.randomUUID().toString() + UUID.randomUUID().toString())
//                                .replace("-", "").substring(0, 64);
//
//            sessao.setId(token64);
//            sessao.setTenantId(usuario.getTenantId());
//            sessao.setUsuarioId(usuario.getId());
//            sessao.setEmpresaAtivaId(empresa != null ? empresa.getId() : null);
//            sessao.setExpiraEm(LocalDateTime.now().plusHours(8));
//
//            sessaoUsuarioRepository.insert(conn, sessao);
//
//            // 🛠️ VINCULAÇÃO DE SESSÃO GLOBAL PARA O JAVAFX
//            SessionManager.iniciarSessao(usuario, tenant, empresa);
//
//            resposta[0] = new SessaoDTO(usuario, tenant, empresa);
//        });
//
//        return resposta[0];
//    }
//}