package br.com.yat.ecosystemcore.modules.autorizacao.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.yat.ecosystemcore.shared.database.GenericDao;

public class PerfilPermissaoRepository extends GenericDao<Void, Void> {

	public PerfilPermissaoRepository() {
		super("perfil_permissao", null);
	}

	@Override
	protected Void mapResultSetToEntity(ResultSet rs) throws SQLException {
		return null; // Tabela pivô pura, não mapeia entidade de retorno única
	}

	/**
	 * Retorna todas as permissões que um determinado perfil possui ativas.
	 */
	public List<Long> listarIdsPermissoesPorPerfil(Connection conn, Long perfilId) throws SQLException {
		String sql = "SELECT permissao_id FROM perfil_permissao WHERE perfil_id = ?";
		List<Long> ids = new ArrayList<>();
		
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			bindParameters(stmt, perfilId); // 🌟 Usando o binder inteligente do Pai
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					ids.add(rs.getLong("permissao_id"));
				}
			}
		}
		return ids;
	}

	/**
	 * Limpa os privilégios antigos do perfil e vincula a nova coleção enviada pela interface gráfica.
	 */
	public void atualizarVinculos(Connection conn, Long perfilId, List<Long> idsPermissoes) throws SQLException {
		// 1. Remove acessos antigos usando o executor limpo da mãe
		String sqlDelete = "DELETE FROM perfil_permissao WHERE perfil_id = ?";
		executeUpdate(conn, sqlDelete, perfilId);

		// 2. Carga em Batch eficiente das novas permissões
		if (idsPermissoes == null || idsPermissoes.isEmpty()) return;

		String sqlInsert = "INSERT INTO perfil_permissao (perfil_id, permissao_id) VALUES (?, ?)";
		try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
			for (Long permId : idsPermissoes) {
				bindParameters(stmt, perfilId, permId); // 🌟 Bind automático por pattern matching da mãe
				stmt.addBatch();
			}
			stmt.executeBatch();
		}
	}
}
