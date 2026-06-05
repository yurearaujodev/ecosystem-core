package br.com.yat.ecosystemcore.repository.perfil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PerfilPermissaoRepository {

    /**
     * Retorna todas as permissões que um determinado perfil possui ativas.
     */
    public List<Long> listarIdsPermissoesPorPerfil(Connection conn, Long perfilId) throws SQLException {
        String sql = "SELECT permissao_id FROM perfil_permissao WHERE perfil_id = ?";
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, perfilId);
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
        // 1. Remove acessos antigos
        String sqlDelete = "DELETE FROM perfil_permissao WHERE perfil_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
            stmt.setLong(1, perfilId);
            stmt.executeUpdate();
        }

        // 2. Carga em Batch eficiente das novas permissões
        if (idsPermissoes == null || idsPermissoes.isEmpty()) return;

        String sqlInsert = "INSERT INTO perfil_permissao (perfil_id, permissao_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
            for (Long permId : idsPermissoes) {
                stmt.setLong(1, perfilId);
                stmt.setLong(2, permId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
