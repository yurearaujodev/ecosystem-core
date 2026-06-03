package br.com.yat.ecosystemcore.repository.usuario;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.*;

public class PessoaRepository extends GenericDao<Pessoa, Long> {

    public PessoaRepository() {
        super("pessoa", "id");
    }

    @Override
    protected Pessoa mapResultSetToEntity(ResultSet rs) throws SQLException {
        Pessoa p = new Pessoa();
        p.setId(rs.getLong("id"));
        p.setUuidPublico(rs.getString("uuid_publico"));
        p.setTenantId(rs.getString("tenant_id"));
        p.setTipo(rs.getString("tipo"));
        p.setNomeRazao(rs.getString("nome_razao"));
        p.setApelidoFantasia(rs.getString("apelido_fantasia"));
        p.setCpfCnpj(rs.getString("cpf_cnpj"));
        p.setTelefone(rs.getString("telefone"));
        return p;
    }

    public Long insert(Connection conn, Pessoa pessoa) throws SQLException {
        String sql = "INSERT INTO pessoa (uuid_publico, tenant_id, tipo, nome_razao, apelido_fantasia, cpf_cnpj, telefone) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, pessoa.getUuidPublico());
            stmt.setString(2, pessoa.getTenantId());
            stmt.setString(3, pessoa.getTipo());
            stmt.setString(4, pessoa.getNomeRazao());
            stmt.setString(5, pessoa.getApelidoFantasia());
            stmt.setString(6, pessoa.getCpfCnpj());
            stmt.setString(7, pessoa.getTelefone());
            
            stmt.executeUpdate();
            
            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) return gk.getLong(1);
                throw new SQLException("Erro ao obter ID gerado para Pessoa.");
            }
        }
    }
}

