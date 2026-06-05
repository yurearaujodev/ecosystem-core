package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PerfilRepository extends GenericDao<Perfil, Long> {

    public PerfilRepository() {
        super("perfil", "id");
    }

    @Override
    protected Perfil mapResultSetToEntity(ResultSet rs) throws SQLException {
        Perfil p = new Perfil();
        p.setId(rs.getLong("id"));
        p.setUuidPublico(rs.getString("uuid_publico"));
        p.setTenantId(rs.getString("tenant_id"));
        p.setNome(rs.getString("nome"));
        p.setChaveIdentificadora(rs.getString("chave_identificadora"));
        p.setDescricao(rs.getString("descricao"));
        
        // Mapeamento dos novos campos de auditoria
        p.setVersion(rs.getInt("version"));
        p.setCreatedAt(readLocalDateTime(rs, "created_at"));
        p.setDeletedAt(readLocalDateTime(rs, "deleted_at"));
        p.setCreatedBy(rs.getObject("created_by") != null ? rs.getLong("created_by") : null);
        p.setUpdatedBy(rs.getObject("updated_by") != null ? rs.getLong("updated_by") : null);
        p.setDeletedBy(rs.getObject("deleted_by") != null ? rs.getLong("deleted_by") : null);
        
        return p;
    }
    
    public Long criarPerfilAdminSeNecessario(Connection conn, String tenantId, Long usuarioCriadorId) throws SQLException {
        String sql = """
            INSERT INTO perfil (uuid_publico, tenant_id, nome, chave_identificadora, descricao, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        // Chama o executor herdado passando os parâmetros em ordem
        Long idGerado = executeInsertReturningId(conn, sql, 
            UUID.randomUUID().toString(),
            tenantId,
            "Administrador",
            "ADMIN",
            "Acesso total ao sistema",
            usuarioCriadorId // O bindParameters já trata o nulo de forma nativa!
        );

        if (idGerado == null) {
            throw new SQLException("Erro ao criar perfil ADMIN: Nenhuma chave primária foi retornada.");
        }

        return idGerado;
    }

    public void salvar(Connection conn, Perfil perfil) throws SQLException {
        String sql = """
            INSERT INTO perfil (uuid_publico, tenant_id, nome, chave_identificadora, descricao, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        Long idGerado = executeInsertReturningId(conn, sql, 
            perfil.getUuidPublico(), 
            perfil.getTenantId(), 
            perfil.getNome(), 
            perfil.getChaveIdentificadora(), 
            perfil.getDescricao(), 
            perfil.getCreatedBy()
        );
        if (idGerado != null) {
            perfil.setId(idGerado);
        }
    }

    public void atualizar(Connection conn, Perfil perfil) throws SQLException {
        String sql = """
            UPDATE perfil 
            SET nome = ?, chave_identificadora = ?, descricao = ?, updated_by = ?, version = version + 1
            WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL
        """;
        executeUpdate(conn, sql, 
            perfil.getNome(), 
            perfil.getChaveIdentificadora(), 
            perfil.getDescricao(), 
            perfil.getUpdatedBy(), 
            perfil.getId(), 
            perfil.getTenantId()
        );
    }

    public List<Perfil> listarPorTenant(Connection conn, String tenantId) throws SQLException {
        String sql = "SELECT * FROM perfil WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY nome ASC";
        return executeQuery(conn, sql, tenantId);
    }
    
    public Optional<Perfil> buscarPorChave(Connection conn, String tenantId, String chave) throws SQLException {
        String sql = "SELECT * FROM perfil WHERE tenant_id = ? AND chave_identificadora = ? AND deleted_at IS NULL";
        return executeQuerySingleEntity(conn, sql, tenantId, chave);
    }

    /**
     * Sobrescreve o softDelete original para injetar o ID do usuário que realizou a deleção
     */
    public boolean softDeleteComAuditoria(Connection conn, Long id, String tenantId, Long usuarioDeletouId) throws SQLException {
        String sql = """
            UPDATE perfil 
            SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? 
            WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL
        """;
        return executeUpdate(conn, sql, usuarioDeletouId, id, tenantId) > 0;
    }
}