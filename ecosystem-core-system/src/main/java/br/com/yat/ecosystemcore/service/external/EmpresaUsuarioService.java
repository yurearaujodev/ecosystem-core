package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.application.system.dto.UsuarioSegurancaConfigDTO;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.infrastructure.database.TransactionManager;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class EmpresaUsuarioService {

    private final EmpresaUsuarioRepository repository = new EmpresaUsuarioRepository();
    private final UsuarioPermissaoRepository usuarioPermissaoRepo = new UsuarioPermissaoRepository();
    private final UsuarioSegurancaConfigRepository segurancaConfigRepo = new UsuarioSegurancaConfigRepository();

    /**
     * Sincroniza os vínculos de um usuário capturando a conexão contextual da Thread.
     */
    public void sincronizarVinculos(Long usuarioId, List<EmpresaUsuarioDetalheDTO> novosVinculos) throws SQLException {
        String tenantId = SessionManager.getTenantAtual().getId();

        try (Connection conn = TransactionManager.getConnection()) {
            // 1. Remove vínculos antigos
            String sqlDelete = "DELETE FROM empresa_usuario WHERE usuario_id = ? AND tenant_id = ?";
            try (var stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setLong(1, usuarioId);
                stmt.setString(2, tenantId);
                stmt.executeUpdate();
            }

            // 2. Insere os novos vínculos
            for (EmpresaUsuarioDetalheDTO dto : novosVinculos) {
                repository.vincular(conn, tenantId, dto.getEmpresaId(), usuarioId, dto.getPerfilId());
            }
        }
    }

    public List<EmpresaUsuarioDetalheDTO> listarVinculosDoUsuario(Long usuarioId) {
        String tenantId = SessionManager.getTenantAtual().getId();
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.findVinculosPorUsuario(conn, usuarioId, tenantId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar vínculos de empresas para o usuário.", e);
        }
    }

    public void removerVinculo(Long usuarioId, Long empresaId) {
        String tenantId = SessionManager.getTenantAtual().getId();
        try (Connection conn = TransactionManager.getConnection()) {
            repository.removerVinculo(conn, tenantId, empresaId, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover vínculo de empresa.", e);
        }
    }

    public List<Long> listarIdsPermissoesExtrasDoUsuario(Long usuarioId) {
        try (Connection conn = TransactionManager.getConnection()) {
            return usuarioPermissaoRepo.listarIdsPermissoesPorUsuario(conn, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar permissões extras do usuário.", e);
        }
    }

    public UsuarioSegurancaConfigDTO buscarConfiguracoesDeSeguranca(Long usuarioId) {
        try (Connection conn = TransactionManager.getConnection()) {
            return segurancaConfigRepo.buscarPorUsuario(conn, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar configurações de segurança do usuário.", e);
        }
    }
}