package br.com.yat.ecosystemcore.repository.empresa;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EmpresaRepository extends GenericDao<Empresa, Long> {

    public EmpresaRepository() {
        super("empresa", "id");
    }

    @Override
    protected Empresa mapResultSetToEntity(ResultSet rs) throws SQLException {
        Empresa e = new Empresa();
        e.setId(rs.getLong("id"));
        e.setUuidPublico(rs.getString("uuid_publico"));
        e.setTenantId(rs.getString("tenant_id"));
        e.setRazaoSocial(rs.getString("razao_social"));
        e.setNomeFantasia(rs.getString("nome_fantasia"));
        e.setCnpj(rs.getString("cnpj"));
        e.setInscricaoEstadual(rs.getString("inscricao_estadual"));
        e.setTelefone(rs.getString("telefone"));
        e.setLogradouro(rs.getString("logradouro"));
        e.setNumero(rs.getString("numero"));
        e.setBairro(rs.getString("bairro"));
        e.setCidade(rs.getString("cidade"));
        e.setEstado(rs.getString("estado"));
        e.setCep(rs.getString("cep"));
        e.setVersion(rs.getInt("version"));
        e.setAtivo(rs.getBoolean("ativo"));
        e.setCreatedAt(readLocalDateTime(rs, "created_at"));
        e.setDeletedAt(readLocalDateTime(rs, "deleted_at"));
        return e;
    }

    // Lista empresas do Tenant ativo que não foram excluídas
    public List<Empresa> listarPorTenant(Connection conn, String tenantId) throws SQLException {
        String sql = "SELECT * FROM empresa WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY razao_social";
        return executeQuery(conn, sql, tenantId);
    }

    // Insere usando seu método utilitário que captura a Primary Key auto-incrementada
    public Long insert(Connection conn, Empresa empresa) throws SQLException {
        String sql = """
            INSERT INTO empresa (uuid_publico, tenant_id, razao_social, nome_fantasia, cnpj, 
                                 inscricao_estadual, telefone, logradouro, numero, bairro, cidade, estado, cep, ativo) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        String uuid = (empresa.getUuidPublico() != null) ? empresa.getUuidPublico() : UUID.randomUUID().toString();
        Boolean ativo = (empresa.getAtivo() != null) ? empresa.getAtivo() : true;

        return executeInsertReturningId(conn, sql, 
            uuid, empresa.getTenantId(), empresa.getRazaoSocial(), empresa.getNomeFantasia(),
            empresa.getCnpj(), empresa.getInscricaoEstadual(), empresa.getTelefone(), 
            empresa.getLogradouro(), empresa.getNumero(), empresa.getBairro(), 
            empresa.getCidade(), empresa.getEstado(), empresa.getCep(), ativo
        );
    }

    // Atualiza controlando a concorrência via bloqueio otimista (Versionamento)
    public boolean update(Connection conn, Empresa empresa) throws SQLException {
        String sql = """
            UPDATE empresa SET razao_social = ?, nome_fantasia = ?, cnpj = ?, inscricao_estadual = ?, 
                               telefone = ?, logradouro = ?, numero = ?, bairro = ?, cidade = ?, 
                               estado = ?, cep = ?, ativo = ?, version = version + 1
            WHERE id = ? AND tenant_id = ? AND version = ?
        """;

        int linhasAfetadas = executeUpdate(conn, sql,
            empresa.getRazaoSocial(), empresa.getNomeFantasia(), empresa.getCnpj(), empresa.getInscricaoEstadual(),
            empresa.getTelefone(), empresa.getLogradouro(), empresa.getNumero(), empresa.getBairro(),
            empresa.getCidade(), empresa.getEstado(), empresa.getCep(), empresa.getAtivo(),
            empresa.getId(), empresa.getTenantId(), empresa.getVersion()
        );
        
        return linhasAfetadas > 0;
    }

    // Soft Delete refinado atualizando quem realizou a exclusão técnica (deleted_by)
    public boolean softDelete(Connection conn, Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        String sql = "UPDATE empresa SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL";
        return executeUpdate(conn, sql, usuarioLogadoId, id, tenantId) > 0;
    }

    public Optional<Empresa> findById(Connection conn, Long id) throws SQLException {
        String sql = "SELECT * FROM empresa WHERE id = ? AND deleted_at IS NULL";
        return executeQuerySingleEntity(conn, sql, id);
    }
    
    /** * Alias mantido para compatibilidade com o caso de uso de autenticação (AutenticacaoUseCase) 
     */
    public Optional<Empresa> findEmpresaPorIdSemTenantId(Connection conn, Long id) throws SQLException {
        return findById(conn, id);
    }
}