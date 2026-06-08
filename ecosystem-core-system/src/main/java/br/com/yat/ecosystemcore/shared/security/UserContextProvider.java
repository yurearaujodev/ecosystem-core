package br.com.yat.ecosystemcore.shared.security;

import br.com.yat.ecosystemcore.shared.context.UserContext;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.ui.modules.usuario.repository.UsuarioRepository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

public final class UserContextProvider {

	private static UsuarioRepository repository;

	private static final Cache<String, UserContext> CACHE = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
			.maximumSize(10_000).build();

	private UserContextProvider() {
	}

	public static void init(UsuarioRepository repo) {
		repository = repo;
	}

	public static UserContext get(Long usuarioId, String tenantId, Long empresaId) {
		if (usuarioId == null || tenantId == null || empresaId == null)
			return null;

		String cacheKey = tenantId + ":" + usuarioId + ":" + empresaId;

		return CACHE.get(cacheKey, key -> {
			try (Connection conn = TransactionManager.getConnection()) {
				return repository.buscarUserContext(conn, usuarioId, tenantId, empresaId);
			} catch (Exception e) {
				throw new RuntimeException("Falha ao carregar UserContext para a chave " + key, e);
			}
		});
	}

	public static void invalidate(String tenantId, Long usuarioId, Long empresaId) {
		if (tenantId != null && usuarioId != null && empresaId != null) {
			String cacheKey = tenantId + ":" + usuarioId + ":" + empresaId;
			CACHE.invalidate(cacheKey);
		}
	}

	public static void invalidateCompletamenteUsuario(String tenantId, Long usuarioId) {
		if (tenantId == null || usuarioId == null)
			return;

		String prefixoParaRemover = tenantId + ":" + usuarioId + ":";

		CACHE.asMap().keySet().removeIf(key -> key.startsWith(prefixoParaRemover));
	}

	public static void invalidateTodoTenant(String tenantId) {
		if (tenantId == null)
			return;

		String prefixoTenant = tenantId + ":";
		CACHE.asMap().keySet().removeIf(key -> key.startsWith(prefixoTenant));
	}
}