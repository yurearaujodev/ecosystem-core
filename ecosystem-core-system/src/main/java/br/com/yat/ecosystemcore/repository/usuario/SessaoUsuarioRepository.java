package br.com.yat.ecosystemcore.repository.usuario;

import br.com.yat.ecosystemcore.domain.entity.SessaoUsuario;
import br.com.yat.ecosystemcore.repository.base.GenericDao;
import java.sql.*;

public class SessaoUsuarioRepository extends GenericDao<SessaoUsuario, String> {

    public SessaoUsuarioRepository() {
        super("sessao_usuario", "id");
    }

    @Override
    protected SessaoUsuario mapResultSetToEntity(ResultSet rs) throws SQLException {
        SessaoUsuario s = new SessaoUsuario();
        s.setId(rs.getString("id"));
        s.setTenantId(rs.getString("tenant_id"));
        s.setUsuarioId(rs.getLong("usuario_id"));
        s.setEmpresaAtivaId(rs.getObject("empresa_ativa_id") != null ? rs.getLong("empresa_ativa_id") : null);
        s.setTokenAtualizacao(rs.getString("refresh_token"));
        s.setIpOrigem(rs.getString("ip_origem"));
        s.setDispositivoInfo(rs.getString("dispositivo_info"));
        
        if (rs.getTimestamp("refresh_expira_em") != null) s.setRefreshExpiraEm(rs.getTimestamp("refresh_expira_em").toLocalDateTime());
        if (rs.getTimestamp("criado_em") != null) s.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        if (rs.getTimestamp("expira_em") != null) s.setExpiraEm(rs.getTimestamp("expira_em").toLocalDateTime());
        if (rs.getTimestamp("revogado_em") != null) s.setRevogadoEm(rs.getTimestamp("revogado_em").toLocalDateTime());
        
        return s;
    }

    public void insert(Connection conn, SessaoUsuario sessao) throws SQLException {
        String sql = "INSERT INTO sessao_usuario (id, tenant_id, usuario_id, empresa_ativa_id, refresh_token, refresh_expira_em, ip_origem, dispositivo_info, expira_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessao.getId());
            stmt.setString(2, sessao.getTenantId());
            stmt.setLong(3, sessao.getUsuarioId());
            
            if (sessao.getEmpresaAtivaId() != null) {
                stmt.setLong(4, sessao.getEmpresaAtivaId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            
            stmt.setString(5, sessao.getTokenAtualizacao());
            stmt.setTimestamp(6, sessao.getRefreshExpiraEm() != null ? Timestamp.valueOf(sessao.getRefreshExpiraEm()) : null);
            stmt.setString(7, sessao.getIpOrigem());
            stmt.setString(8, sessao.getDispositivoInfo());
            stmt.setTimestamp(9, Timestamp.valueOf(sessao.getExpiraEm()));
            
            stmt.executeUpdate();
        }
    }
}
