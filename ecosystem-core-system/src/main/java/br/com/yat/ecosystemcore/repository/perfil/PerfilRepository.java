package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.*;
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
        return p;
    }

    public Long criarPerfilAdminSeNecessario(Connection conn, String tenantId) throws SQLException {

        String sql = """
            INSERT INTO perfil (uuid_publico, tenant_id, nome, chave_identificadora, descricao)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, tenantId);
            stmt.setString(3, "Administrador");
            stmt.setString(4, "ADMIN");
            stmt.setString(5, "Acesso total ao sistema");

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Erro ao criar perfil ADMIN");
    }
}