package br.com.yat.ecosystemcore.repository.empresa;

import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class EmpresaRepository extends GenericDao<Empresa, Long> {

    private static final String SQL_INSERT =
            "INSERT INTO empresa (uuid_publico, tenant_id, razao_social, nome_fantasia, cnpj, "
                    + "inscricao_estadual, telefone, logradouro, numero, bairro, cidade, estado, cep, ativo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM empresa WHERE id = ? AND deleted_at IS NULL";

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
        e.setAtivo(rs.getBoolean("ativo"));
        e.setCreatedAt(readLocalDateTime(rs, "created_at"));
        return e;
    }

    public Long insert(Connection conn, Empresa empresa) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, empresa.getUuidPublico());
            stmt.setString(2, empresa.getTenantId());
            stmt.setString(3, empresa.getRazaoSocial());
            stmt.setString(4, empresa.getNomeFantasia());
            stmt.setString(5, empresa.getCnpj());
            stmt.setString(6, empresa.getInscricaoEstadual());
            stmt.setString(7, empresa.getTelefone());
            stmt.setString(8, empresa.getLogradouro());
            stmt.setString(9, empresa.getNumero());
            stmt.setString(10, empresa.getBairro());
            stmt.setString(11, empresa.getCidade());
            stmt.setString(12, empresa.getEstado());
            stmt.setString(13, empresa.getCep());
            stmt.setBoolean(14, empresa.getAtivo() != null ? empresa.getAtivo() : true);

            stmt.executeUpdate();

            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) {
                    return gk.getLong(1);
                }
                throw new SQLException("Erro ao obter ID gerado para Empresa.");
            }
        }
    }

    /**
     * Busca empresa ativa por id (login / contexto sem filtro tenant_id na query).
     */
    public Optional<Empresa> findById(Connection conn, Long id) throws SQLException {
        return executeQuerySingleEntity(conn, SQL_FIND_BY_ID, id);
    }

    /** Alias mantido para compatibilidade com use cases existentes. */
    public Optional<Empresa> findEmpresaPorIdSemTenantId(Connection conn, Long id) throws SQLException {
        return findById(conn, id);
    }
}
