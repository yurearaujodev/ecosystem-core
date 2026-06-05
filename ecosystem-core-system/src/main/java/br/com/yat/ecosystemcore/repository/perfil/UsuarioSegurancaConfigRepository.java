package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.application.system.dto.UsuarioSegurancaConfigDTO;
import br.com.yat.ecosystemcore.repository.base.GenericDao;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioSegurancaConfigRepository extends GenericDao<UsuarioSegurancaConfigDTO, Long> {

    public UsuarioSegurancaConfigRepository() {
        super("usuario_seguranca_config", "usuario_id");
    }

    @Override
    protected UsuarioSegurancaConfigDTO mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new UsuarioSegurancaConfigDTO(
            rs.getBoolean("requer_nova_senha"),
            rs.getBoolean("aceita_acesso_fora_empresa"),
            rs.getString("ip_estatico_obrigatorio"),
            rs.getBoolean("permitir_multiplas_sessoes")
        );
    }

    public UsuarioSegurancaConfigDTO buscarPorUsuario(Connection conn, Long usuarioId) throws SQLException {
        String sql = "SELECT * FROM usuario_seguranca_config WHERE usuario_id = ?";
        
        // 🌟 Reduzido a uma única linha usando o mapeador genérico da classe mãe!
        Optional<UsuarioSegurancaConfigDTO> config = executeQuerySingleEntity(conn, sql, usuarioId);
        
        return config.orElseGet(() -> new UsuarioSegurancaConfigDTO(false, true, null, false));
    }

    public void salvarOuAtualizar(Connection conn, Long usuarioId, String tenantId, UsuarioSegurancaConfigDTO dto) throws SQLException {
        String sql = """
            INSERT INTO usuario_seguranca_config 
            (usuario_id, tenant_id, requer_nova_senha, aceita_acesso_fora_empresa, ip_estatico_obrigatorio, permitir_multiplas_sessoes)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                requer_nova_senha = VALUES(requer_nova_senha),
                aceita_acesso_fora_empresa = VALUES(aceita_acesso_fora_empresa),
                ip_estatico_obrigatorio = VALUES(ip_estatico_obrigatorio),
                permitir_multiplas_sessoes = VALUES(permitir_multiplas_sessoes)
        """;
        
        // 🌟 Executa o insert/update limpando o boiler plate de setBoolean, setString, etc.
        executeInsert(conn, sql, 
            usuarioId, 
            tenantId, 
            dto.requerNovaSenha(), 
            dto.aceitaAcessoForaEmpresa(), 
            dto.ipEstaticoObrigatorio(), 
            dto.permitirMultiplasSessoes()
        );
    }
}