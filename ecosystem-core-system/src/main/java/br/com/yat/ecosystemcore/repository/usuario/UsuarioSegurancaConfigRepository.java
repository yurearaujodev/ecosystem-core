package br.com.yat.ecosystemcore.repository.usuario;

import br.com.yat.ecosystemcore.domain.entity.UsuarioSegurancaConfig;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}