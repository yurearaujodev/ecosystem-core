package br.com.yat.ecosystemcore.repository.usuario;

import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository extends GenericDao<Usuario, Long> {

	public UsuarioRepository() {
		super("usuario", "id");
	}

	@Override
	protected Usuario mapResultSetToEntity(ResultSet rs) throws SQLException {
		Usuario u = new Usuario();
		u.setId(rs.getLong("id"));
		u.setUuidPublico(rs.getString("uuid_publico"));
		u.setTenantId(rs.getString("tenant_id"));
		u.setPessoaId(rs.getLong("pessoa_id"));
		u.setEmpresaPadraoId(rs.getObject("empresa_padrao_id") != null ? rs.getLong("empresa_padrao_id") : null);
		u.setEmail(rs.getString("email"));
		u.setSenhaHash(rs.getString("senha_hash"));
		u.setMacAddressAutorizado(rs.getString("mac_address_autorizado"));
		u.setTentativasLogin(rs.getInt("tentativas_login"));
		u.setBloqueadoAte(readLocalDateTime(rs, "bloqueado_ate"));
		u.setUltimoAcesso(readLocalDateTime(rs, "ultimo_acesso"));
		u.setStatus(rs.getString("status"));
		u.setVersion(rs.getInt("version"));

		// Novos campos de Auditoria e LGPD
		u.setCreatedAt(readLocalDateTime(rs, "created_at"));
		u.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
		u.setDeletedAt(readLocalDateTime(rs, "deleted_at"));

		u.setCreatedBy(rs.getObject("created_by") != null ? rs.getLong("created_by") : null);
		u.setUpdatedBy(rs.getObject("updated_by") != null ? rs.getLong("updated_by") : null);
		u.setDeletedBy(rs.getObject("deleted_by") != null ? rs.getLong("deleted_by") : null);

		u.setConsentimentoDados(rs.getBoolean("consentimento_dados"));
		u.setTermoAceitoEm(readLocalDateTime(rs, "termo_aceito_em"));
		u.setVersaoTermo(rs.getString("versao_termo"));
		u.setAnonimizadoEm(readLocalDateTime(rs, "anonimizado_em"));

		return u;
	}

	public Long insert(Connection conn, Usuario usuario, Long usuarioLogadoId) throws SQLException {
		String sql = """
				    INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, empresa_padrao_id, email,
				                         senha_hash, mac_address_autorizado, status, version, created_by,
				                         consentimento_dados, versao_termo)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
				""";
		Long id = executeInsertReturningId(conn, sql, usuario.getUuidPublico(), usuario.getTenantId(),
				usuario.getPessoaId(), usuario.getEmpresaPadraoId(), usuario.getEmail(), usuario.getSenhaHash(),
				usuario.getMacAddressAutorizado(), // ADICIONE ISSO
				usuario.getStatus() != null ? usuario.getStatus() : "ACTIVE", usuarioLogadoId,
				usuario.isConsentimentoDados(), usuario.getVersaoTermo());
		return id;
	}

	public void update(Connection conn, Usuario usuario, Long usuarioLogadoId) throws SQLException {
		String sql = """
				    UPDATE usuario SET empresa_padrao_id = ?, email = ?, senha_hash = ?, status = ?,
				                       mac_address_autorizado = ?, updated_by = ?, version = version + 1,
				                       consentimento_dados = ?, termo_aceito_em = ?
				    WHERE id = ? AND tenant_id = ? AND version = ? AND deleted_at IS NULL
				""";

		int rows = executeUpdate(conn, sql, usuario.getEmpresaPadraoId(), usuario.getEmail(), usuario.getSenhaHash(),
				usuario.getStatus(), usuario.getMacAddressAutorizado(), usuarioLogadoId, // ADICIONE ISSO
				usuario.isConsentimentoDados(), usuario.getTermoAceitoEm(), usuario.getId(), usuario.getTenantId(),
				usuario.getVersion());

		if (rows == 0)
			throw new SQLException("Erro de concorrência ao atualizar usuário.");
	}

	public List<Usuario> findAll(Connection conn, String tenantId) throws SQLException {
		String sql = "SELECT * FROM usuario WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY email ASC";
		return executeQuery(conn, sql, tenantId);
	}

	public Optional<Usuario> findByEmailETenant(Connection conn, String email, String tenantId) throws SQLException {
		String sql = "SELECT * FROM usuario WHERE email = ? AND tenant_id = ? AND status = 'ACTIVE' AND deleted_at IS NULL";
		return executeQuerySingleEntity(conn, sql, email, tenantId);
	}

	public boolean softDeleteComUsuario(Connection conn, Long id, String tenantId, Long usuarioLogadoId)
			throws SQLException {
		String sql = """
				    UPDATE usuario
				    SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ?
				    WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL
				""";
		return executeUpdate(conn, sql, usuarioLogadoId, id, tenantId) > 0;
	}

	public void incrementarTentativasFalhas(Connection conn, Long usuarioId, String tenantId) throws SQLException {
		String sql = """
				    UPDATE usuario
				    SET tentativas_login = tentativas_login + 1,
				        bloqueado_ate = CASE WHEN tentativas_login + 1 >= 5 THEN DATE_ADD(NOW(), INTERVAL 15 MINUTE) ELSE NULL END,
				        version = version + 1
				    WHERE id = ? AND tenant_id = ?
				""";
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, usuarioId);
			stmt.setString(2, tenantId);
			stmt.executeUpdate();
		}
	}

	public void resetControleAcesso(Connection conn, Long usuarioId, String tenantId) throws SQLException {
		String sql = """
				    UPDATE usuario
				    SET tentativas_login = 0, bloqueado_ate = NULL, version = version + 1
				    WHERE id = ? AND tenant_id = ?
				""";
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, usuarioId);
			stmt.setString(2, tenantId);
			stmt.executeUpdate();
		}
	}
}