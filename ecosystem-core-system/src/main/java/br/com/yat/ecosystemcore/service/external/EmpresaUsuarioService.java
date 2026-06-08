package br.com.yat.ecosystemcore.service.external;

import br.com.yat.ecosystemcore.application.system.dto.UsuarioSegurancaConfigDTO;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.repository.perfil.EmpresaUsuarioRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioPermissaoRepository;
import br.com.yat.ecosystemcore.repository.perfil.UsuarioSegurancaConfigRepository;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.ui.modules.perfil.UsuarioGerenciamentoAbasController.CheckBoxListCellData;
import br.com.yat.ecosystemcore.ui.modules.perfil.UsuarioGerenciamentoAbasController.CustomItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class EmpresaUsuarioService {

    private final EmpresaUsuarioRepository repository = new EmpresaUsuarioRepository();
    private final UsuarioPermissaoRepository usuarioPermissaoRepo = new UsuarioPermissaoRepository();
    private final UsuarioSegurancaConfigRepository segurancaConfigRepo = new UsuarioSegurancaConfigRepository();

    /**
     * Sincroniza os vínculos utilizando uma conexão transacional ativa fornecida pelo chamador.
     * ⚡ PARTICIPA DA TRANSAÇÃO MÃE: Evita o erro de compilação na UsuarioService.
     */
    public void sincronizarVinculos(Connection conn, Long usuarioId, List<EmpresaUsuarioDetalheDTO> novosVinculos) throws SQLException {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Nenhum tenant ativo na sessão para sincronizar vínculos.");
        }
        
        String tenantId = Sessao.tenant().getId();

        // 1. Remove os vínculos antigos do escopo utilizando a conexão compartilhada
        String sqlDelete = "DELETE FROM empresa_usuario WHERE usuario_id = ? AND tenant_id = ?";
        try (var stmt = conn.prepareStatement(sqlDelete)) {
            stmt.setLong(1, usuarioId);
            stmt.setString(2, tenantId);
            stmt.executeUpdate();
        }

        // 2. Insere lote de novos vínculos de forma atômica
        if (novosVinculos != null) {
            for (EmpresaUsuarioDetalheDTO dto : novosVinculos) {
                repository.vincular(conn, tenantId, dto.getEmpresaId(), usuarioId, dto.getPerfilId());
            }
        }
    }

    /**
     * Lista todas as empresas vinculadas a uma conta de usuário filtrando pelo Tenant atual.
     */
    public List<EmpresaUsuarioDetalheDTO> listarVinculosDoUsuario(Long usuarioId) {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Nenhum tenant ativo na sessão para buscar os vínculos.");
        }
        
        String tenantId = Sessao.tenant().getId();
        
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.findVinculosPorUsuario(conn, usuarioId, tenantId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar vínculos de empresas para o usuário.", e);
        }
    }

    /**
     * Remove o acesso de um usuário a uma determinada filial da corporação.
     */
    public void removerVinculo(Long usuarioId, Long empresaId) {
        if (!Sessao.isActive()) {
            throw new IllegalStateException("Sessão inválida para remover o vínculo.");
        }
        
        String tenantId = Sessao.tenant().getId();
        
        try (Connection conn = TransactionManager.getConnection()) {
            repository.removerVinculo(conn, tenantId, empresaId, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover vínculo de empresa.", e);
        }
    }

    /**
     * Resgata chaves e IDs de permissões customizadas diretas do usuário.
     */
    public List<Long> listarIdsPermissoesExtrasDoUsuario(Long usuarioId) {
        try (Connection conn = TransactionManager.getConnection()) {
            return usuarioPermissaoRepo.listarIdsPermissoesPorUsuario(conn, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar permissões extras do usuário.", e);
        }
    }

    /**
     * Busca dados e políticas estruturais de segurança e auditoria ativas na conta.
     */
    public UsuarioSegurancaConfigDTO buscarConfiguracoesDeSeguranca(Long usuarioId) {
        try (Connection conn = TransactionManager.getConnection()) {
            return segurancaConfigRepo.buscarPorUsuario(conn, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar configurações de segurança do usuário.", e);
        }
    }
    
    
 // Dentro de EmpresaUsuarioService.java

    public List<CustomItem> buscarTodosUsuariosAtivos() throws SQLException {
        String tenantId = Sessao.tenant().getId();
        try (Connection conn = TransactionManager.getConnection()) {
            // Supondo que você tenha um método no repositório ou um SQL simples
            return repository.findAllUsersAsCustomItems(conn, tenantId);
        }
    }

    public List<CustomItem> buscarEmpresasDoTenant() throws SQLException {
        String tenantId = Sessao.tenant().getId();
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.findAllEmpresas(conn, tenantId);
        }
    }

    public List<CustomItem> buscarPerfisDisponiveis() throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.findAllPerfis(conn);
        }
    }

    public List<CheckBoxListCellData> buscarTodasPermissoes() throws SQLException {
        try (Connection conn = TransactionManager.getConnection()) {
            return repository.findAllPermissoes(conn);
        }
    }
}