package br.com.yat.ecosystemcore.modules.autenticacao.service;

//import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.shared.context.SessionContext;
import br.com.yat.ecosystemcore.shared.context.UserContext;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.modules.autenticacao.entity.SessaoUsuario;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Empresa;
import br.com.yat.ecosystemcore.modules.cadastro.repository.EmpresaRepository;
import br.com.yat.ecosystemcore.modules.tenant.entity.Tenant;
import br.com.yat.ecosystemcore.modules.tenant.repository.TenantRepository;
import br.com.yat.ecosystemcore.modules.usuario.entity.Usuario;
import br.com.yat.ecosystemcore.modules.usuario.repository.UsuarioRepository;
import br.com.yat.ecosystemcore.modules.usuario.service.PasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class AutenticacaoUseCase {

	private static final Logger logger = LoggerFactory.getLogger(AutenticacaoUseCase.class);

	private final UsuarioRepository usuarioRepository;
	private final TenantRepository tenantRepository;
	private final EmpresaRepository empresaRepository;
	private final SessaoUsuarioService sessionService;
	private final PasswordEncoder passwordEncoder;

	public AutenticacaoUseCase(UsuarioRepository usuarioRepository, TenantRepository tenantRepository,
			EmpresaRepository empresaRepository, SessaoUsuarioService sessionService, PasswordEncoder passwordEncoder) {

		this.usuarioRepository = usuarioRepository;
		this.tenantRepository = tenantRepository;
		this.empresaRepository = empresaRepository;
		this.sessionService = sessionService;
		this.passwordEncoder = passwordEncoder;
	}

	public SessionContext autenticar(String email, char[] senhaDisponivel, String tenantId) throws SQLException {
		try {
			return TransactionManager.executeInTransaction(conn -> {

				// 🛡️ CORREÇÃO MULTI-TENANT: Busca restrita ao escopo do Tenant informado na UI
				Usuario usuario = usuarioRepository.findByEmailETenant(conn, email, tenantId)
						.orElseThrow(() -> new IllegalArgumentException("Usuário ou senha incorretos."));

				// 🛡️ MITIGAÇÃO DE ENUMERAÇÃO DE CONTAS: Se bloqueado, lança a mesma mensagem
				// de credenciais incorretas
				if (usuario.getBloqueadoAte() != null && usuario.getBloqueadoAte().isAfter(LocalDateTime.now())) {
					logger.warn("Tentativa de login rejeitada por Lockout ativo. Usuário: {} | Tenant: {}", email,
							tenantId);
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

				Tenant tenant = tenantRepository.findGlobalById(conn, usuario.getTenantId())
						.orElseThrow(() -> new SQLException("Tenant escopo não encontrado no ecossistema."));

				Empresa empresa = null;
				if (usuario.getEmpresaPadraoId() != null && usuario.getEmpresaPadraoId() > 0) {

					empresa = empresaRepository
							.findByIdAndTenant(conn, usuario.getEmpresaPadraoId(), usuario.getTenantId()).orElse(null);
				}

				SessaoUsuario sessao = sessionService.criarSessao(usuario, empresa);

				sessionService.salvarSessao(conn, sessao);

				logger.info("Sessão [{}] aberta com sucesso para o usuário: {}", sessao.getId(), usuario.getEmail());

				UserContext userContext = new UserContext(usuario.getId(), usuario.getEmail(), usuario.getEmail(),
						java.util.Collections.emptySet());

				return new SessionContext(usuario.getId(), tenant.getId(), empresa != null ? empresa.getId() : null,
						sessao.getId(), sessao.getExpiraEm(), null, sessao.getRefreshToken(), userContext);
			});

			// 🌟 AGORA RETORNA COMPLETO: Instancia o SessaoDTO com os novos metadados
			// obrigatórios!
//				return new SessaoDTO(usuario, tenant, empresa, sessao.getId(), sessao.getExpiraEm(),
//						sessao.getRefreshToken());
//			});

		} finally {
			// Garante a destruição física imediata dos dados da stack ao sair do UseCase
			java.util.Arrays.fill(senhaDisponivel, '\0');
		}
	}
}