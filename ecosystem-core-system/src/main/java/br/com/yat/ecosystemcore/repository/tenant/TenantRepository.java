package br.com.yat.ecosystemcore.repository.tenant;

import br.com.yat.ecosystemcore.domain.entity.Tenant;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class TenantRepository extends GenericDao<Tenant, String> {

	private static final String SQL_INSERT = "INSERT INTO tenant (id, nome_conta, plano, status) VALUES (?, ?, ?, ?)";

	private static final String SQL_FIND_BY_ID = "SELECT * FROM tenant WHERE id = ? AND deleted_at IS NULL";
	private static final String SQL_FIND_ACTIVE_BY_ID = "SELECT * FROM tenant WHERE id = ? AND deleted_at IS NULL AND status = 'ACTIVE'";

	public TenantRepository() {
		super("tenant", "id");
	}

	@Override
	protected Tenant mapResultSetToEntity(ResultSet rs) throws SQLException {
		Tenant tenant = new Tenant();
		tenant.setId(rs.getString("id"));
		tenant.setNomeConta(rs.getString("nome_conta"));
		tenant.setPlano(rs.getString("plano"));
		tenant.setStatus(rs.getString("status"));
		tenant.setCreatedAt(readLocalDateTime(rs, "created_at"));
		tenant.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
		tenant.setDeletedAt(readLocalDateTime(rs, "deleted_at"));
		return tenant;
	}

	public void insert(Connection conn, Tenant tenant) throws SQLException {
		executeInsert(conn, SQL_INSERT, tenant.getId(), tenant.getNomeConta(), tenant.getPlano(), tenant.getStatus());
	}

	public boolean updateStatus(Connection conn, String id, String status) throws SQLException {
		String sql = "UPDATE tenant SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
		return executeUpdate(conn, sql, status, id) > 0;
	}

	public boolean exists(Connection conn, String id) throws SQLException {
		String sql = "SELECT COUNT(1) FROM tenant WHERE id = ? AND deleted_at IS NULL";
		// Passamos Long.class como argumento para indicar que esperamos um Long do
		// banco
		Long count = executeQueryForScalar(conn, sql, Long.class, id);
		return count != null && count > 0;
	}

	public Optional<Tenant> findActiveById(Connection conn, String id) throws SQLException {
		return executeQuerySingleEntity(conn, SQL_FIND_ACTIVE_BY_ID, id);
	}

	/**
	 * Busca tenant ativo por id (tabela raiz, sem filtro tenant_id).
	 */
	public Optional<Tenant> findById(Connection conn, String id) throws SQLException {
		return executeQuerySingleEntity(conn, SQL_FIND_BY_ID, id);
	}

	/** Alias mantido para compatibilidade com use cases existentes. */
	public Optional<Tenant> findGlobalById(Connection conn, String id) throws SQLException {
		return findById(conn, id);
	}
}
