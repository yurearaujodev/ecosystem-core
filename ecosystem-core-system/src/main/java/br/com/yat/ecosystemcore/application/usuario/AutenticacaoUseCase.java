package br.com.yat.ecosystemcore.application.usuario;

import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
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
            return TransactionManager.executeInTransaction(conn -> {
                
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

                // 🚀 GERAÇÃO DE CHAVES E TOKENS DE SEGURANÇA
                String sessionId = UUID.randomUUID().toString().replace("-", "") 
                                 + UUID.randomUUID().toString().replace("-", "");
                
                String refreshToken = UUID.randomUUID().toString().replace("-", "");
                
                LocalDateTime expiraEm = LocalDateTime.now().plusHours(8);
                LocalDateTime refreshExpiraEm = LocalDateTime.now().plusDays(7); // Refresh token válido por 7 dias

                // Instancia o registro físico da sessão para gravação em banco
                SessaoUsuario sessao = new SessaoUsuario();
                sessao.setId(sessionId);
                sessao.setTenantId(usuario.getTenantId());
                sessao.setUsuarioId(usuario.getId());
                sessao.setEmpresaAtivaId(empresa != null ? empresa.getId() : null);
                sessao.setTokenAtualizacao(refreshToken);
                sessao.setRefreshExpiraEm(refreshExpiraEm);
                sessao.setExpiraEm(expiraEm);
                sessao.setIpOrigem("127.0.0.1"); // Pode mapear o IP real futuramente
                sessao.setDispositivoInfo("Desktop JavaFX");

                // Salva no banco usando as regras do seu repositório físico
                sessaoUsuarioRepository.insert(conn, sessao);

                logger.info("Sessão [{}] aberta com sucesso para o usuário: {}", sessionId, usuario.getEmail());

                // 🌟 AGORA RETORNA COMPLETO: Instancia o SessaoDTO com os novos metadados obrigatórios!
                return new SessaoDTO(usuario, tenant, empresa, sessionId, expiraEm, refreshToken);
            });

        } finally {
            // Garante a destruição física imediata dos dados da stack ao sair do UseCase
            java.util.Arrays.fill(senhaDisponivel, '\0');
        }
    }
}