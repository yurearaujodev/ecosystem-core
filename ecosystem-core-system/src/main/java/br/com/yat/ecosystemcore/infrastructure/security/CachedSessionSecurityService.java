package br.com.yat.ecosystemcore.infrastructure.security;

import br.com.yat.ecosystemcore.domain.entity.SessaoUsuario;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.repository.usuario.SessaoUsuarioRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CachedSessionSecurityService implements SessionValidator, SessionRepository {

    private final SessaoUsuarioRepository repository;
    private final Cache<String, Boolean> sessionCache;
    // ⚡ NOVO: Cache composto para permissões de empresas (Evita travar a UI do JavaFX)
    private final Cache<String, Boolean> empresaAcessoCache; 

    public CachedSessionSecurityService(SessaoUsuarioRepository repository) {
        this.repository = repository;

        // Configura o cache para expirar após 30 segundos da escrita na memória
        this.sessionCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();

        // ⚡ NOVO: Cache de 1 minuto para os acessos a filiais corporativas
        this.empresaAcessoCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(2000)
                .build();
    }

    @Override
    public boolean isSessionValid(String sessionId) {
        return sessionCache.get(sessionId, id -> {
            try (Connection conn = TransactionManager.getConnection()) {
                Optional<SessaoUsuario> sessaoOpt = repository.buscarPorId(conn, id);
                return sessaoOpt.isPresent() && sessaoOpt.get().isValida();
            } catch (SQLException e) {
                return false;
            }
        });
    }

    @Override
    public boolean usuarioPodeAcessarEmpresa(Long usuarioId, Long empresaId) {
        String cacheKey = usuarioId + "-" + empresaId;
        
        // ⚡ OTIMIZADO: Busca do Cache primeiro. Se não achar, vai no banco.
        return empresaAcessoCache.get(cacheKey, key -> {
            try (Connection conn = TransactionManager.getConnection()) {
                return repository.verificarVinculoEmpresa(conn, usuarioId, empresaId);
            } catch (SQLException e) {
                return false;
            }
        });
    }

    @Override
    public void revokeSession(String sessionId) {
        sessionCache.put(sessionId, false);
        // Limpa o cache de acessos para evitar retenção de lixo em memória após logout
        empresaAcessoCache.invalidateAll(); 

        try (Connection conn = TransactionManager.getConnection()) {
            repository.revoke(conn, sessionId);
        } catch (SQLException e) {
            System.err.println("Erro ao persistir revogação de sessão: " + e.getMessage());
        }
    }
}