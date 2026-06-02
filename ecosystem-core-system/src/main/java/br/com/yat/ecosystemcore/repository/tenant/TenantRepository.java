package br.com.yat.ecosystemcore.repository.tenant;


import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

// Passamos a Entidade e o tipo da chave primária (UUID) para o Generics
public class TenantRepository extends GenericDao<Tenant, UUID> {

    public TenantRepository() {
        super("tenant", "id");
    }

    @Override
    protected Tenant mapResultSetToEntity(ResultSet rs) throws SQLException {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString(rs.getString("id")));
        tenant.setNomeConta(rs.getString("nome_conta"));
        // Mapeie os demais campos...
        return tenant;
    }

    public void salvarNovo(Connection conn, Tenant tenant) throws SQLException {
        String sql = "INSERT INTO tenant (id, nome_conta, plano, status) VALUES (?, ?, ?, ?)";
        executeInsert(conn, sql, tenant.getId(), tenant.getNomeConta(), tenant.getPlano(), tenant.getStatus());
    }
}
