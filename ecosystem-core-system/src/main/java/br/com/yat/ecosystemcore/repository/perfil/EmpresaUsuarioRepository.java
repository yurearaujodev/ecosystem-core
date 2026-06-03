package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuario;
import br.com.yat.ecosystemcore.repository.base.GenericDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpresaUsuarioRepository extends GenericDao<EmpresaUsuario, String> {

    public EmpresaUsuarioRepository() {
        super("empresa_usuario", null);
    }

    @Override
    protected EmpresaUsuario mapResultSetToEntity(ResultSet rs) throws SQLException {
        EmpresaUsuario e = new EmpresaUsuario();
        e.setTenantId(rs.getString("tenant_id"));
        e.setEmpresaId(rs.getLong("empresa_id"));
        e.setUsuarioId(rs.getLong("usuario_id"));
        e.setPerfilId(rs.getLong("perfil_id"));
        return e;
    }

    public void vincular(Connection conn,
                         String tenantId,
                         Long empresaId,
                         Long usuarioId,
                         Long perfilId) throws SQLException {

        String sql = """
            INSERT INTO empresa_usuario (tenant_id, empresa_id, usuario_id, perfil_id)
            VALUES (?, ?, ?, ?)
        """;

        executeInsert(conn, sql, tenantId, empresaId, usuarioId, perfilId);
    }
}