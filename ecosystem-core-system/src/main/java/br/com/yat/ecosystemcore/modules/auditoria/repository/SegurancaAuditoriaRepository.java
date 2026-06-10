package br.com.yat.ecosystemcore.modules.auditoria.repository;

import br.com.yat.ecosystemcore.modules.auditoria.entity.TentativaLoginLog;
import br.com.yat.ecosystemcore.shared.database.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SegurancaAuditoriaRepository extends GenericDao<TentativaLoginLog, Long> {

	public SegurancaAuditoriaRepository() {
		super("tentativa_login_log", "id");
	}

	@Override
	protected TentativaLoginLog mapResultSetToEntity(ResultSet rs) throws SQLException {
		TentativaLoginLog log = new TentativaLoginLog();
		log.setId(rs.getLong("id"));
		log.setTenantIdDetectado(rs.getString("tenant_id_detectado"));
		log.setEmailTentativa(rs.getString("email_tentativa"));
		log.setIpOrigem(rs.getString("ip_origem"));
		log.setDispositivoInfo(rs.getString("dispositivo_info"));
		log.setSucesso(rs.getByte("sucesso") == 1);
		log.setMotivoFalha(rs.getString("motivo_falha"));
		log.setDataHora(readLocalDateTime(rs, "data_hora"));
		return log;
	}

	/**
	 * Retorna o histórico recente de falhas e sucessos de login associados ao
	 * Tenant detectado.
	 */
	public List<TentativaLoginLog> buscarLogsRecentes(Connection conn, String tenantId, int limite)
			throws SQLException {
		String sql = "SELECT * FROM tentativa_login_log WHERE tenant_id_detectado = ? ORDER BY data_hora DESC LIMIT ?";
		return executeQuery(conn, sql, tenantId, limite);
	}

	/**
	 * Remove os computadores cadastrados como "Lembrar dispositivo" vinculados a um
	 * usuário específico.
	 */
	public void revogarDispositivoConfiavel(Connection conn, Long idDispositivo, String tenantId) throws SQLException {
		String sql = "DELETE FROM dispositivo_confiavel WHERE id = ? AND tenant_id = ?";
		executeUpdate(conn, sql, idDispositivo, tenantId);
	}
}