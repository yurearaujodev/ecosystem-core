package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.application.system.dto.MfaConfigDTO;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioMfaRepository extends GenericDao<MfaConfigDTO, Long> {

    public UsuarioMfaRepository() {
        super("usuario_mfa", "usuario_id");
    }

    /**
     * Implementação obrigatória da sua classe base para converter a linha do banco no DTO
     */
    @Override
    protected MfaConfigDTO mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new MfaConfigDTO(
            rs.getLong("usuario_id"),
            rs.getString("mfa_secret"),
            null, // URL do QRCode é gerada em tempo de execução na Service
            rs.getBoolean("ativo")
        );
    }

    public void salvarNovoSegredo(Connection conn, Long usuarioId, String tenantId, String secret) throws SQLException {
        String sql = """
            INSERT INTO usuario_mfa (usuario_id, tenant_id, tipo, mfa_secret, ativo)
            VALUES (?, ?, 'TOTP', ?, 0)
            ON DUPLICATE KEY UPDATE mfa_secret = ?, ativo = 0
            """;
        // 🌟 Usando o bind inteligente automático do seu GenericDao
        executeInsert(conn, sql, usuarioId, tenantId, secret, secret);
    }

    public Optional<MfaConfigDTO> buscarPorUsuario(Connection conn, Long usuarioId) throws SQLException {
        String sql = "SELECT * FROM usuario_mfa WHERE usuario_id = ?";
        // 🌟 Usando o executor nativo do seu GenericDao
        return executeQuerySingleEntity(conn, sql, usuarioId);
    }

    public void confirmarAtivacao(Connection conn, Long usuarioId) throws SQLException {
        String sql = "UPDATE usuario_mfa SET ativo = 1 WHERE usuario_id = ?";
        executeUpdate(conn, sql, usuarioId);
    }

    public void deletarMfa(Connection conn, Long usuarioId) throws SQLException {
        String sql = "DELETE FROM usuario_mfa WHERE usuario_id = ?";
        executeUpdate(conn, sql, usuarioId);
    }
}