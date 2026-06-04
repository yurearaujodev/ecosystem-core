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
        u.setTentativasLogin(rs.getInt("tentativas_login"));
        u.setBloqueadoAte(readLocalDateTime(rs, "bloqueado_ate"));
        u.setUltimoAcesso(readLocalDateTime(rs, "ultimo_acesso"));
        u.setStatus(rs.getString("status"));
        u.setVersion(rs.getInt("version"));
        return u;
    }

    public Long insert(Connection conn, Usuario usuario, Long usuarioLogadoId) throws SQLException {
        String sql = """
            INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, empresa_padrao_id, email, 
                                 senha_hash, status, version, created_by) 
            VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)
        """;
        Long id = executeInsertReturningId(conn, sql, 
            usuario.getUuidPublico(),
            usuario.getTenantId(),
            usuario.getPessoaId(),
            usuario.getEmpresaPadraoId(),
            usuario.getEmail(),
            usuario.getSenhaHash(),
            usuario.getStatus() != null ? usuario.getStatus() : "ACTIVE",
            usuarioLogadoId // Vincula quem realizou a criação do acesso
        );

        if (id == null) {
            throw new SQLException("Erro ao obter ID gerado para Usuário.");
        }
        return id;
    }
    
    public Long insert(Connection conn, Usuario usuario) throws SQLException {
        String sql = """
            INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, empresa_padrao_id, email, senha_hash, status) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        return executeInsertReturningId(conn, sql, 
            usuario.getUuidPublico(),
            usuario.getTenantId(),
            usuario.getPessoaId(),
            usuario.getEmpresaPadraoId(),
            usuario.getEmail(),
            usuario.getSenhaHash(),
            usuario.getStatus() != null ? usuario.getStatus() : "ACTIVE"
        );
    }

    public void update(Connection conn, Usuario usuario, Long usuarioLogadoId) throws SQLException {
        String sql = """
            UPDATE usuario SET empresa_padrao_id = ?, email = ?, senha_hash = ?, status = ?, 
                               version = version + 1, updated_by = ?
            WHERE id = ? AND tenant_id = ? AND version = ? AND deleted_at IS NULL
        """;
        
        int rows = executeUpdate(conn, sql, 
            usuario.getEmpresaPadraoId(),
            usuario.getEmail(),
            usuario.getSenhaHash(),
            usuario.getStatus(),
            usuarioLogadoId,
            usuario.getId(),
            usuario.getTenantId(),
            usuario.getVersion() // Proteção de concorrência
        );

        if (rows == 0) {
            throw new SQLException("Falha ao atualizar usuário: Registro não encontrado ou erro de concorrência.");
        }
    }

    public List<Usuario> findAll(Connection conn, String tenantId) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY email ASC";
        return executeQuery(conn, sql, tenantId);
    }

    public Optional<Usuario> findByEmailETenant(Connection conn, String email, String tenantId) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE email = ? AND tenant_id = ? AND status = 'ACTIVE' AND deleted_at IS NULL";
        return executeQuerySingleEntity(conn, sql, email, tenantId);
    }

    public boolean softDeleteComUsuario(Connection conn, Long id, String tenantId, Long usuarioLogadoId) throws SQLException {
        String sql = """
            UPDATE usuario 
            SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? 
            WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL
        """;
        return executeUpdate(conn, sql, usuarioLogadoId, id, tenantId) > 0;
    }

    
    /**
     * Busca um usuário ativo pelo e-mail informado para o fluxo de login.
     */
    public Optional<Usuario> findByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE email = ? AND status = 'ACTIVE'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
            }
        }
        return Optional.empty();
    }
}

//package br.com.yat.ecosystemcore.repository.usuario;
//
//import br.com.yat.ecosystemcore.domain.entity.Usuario;
//import br.com.yat.ecosystemcore.repository.base.GenericDao;
//
//import java.sql.*;
//import java.util.Optional;
//
//public class UsuarioRepository extends GenericDao<Usuario, Long> {
//
//    public UsuarioRepository() {
//        super("usuario", "id");
//    }
//
//    @Override
//    protected Usuario mapResultSetToEntity(ResultSet rs) throws SQLException {
//        Usuario u = new Usuario();
//        u.setId(rs.getLong("id"));
//        u.setUuidPublico(rs.getString("uuid_publico"));
//        u.setTenantId(rs.getString("tenant_id"));
//        u.setPessoaId(rs.getLong("pessoa_id"));
//        u.setEmpresaPadraoId(rs.getObject("empresa_padrao_id") != null ? rs.getLong("empresa_padrao_id") : null);
//        u.setEmail(rs.getString("email"));
//        u.setSenhaHash(rs.getString("senha_hash"));
//        u.setTentativasLogin(rs.getInt("tentativas_login"));
//        u.setBloqueadoAte(readLocalDateTime(rs, "bloqueado_ate"));
//        u.setUltimoAcesso(readLocalDateTime(rs, "ultimo_acesso"));
//        u.setStatus(rs.getString("status"));
//        u.setVersion(rs.getInt("version"));
//        return u;
//    }
//
//    public Long insert(Connection conn, Usuario usuario) throws SQLException {
//        String sql = """
//            INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, empresa_padrao_id, email, senha_hash, status) 
//            VALUES (?, ?, ?, ?, ?, ?, ?)
//        """;
//        return executeInsertReturningId(conn, sql, 
//            usuario.getUuidPublico(),
//            usuario.getTenantId(),
//            usuario.getPessoaId(),
//            usuario.getEmpresaPadraoId(),
//            usuario.getEmail(),
//            usuario.getSenhaHash(),
//            usuario.getStatus() != null ? usuario.getStatus() : "ACTIVE"
//        );
//    }
//
//    /**
//     * Busca um usuário ativo pelo e-mail isolando estritamente pelo Tenant ID corporativo.
//     */
//    public Optional<Usuario> findByEmailETenant(Connection conn, String email, String tenantId) throws SQLException {
//        String sql = "SELECT * FROM usuario WHERE email = ? AND tenant_id = ? AND status = 'ACTIVE' AND deleted_at IS NULL";
//        return executeQuerySingleEntity(conn, sql, email, tenantId);
//    }
//    
//    /**
//     * Busca um usuário ativo pelo e-mail informado para o fluxo de login.
//     */
//    public Optional<Usuario> findByEmail(Connection conn, String email) throws SQLException {
//        String sql = "SELECT * FROM usuario WHERE email = ? AND status = 'ACTIVE'";
//        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setString(1, email);
//            try (ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    return Optional.of(mapResultSetToEntity(rs));
//                }
//            }
//        }
//        return Optional.empty();
//    }
//}