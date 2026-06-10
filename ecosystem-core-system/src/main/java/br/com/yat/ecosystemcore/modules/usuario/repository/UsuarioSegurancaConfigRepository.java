package br.com.yat.ecosystemcore.modules.usuario.repository;

import br.com.yat.ecosystemcore.application.system.dto.UsuarioSegurancaConfigDTO;
import br.com.yat.ecosystemcore.modules.usuario.entity.UsuarioSegurancaConfig;
import br.com.yat.ecosystemcore.shared.database.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioSegurancaConfigRepository extends GenericDao<UsuarioSegurancaConfig, Long> {

	public UsuarioSegurancaConfigRepository() {
		super("usuario_seguranca_config", "usuario_id");
	}

	@Override
	protected UsuarioSegurancaConfig mapResultSetToEntity(ResultSet rs) throws SQLException {
		UsuarioSegurancaConfig u = new UsuarioSegurancaConfig();
		u.setUsuarioId(rs.getLong("usuario_id"));
		u.setTenantId(rs.getString("tenant_id"));
		u.setRequerNovaSenha(rs.getBoolean("requer_nova_senha"));
		u.setAceitaAcessoForaEmpresa(rs.getBoolean("aceita_acesso_fora_empresa"));
		u.setIpEstaticoObrigatorio(rs.getString("ip_estatico_obrigatorio"));
		u.setPermitirMultiplasSessoes(rs.getBoolean("permitir_multiplas_sessoes"));
		return u;
	}

	public void inserirPadrao(Connection conn, Long usuarioId, String tenantId) throws SQLException {
		String sql = """
				    INSERT INTO usuario_seguranca_config
				    (usuario_id, tenant_id, requer_nova_senha, aceita_acesso_fora_empresa)
				    VALUES (?, ?, 0, 1)
				""";

		executeInsert(conn, sql, usuarioId, tenantId);
	}

	public UsuarioSegurancaConfigDTO buscarPorUsuario(Connection conn, Long usuarioId) throws SQLException {
		String sql = "SELECT * FROM usuario_seguranca_config WHERE usuario_id = ?";

		Optional<UsuarioSegurancaConfig> configOpt = executeQuerySingleEntity(conn, sql, usuarioId);

		return configOpt
				.map(u -> new UsuarioSegurancaConfigDTO(u.isRequerNovaSenha(), u.isAceitaAcessoForaEmpresa(),
						u.getIpEstaticoObrigatorio(), u.isPermitirMultiplasSessoes()))
				.orElseGet(() -> new UsuarioSegurancaConfigDTO(false, true, null, false));
	}

	public boolean existeConfiguracao(Connection conn, Long usuarioId) throws SQLException {
		String sql = "SELECT COUNT(1) FROM usuario_seguranca_config WHERE usuario_id = ?";
		Long count = executeQueryForScalar(conn, sql, Long.class, usuarioId);
		return count != null && count > 0;
	}

	public void inserir(Connection conn, Long usuarioId, String tenantId, UsuarioSegurancaConfigDTO dto)
			throws SQLException {
		String sql = """
				    INSERT INTO usuario_seguranca_config
				    (usuario_id, tenant_id, requer_nova_senha, aceita_acesso_fora_empresa, ip_estatico_obrigatorio, permitir_multiplas_sessoes)
				    VALUES (?, ?, ?, ?, ?, ?)
				""";

		executeInsert(conn, sql, usuarioId, tenantId, dto.requerNovaSenha(), dto.aceitaAcessoForaEmpresa(),
				dto.ipEstaticoObrigatorio(), dto.permitirMultiplasSessoes());
	}

	public void atualizar(Connection conn, Long usuarioId, UsuarioSegurancaConfigDTO dto) throws SQLException {
		String sql = """
				    UPDATE usuario_seguranca_config
				    SET requer_nova_senha = ?,
				        aceita_acesso_fora_empresa = ?,
				        ip_estatico_obrigatorio = ?,
				        permitir_multiplas_sessoes = ?
				    WHERE usuario_id = ?
				""";

		executeUpdate(conn, sql, dto.requerNovaSenha(), dto.aceitaAcessoForaEmpresa(), dto.ipEstaticoObrigatorio(),
				dto.permitirMultiplasSessoes(), usuarioId);
	}

	/**
	 * BLINDAGEM CONTRA MÉTODOS INCOMPATÍVEIS DO GENERIC_DAO
	 */
	@Override
	public Optional<UsuarioSegurancaConfig> searchById(Connection conn, Long id, String tenantId) throws SQLException {
		// Essa tabela usa 'usuario_id' como PK direta e não possui coluna 'deleted_at'
		throw new UnsupportedOperationException(
				"Não utilize o método genérico searchById nesta classe. Utilize o método buscarPorUsuario(conn, usuarioId).");
	}

	@Override
	public boolean softDeleteById(Connection conn, Long id, String tenantId) throws SQLException {
		// Essa tabela não possui Soft Delete (coluna deleted_at) no modelo de banco de
		// dados
		throw new UnsupportedOperationException(
				"Esta tabela não possui suporte a Soft Delete (coluna deleted_at inexistente no DDL).");
	}
}