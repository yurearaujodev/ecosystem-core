//package br.com.yat.ecosystemcore.shared.security;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.concurrent.TimeUnit;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//
//import br.com.yat.ecosystemcore.domain.entity.SessaoUsuario;
//import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;
//import br.com.yat.ecosystemcore.shared.database.TransactionManager;
//
//public class SessionValidatorCache {
//
//	private final Cache<String, Boolean> cache;
//
//	private final SessaoUsuarioRepository repository;
//
//	public SessionValidatorCache(SessaoUsuarioRepository repository) {
//		this.repository = repository;
//
//		this.cache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(5000).build();
//	}
//
//	public boolean isValid(String sessionId) {
//		return cache.get(sessionId, this::checkDb);
//	}
//
//	private Boolean checkDb(String sessionId) {
//		try (Connection conn = TransactionManager.getConnection()) {
//			return repository.buscarPorId(conn, sessionId).map(SessaoUsuario::isValida).orElse(false);
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//	}
//}
