package br.com.yat.ecosystemcore.repository.usuario;

import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.*;
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
        u.setPessoaId(rs.getLong("pessoa_id"));          // Unificado: essencial para o fluxo
        u.setEmpresaPadraoId(rs.getLong("empresa_padrao_id")); // Unificado: essencial para a sessão
        u.setEmail(rs.getString("email"));
        u.setSenhaHash(rs.getString("senha_hash"));
        u.setStatus(rs.getString("status"));
        return u;
    }

    /**
     * Insere um novo usuário gerando a chave primária de forma automatizada pelo banco.
     */
    public Long insert(Connection conn, Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (uuid_publico, tenant_id, pessoa_id, empresa_padrao_id, email, senha_hash, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario.getUuidPublico());
            stmt.setString(2, usuario.getTenantId());
            stmt.setLong(3, usuario.getPessoaId());
            
            if (usuario.getEmpresaPadraoId() != null) {
                stmt.setLong(4, usuario.getEmpresaPadraoId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            
            stmt.setString(5, usuario.getEmail());
            stmt.setString(6, usuario.getSenhaHash());
            stmt.setString(7, usuario.getStatus() != null ? usuario.getStatus() : "ACTIVE");
            
            stmt.executeUpdate();
            
            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) return gk.getLong(1);
                throw new SQLException("Erro ao obter ID gerado para Usuário.");
            }
        }
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
