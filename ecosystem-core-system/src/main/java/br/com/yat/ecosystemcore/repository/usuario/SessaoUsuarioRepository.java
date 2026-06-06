package br.com.yat.ecosystemcore.repository.usuario;

import br.com.yat.ecosystemcore.domain.dto.SessaoAtivaProjecaoDTO;
import br.com.yat.ecosystemcore.domain.entity.SessaoUsuario;
import br.com.yat.ecosystemcore.repository.base.GenericDao;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

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
        
        s.setRefreshExpiraEm(readLocalDateTime(rs, "refresh_expira_em"));
        s.setCriadoEm(readLocalDateTime(rs, "criado_em"));
        s.setExpiraEm(readLocalDateTime(rs, "expira_em"));
        s.setRevogadoEm(readLocalDateTime(rs, "revogado_em"));
        
        return s;
    }

    public Optional<SessaoUsuario> buscarPorId(Connection conn, String id) throws SQLException {
        String sql = "SELECT * FROM sessao_usuario WHERE id = ?";
        return executeQuerySingleEntity(conn, sql, id);
    }

    public void insert(Connection conn, SessaoUsuario sessao) throws SQLException {
        String sql = "INSERT INTO sessao_usuario (id, tenant_id, usuario_id, empresa_ativa_id, refresh_token, refresh_expira_em, ip_origem, dispositivo_info, expira_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // 🌟 ADEUS IF/ELSE! O 'executeInsert' repassa para o 'bindParameters' da GenericDao 
        // que resolve qualquer valor nulo de forma limpa e automática.
        executeInsert(conn, sql, 
            sessao.getId(), 
            sessao.getTenantId(), 
            sessao.getUsuarioId(), 
            sessao.getEmpresaAtivaId(), 
            sessao.getTokenAtualizacao(), 
            sessao.getRefreshExpiraEm(), 
            sessao.getIpOrigem(), 
            sessao.getDispositivoInfo(), 
            sessao.getExpiraEm()
        );
    }

    public void revoke(Connection conn, String sessionId) throws SQLException {
        String sql = "UPDATE sessao_usuario SET revogado_em = ? WHERE id = ?";
        executeUpdate(conn, sql, LocalDateTime.now(), sessionId);
    }

    public boolean verificarVinculoEmpresa(Connection conn, Long usuarioId, Long empresaId) throws SQLException {
        String sql = "SELECT 1 FROM empresa_usuario WHERE usuario_id = ? AND empresa_id = ?";
        // Usa a busca genérica com um mapper funcional simples
        return executeQuerySingle(conn, sql, rs -> true, usuarioId, empresaId).orElse(false);
    }
    
    /**
     * Retorna todas as sessões em tempo real que estão ativas dentro do Tenant atual.
     */
    public java.util.List<SessaoAtivaProjecaoDTO> listarSessoesAtivasPorTenant(Connection conn, String tenantId) throws SQLException {
        String sql = """
            SELECT s.id, p.nome_razao AS nome_usuario, u.email AS email_usuario, e.nome_fantasia AS nome_empresa,
                   s.ip_origem, s.dispositivo_info, s.criado_em, s.expira_em
            FROM sessao_usuario s
            INNER JOIN usuario u ON s.usuario_id = u.id AND s.tenant_id = u.tenant_id
            INNER JOIN pessoa p ON u.pessoa_id = p.id AND u.tenant_id = p.tenant_id
            LEFT JOIN empresa e ON s.empresa_ativa_id = e.id
            WHERE s.tenant_id = ? AND s.revogado_em IS NULL AND s.expira_em > CURRENT_TIMESTAMP
            ORDER BY s.criado_em DESC
            """;

        java.util.List<SessaoAtivaProjecaoDTO> lista = new java.util.ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new SessaoAtivaProjecaoDTO(
                        rs.getString("id"),
                        rs.getString("nome_usuario"), // Agora vem de p.nome_razao
                        rs.getString("email_usuario"),
                        rs.getString("nome_empresa") != null ? rs.getString("nome_empresa") : "Nenhuma Selecionada",
                        rs.getString("ip_origem"),
                        rs.getString("dispositivo_info"),
                        readLocalDateTime(rs, "criado_em"),
                        readLocalDateTime(rs, "expira_em")
                    ));
                }
            }
        }
        return lista;
    }
}