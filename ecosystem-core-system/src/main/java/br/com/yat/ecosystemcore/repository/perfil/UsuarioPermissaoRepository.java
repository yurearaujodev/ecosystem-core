package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.repository.base.GenericDao;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioPermissaoRepository extends GenericDao<Void, Void> {

    public UsuarioPermissaoRepository() {
        super("usuario_permissao", null);
    }

    @Override
    protected Void mapResultSetToEntity(ResultSet rs) throws SQLException {
        return null; // Tabela pivô pura não mapeia entidade única
    }

    public List<Long> listarIdsPermissoesPorUsuario(Connection conn, Long usuarioId) throws SQLException {
        String sql = "SELECT permissao_id FROM usuario_permissao WHERE usuario_id = ?";
        List<Long> ids = new ArrayList<>();
        
        // Usando o engine de query customizado da classe mãe
        try (var stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("permissao_id"));
                }
            }
        }
        return ids;
    }

    public void atualizarVinculosUsuario(Connection conn, Long usuarioId, List<Long> idsPermissoes) throws SQLException {
        String sqlDelete = "DELETE FROM usuario_permissao WHERE usuario_id = ?";
        executeUpdate(conn, sqlDelete, usuarioId); // 🌟 Usando executor limpo da mãe

        if (idsPermissoes == null || idsPermissoes.isEmpty()) return;

        String sqlInsert = "INSERT INTO usuario_permissao (usuario_id, permissao_id) VALUES (?, ?)";
        try (var stmt = conn.prepareStatement(sqlInsert)) {
            for (Long permId : idsPermissoes) {
                bindParameters(stmt, usuarioId, permId); // 🌟 Bind automático por pattern matching da mãe
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}