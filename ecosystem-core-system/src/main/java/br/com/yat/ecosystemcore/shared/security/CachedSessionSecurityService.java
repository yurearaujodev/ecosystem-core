package br.com.yat.ecosystemcore.shared.security;

import br.com.yat.ecosystemcore.modules.autenticacao.entity.SessaoUsuario;
import br.com.yat.ecosystemcore.modules.autenticacao.repository.SessaoUsuarioRepository;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CachedSessionSecurityService implements SessionValidator, SessionRepository {

	private final SessaoUsuarioRepository repository;

	private final Cache<String, SessaoUsuario> sessionCache;
	private final Cache<String, Boolean> empresaAcessoCache;

	public CachedSessionSecurityService(SessaoUsuarioRepository repository) {

		this.repository = repository;

		this.sessionCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(2000).build();

		this.empresaAcessoCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(5000).build();
	}

	@Override
	public boolean isSessionValid(String sessionId) {

		SessaoUsuario sessao = sessionCache.get(sessionId, this::buscarSessaoBanco);

		return sessao != null && sessao.isValida();
	}

	private SessaoUsuario buscarSessaoBanco(String sessionId) {

		try (Connection conn = TransactionManager.getConnection()) {

			Optional<SessaoUsuario> sessao = repository.buscarPorId(conn, sessionId);

			return sessao.orElse(null);

		} catch (SQLException e) {
			throw new RuntimeException("Falha ao validar sessão", e);
		}
	}

	@Override
	public boolean usuarioPodeAcessarEmpresa(Long usuarioId, Long empresaId) {

		String key = usuarioId + ":" + empresaId;

		return empresaAcessoCache.get(key, k -> consultarEmpresa(usuarioId, empresaId));
	}

	private Boolean consultarEmpresa(Long usuarioId, Long empresaId) {

		try (Connection conn = TransactionManager.getConnection()) {

			return repository.verificarVinculoEmpresa(conn, usuarioId, empresaId);

		} catch (SQLException e) {
			throw new RuntimeException("Falha ao validar acesso à empresa", e);
		}
	}

	@Override
	public void revokeSession(String sessionId) {

		sessionCache.invalidate(sessionId);

		try (Connection conn = TransactionManager.getConnection()) {
			repository.revoke(conn, sessionId);
		} catch (SQLException e) {
			throw new RuntimeException("Falha ao revogar sessão", e);
		}
	}

	public void invalidate(String sessionId) {
		sessionCache.invalidate(sessionId);
	}
}