package br.com.yat.ecosystemcore.repository.perfil;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuario;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.repository.base.GenericDao;
import br.com.yat.ecosystemcore.ui.modules.perfil.UsuarioGerenciamentoAbasController.CheckBoxListCellData;
import br.com.yat.ecosystemcore.ui.modules.perfil.UsuarioGerenciamentoAbasController.CustomItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 🔥 NOVO MÉTODO ACOPLADO À SUA IDEIA:
     * Carrega a visão detalhe realizando a junção de tabelas (JOIN) exigida 
     * pela tabela reativa do JavaFX, isolando por Tenant e Usuário Ativo.
     */
    public List<EmpresaUsuarioDetalheDTO> findVinculosPorUsuario(Connection conn, Long usuarioId, String tenantId) throws SQLException {
        String sql = """
            SELECT eu.empresa_id, e.nome_fantasia AS empresa_nome, eu.perfil_id, p.nome AS perfil_nome
            FROM empresa_usuario eu
            INNER JOIN empresa e ON e.id = eu.empresa_id
            INNER JOIN perfil p ON p.id = eu.perfil_id
            WHERE eu.usuario_id = ? AND eu.tenant_id = ? AND e.deleted_at IS NULL
        """;
        
        List<EmpresaUsuarioDetalheDTO> lista = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, usuarioId);
            stmt.setString(2, tenantId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new EmpresaUsuarioDetalheDTO(
                        rs.getLong("empresa_id"),
                        rs.getString("empresa_nome"),
                        rs.getLong("perfil_id"),
                        rs.getString("perfil_nome")
                    ));
                }
            }
        }
        return lista;
    }
    
    public void removerVinculo(Connection conn, String tenantId, Long empresaId, Long usuarioId) throws SQLException {
        String sql = "DELETE FROM empresa_usuario WHERE tenant_id = ? AND empresa_id = ? AND usuario_id = ?";
        executeUpdate(conn, sql, tenantId, empresaId, usuarioId);
    }
    


    public List<CustomItem> findAllUsersAsCustomItems(Connection conn, String tenantId) throws SQLException {
    	System.out.println("DEBUG: Buscando usuários para o Tenant ID: " + tenantId);
        String sql = "SELECT id, email FROM usuario WHERE tenant_id = ? AND status = 'ACTIVE'";
        List<CustomItem> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CustomItem(rs.getLong("id"), rs.getString("email")));
                }
            }
        }
        return lista;
    }

    public List<CustomItem> findAllEmpresas(Connection conn, String tenantId) throws SQLException {
        String sql = "SELECT id, nome_fantasia FROM empresa WHERE tenant_id = ? AND deleted_at IS NULL";
        List<CustomItem> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CustomItem(rs.getLong("id"), rs.getString("nome_fantasia")));
                }
            }
        }
        return lista;
    }

    public List<CustomItem> findAllPerfis(Connection conn) throws SQLException {
        String sql = "SELECT id, nome FROM perfil"; // Ajuste se precisar filtrar por tenant
        List<CustomItem> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new CustomItem(rs.getLong("id"), rs.getString("nome")));
            }
        }
        return lista;
    }

    public List<CheckBoxListCellData> findAllPermissoes(Connection conn) throws SQLException {
        // CORREÇÃO: O banco não tem 'chave', tem 'chave_composta'
        String sql = "SELECT id, chave_composta, descricao FROM permissao";
        List<CheckBoxListCellData> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new CheckBoxListCellData(
                    rs.getLong("id"), 
                    rs.getString("chave_composta"), // Use o nome exato do banco
                    rs.getString("descricao")
                ));
            }
        }
        return lista;
    }
}