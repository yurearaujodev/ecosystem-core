package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.domain.entity.Permissao;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PermissaoRepository extends GenericDao<Permissao, Long> {

    public PermissaoRepository() {
        super("permissao", "id");
    }

    @Override
    protected Permissao mapResultSetToEntity(ResultSet rs) throws SQLException {
        Permissao p = new Permissao();
        p.setId(rs.getLong("id"));
        p.setUuidPublico(rs.getString("uuid_publico"));
        p.setTenantId(rs.getString("tenant_id"));
        p.setModulo(rs.getString("modulo"));
        p.setAcao(rs.getString("acao"));
        p.setChaveComposta(rs.getString("chave_composta"));
        p.setDescricao(rs.getString("descricao"));
        p.setVersion(rs.getInt("version"));
        return p;
    }

    /**
     * Lista todas as permissões do sistema pertencentes ao Tenant ou que sejam Globais semente.
     */
    public List<Permissao> listarPorTenantOuGlobais(Connection conn, String tenantId) throws SQLException {
        String sql = """
            SELECT * FROM permissao 
            WHERE tenant_id = ? OR tenant_id = '00000000-0000-0000-0000-000000000000' 
            ORDER BY modulo ASC, acao ASC
        """;
        return executeQuery(conn, sql, tenantId);
    }

    public Optional<Permissao> buscarPorChaveComposta(Connection conn, String tenantId, String modulo, String acao) throws SQLException {
        String sql = "SELECT * FROM permissao WHERE tenant_id = ? AND modulo = ? AND acao = ?";
        return executeQuerySingleEntity(conn, sql, tenantId, modulo, acao);
    }

    public void salvar(Connection conn, Permissao permissao) throws SQLException {
        String sql = """
            INSERT INTO permissao (uuid_publico, tenant_id, modulo, acao, descricao)
            VALUES (?, ?, ?, ?, ?)
        """;
        Long idGerado = executeInsertReturningId(conn, sql,
            UUID.randomUUID().toString(),
            permissao.getTenantId(),
            permissao.getModulo(),
            permissao.getAcao(),
            permissao.getDescricao()
        );
        if (idGerado != null) {
            permissao.setId(idGerado);
        }
    }
}
