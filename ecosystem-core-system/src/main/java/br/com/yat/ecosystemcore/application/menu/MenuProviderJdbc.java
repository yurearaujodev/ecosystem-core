package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MenuProviderJdbc implements MenuProvider {

    @Override
    public List<MenuPermitidoDTO> carregarMenus(MenuUsuarioContext context) {
        List<MenuPermitidoDTO> itens = new ArrayList<>();

        // 🛡️ 1. Verifica se o usuário atual é o Admin Mestre do Tenant
        // (Geralmente o primeiro usuário criado possui ID 1, ou você pode checar se o perfil dele é root)
        boolean IS_ADMIN_MESTRE = (context.usuarioId() != null && context.usuarioId() == 1);

        String sql;

        if (IS_ADMIN_MESTRE) {
            // 🚀 GOD MODE: Se for o Admin Mestre, traz todos os menus e módulos ativos mapeados no banco
            sql = """
                SELECT 
                    ms.nome AS modulo_nome, 
                    ms.icone AS modulo_icone, 
                    ms.ordem AS modulo_ordem,
                    men.id AS menu_id, 
                    men.nome AS menu_nome, 
                    men.acao_comando AS acao_comando, 
                    men.ordem AS menu_ordem
                FROM menu_sistema men
                INNER JOIN modulo_sistema ms ON men.modulo_id = ms.id
                WHERE men.ativo = true 
                  AND ms.ativo = true
            """;
        } else {
            // 👥 FLUXO COMUM (RBAC): Para usuários normais, passa pelo crivo rigoroso de permissões
            sql = """
                SELECT 
                    ms.nome AS modulo_nome, 
                    ms.icone AS modulo_icone, 
                    ms.ordem AS modulo_ordem,
                    men.id AS menu_id, 
                    men.nome AS menu_nome, 
                    men.acao_comando AS acao_comando, 
                    men.ordem AS menu_ordem
                FROM menu_sistema men
                INNER JOIN modulo_sistema ms ON men.modulo_id = ms.id
                INNER JOIN permissao_menu pm ON men.id = pm.menu_sistema_id
                INNER JOIN perfil_permissao pp ON pm.permissao_id = pp.permissao_id
                INNER JOIN empresa_usuario eu ON pp.perfil_id = eu.perfil_id
                WHERE eu.usuario_id = ? 
                  AND eu.tenant_id = ? 
                  AND men.ativo = true 
                  AND ms.ativo = true
            """;
        }

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Só injeta os parâmetros se NÃO for o admin mestre (evita index out of bounds)
            if (!IS_ADMIN_MESTRE) {
                stmt.setLong(1, context.usuarioId());
                stmt.setString(2, context.tenantId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String acao = rs.getString("acao_comando");
                    
                    try {
                        MenuChave chave = MenuChave.valueOf(acao);
                        
                        itens.add(new MenuPermitidoDTO(
                                rs.getString("modulo_nome"),
                                rs.getString("modulo_icone"),
                                rs.getInt("modulo_ordem"),
                                rs.getLong("menu_id"),
                                rs.getString("menu_nome"),
                                chave.name(),
                                rs.getInt("menu_ordem")
                        ));
                    } catch (IllegalArgumentException e) {
                        // Silencioso para enums não mapeados
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar menu dinâmico do banco: " + e.getMessage());
        }

        return MenuPermitidoDTO.ordenarParaExibicao(itens);
    }
}