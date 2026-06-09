package br.com.yat.ecosystemcore.modules.pessoa.repository;

import br.com.yat.ecosystemcore.repository.base.GenericDao;
import br.com.yat.ecosystemcore.modules.pessoa.entity.Pessoa;

import java.sql.*;
import java.util.List;

public class PessoaRepository extends GenericDao<Pessoa, Long> {

    public PessoaRepository() {
        super("pessoa", "id"); // O contrutor usa a tabela "pessoa" e a PK "id"
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
        p.setAtivo(rs.getBoolean("ativo"));
        
        // MAPEANDO OS CAMPOS QUE FALTAVAM:
        p.setVersion(rs.getInt("version"));
        p.setCreatedAt(readLocalDateTime(rs, "created_at"));
        p.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        p.setDeletedAt(readLocalDateTime(rs, "deleted_at"));
        
        // Tratamento para Long que pode ser nulo no banco
        long cb = rs.getLong("created_by");
        p.setCreatedBy(rs.wasNull() ? null : cb);
        
        long ub = rs.getLong("updated_by");
        p.setUpdatedBy(rs.wasNull() ? null : ub);
        
        long db = rs.getLong("deleted_by");
        p.setDeletedBy(rs.wasNull() ? null : db);
        
        return p;
    }

    public Long insert(Connection conn, Pessoa pessoa) throws SQLException {
        // Incluindo version (começa em 1) e o criador do registro (created_by)
        String sql = """
            INSERT INTO pessoa (uuid_publico, tenant_id, tipo, nome_razao, apelido_fantasia, 
                               cpf_cnpj, telefone, version, ativo, created_by) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        Long id = executeInsertReturningId(conn, sql, 
            pessoa.getUuidPublico(),
            pessoa.getTenantId(),
            pessoa.getTipo(),
            pessoa.getNomeRazao(),
            pessoa.getApelidoFantasia(),
            pessoa.getCpfCnpj(),
            pessoa.getTelefone(),
            pessoa.getVersion(),
            pessoa.isAtivo(),
            pessoa.getCreatedBy() // Salva quem criou
        );
        
        if (id == null) {
            throw new SQLException("Erro ao obter ID gerado para Pessoa.");
        }
        return id;
    }

    public void update(Connection conn, Pessoa pessoa) throws SQLException {
        // Validação de concorrência com base na coluna VERSION e atualização do UPDATED_BY
        String sql = """
            UPDATE pessoa SET tipo = ?, nome_razao = ?, apelido_fantasia = ?, cpf_cnpj = ?, 
                              telefone = ?, ativo = ?, version = version + 1, updated_by = ?
            WHERE id = ? AND tenant_id = ? AND version = ? AND deleted_at IS NULL
        """;
        
        int rows = executeUpdate(conn, sql, 
            pessoa.getTipo(),
            pessoa.getNomeRazao(),
            pessoa.getApelidoFantasia(),
            pessoa.getCpfCnpj(),
            pessoa.getTelefone(),
            pessoa.isAtivo(),
            pessoa.getUpdatedBy(), // Quem está alterando
            pessoa.getId(),
            pessoa.getTenantId(),
            pessoa.getVersion() // Garante que ninguém alterou o registro no meio do caminho
        );
        
        if (rows == 0) {
            throw new SQLException("Falha ao atualizar pessoa: Registro não encontrado ou erro de concorrência (dados desatualizados).");
        }
        pessoa.setVersion(pessoa.getVersion()+ 1);
	}

	public List<Pessoa> findAll(Connection conn, String tenantId) throws SQLException {
		String sql = "SELECT * FROM pessoa WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY nome_razao ASC";
		return executeQuery(conn, sql, tenantId);
	}

	// Sobrescrita do soft delete padrão do GenericDao para também gravar quem
	// deletou (deleted_by)
	public boolean softDeleteComUsuario(Connection conn, Long id, String tenantId, Long usuarioLogadoId)
			throws SQLException {
		String sql = """
				    UPDATE pessoa
				    SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ?
				    WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL
				""";
		return executeUpdate(conn, sql, usuarioLogadoId, id, tenantId) > 0;
	}
}