package br.com.yat.ecosystemcore.infrastructure.database;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DatabaseMenuSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMenuSeeder.class);
    private static final String TENANT_GLOBAL =
            "00000000-0000-0000-0000-000000000000";

    private static final String TENANT_GLOBAL_NOME =
            "Escopo Global do Sistema";

    private static final String TENANT_GLOBAL_PLANO =
            "ENTERPRISE";

    private static final String TENANT_GLOBAL_STATUS =
            "ACTIVE";

    private static class MenuFirma {
        final String moduloNome;
        final String menuNome;
        final MenuChave chaveEnum;
        final String permModulo;
        final String permAcao;
        final int ordem;

        MenuFirma(
                String moduloNome,
                String menuNome,
                MenuChave chaveEnum,
                String permModulo,
                String permAcao,
                int ordem
        ) {
            this.moduloNome = moduloNome;
            this.menuNome = menuNome;
            this.chaveEnum = chaveEnum;
            this.permModulo = permModulo;
            this.permAcao = permAcao;
            this.ordem = ordem;
        }
    }
    
    public static void inicializarCargaEstrutural(Connection conn) {
        logger.info("Sincronizando definições de menus com o banco de dados...");
        List<MenuFirma> catalogo = obterCatalogo();

        try {

            garantirTenantGlobal(conn);

            for (MenuFirma item : catalogo) {
                long moduloId = associarModulo(conn, item.moduloNome, item.ordem);
                long menuId = associarMenu(conn, moduloId, item.menuNome, item.chaveEnum.name(), item.ordem);
                long permissaoId = associarPermissao(conn, item.permModulo, item.permAcao, item.menuNome);
                vincularPermissaoMenu(conn, permissaoId, menuId);
            }

            logger.info("Carga de menus e permissões finalizada com sucesso.");

        } catch (SQLException e) {
            logger.error("Erro fatal durante o seed dos menus estruturais", e);
        }
    }
    
    private static void garantirTenantGlobal(Connection conn) throws SQLException {

        logger.info("Garantindo tenant global do sistema...");

        String sql =
        	    """
        	    INSERT INTO tenant
        	    (
        	        id,
        	        nome_conta,
        	        plano,
        	        status
        	    )
        	    VALUES
        	    (
        	        ?,
        	        ?,
        	        ?,
        	        ?
        	    )
        	    ON DUPLICATE KEY UPDATE
        	        nome_conta = VALUES(nome_conta),
        	        plano      = VALUES(plano),
        	        status     = VALUES(status)
        	    """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        	stmt.setString(1, TENANT_GLOBAL);
            stmt.setString(2, TENANT_GLOBAL_NOME);
            stmt.setString(3, TENANT_GLOBAL_PLANO);
            stmt.setString(4, TENANT_GLOBAL_STATUS);
            stmt.executeUpdate();
        }
    }
    private static long associarModulo(Connection conn, String nome, int ordem) throws SQLException {
        String query = "SELECT id FROM modulo_sistema WHERE nome = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }

        String insert =
        	    """
        	    INSERT INTO modulo_sistema
        	    (
        	        uuid_publico,
        	        nome,
        	        icone,
        	        ordem,
        	        ativo
        	    )
        	    VALUES
        	    (
        	        ?,
        	        ?,
        	        'default-icon',
        	        ?,
        	        1
        	    )
        	    ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    nome = VALUES(nome),
    icone = VALUES(icone),
    ordem = VALUES(ordem),
    ativo = VALUES(ativo)
        	    """;
        try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, nome);
            stmt.setInt(3, ordem);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Impossível criar módulo: " + nome);
    }

    private static long associarMenu(Connection conn, long moduloId, String nome, String acaoComando, int ordem) throws SQLException {
        String query = "SELECT id FROM menu_sistema WHERE modulo_id = ? AND nome = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, moduloId);
            stmt.setString(2, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }

        String insert =
        	    """
        	    INSERT INTO menu_sistema
        	    (
        	        uuid_publico,
        	        modulo_id,
        	        menu_pai_id,
        	        nome,
        	        acao_comando,
        	        ordem,
        	        ativo
        	    )
        	    VALUES
        	    (
        	        ?,
        	        ?,
        	        NULL,
        	        ?,
        	        ?,
        	        ?,
        	        1
        	    )
        	    ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    acao_comando = VALUES(acao_comando),
    ordem = VALUES(ordem),
    ativo = VALUES(ativo)
        	    """;
        try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setLong(2, moduloId);
            stmt.setString(3, nome);
            stmt.setString(4, acaoComando);
            stmt.setInt(5, ordem);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Impossível criar menu: " + nome);
    }

    private static long associarPermissao(Connection conn, String modulo, String acao, String desc) throws SQLException {

        String query =
            """
            SELECT id
              FROM permissao
             WHERE tenant_id = ?
               AND modulo = ?
               AND acao = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, TENANT_GLOBAL);
            stmt.setString(2, modulo);
            stmt.setString(3, acao);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        String insert =
        	    """
        	    INSERT INTO permissao
        	    (
        	        uuid_publico,
        	        tenant_id,
        	        modulo,
        	        acao,
        	        descricao
        	    )
        	    VALUES
        	    (
        	        ?,
        	        ?,
        	        ?,
        	        ?,
        	        ?
        	    )
        	    ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    descricao = VALUES(descricao),
    modulo = VALUES(modulo),
    acao = VALUES(acao)
        	    """;

        try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, TENANT_GLOBAL);
            stmt.setString(3, modulo);
            stmt.setString(4, acao);
            stmt.setString(5, "Acesso operacional à tela " + desc);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Impossível criar permissão para: " + modulo + ":" + acao);
    }

    private static void vincularPermissaoMenu(Connection conn, long permissaoId, long menuId) throws SQLException {
    	String insert =
    		    """
    		    INSERT INTO permissao_menu
    		    (
    		        permissao_id,
    		        menu_sistema_id
    		    )
    		    VALUES
    		    (
    		        ?,
    		        ?
    		    )
    		    ON DUPLICATE KEY UPDATE
    permissao_id = VALUES(permissao_id)
    		    """;
        try (PreparedStatement stmt = conn.prepareStatement(insert)) {
            stmt.setLong(1, permissaoId);
            stmt.setLong(2, menuId);
            stmt.executeUpdate();
        }
    }

    private static List<MenuFirma> obterCatalogo() {
        List<MenuFirma> lista = new ArrayList<>();
        
        lista.add(new MenuFirma("DASHBOARD", "Painel Principal", MenuChave.HOME, "dashboard", "visualizar", 10));
        
        lista.add(new MenuFirma("CADASTROS", "Empresas Clientes", MenuChave.CADASTROS_EMPRESA, "empresa", "visualizar", 20));
        lista.add(new MenuFirma("CADASTROS", "Clientes e Fornecedores", MenuChave.CADASTROS_PESSOA, "pessoa", "visualizar", 21));
        lista.add(new MenuFirma("CADASTROS", "Usuários do Sistema", MenuChave.CADASTROS_USUARIO, "usuario", "visualizar", 22));
        
        lista.add(new MenuFirma("SEGURANÇA", "Perfis de Acesso (RBAC)", MenuChave.SEGURANCA_PERFIL, "perfil", "visualizar", 30));
        lista.add(new MenuFirma("SEGURANÇA", "Permissões Ativas", MenuChave.SEGURANCA_PERMISSAO, "permissao", "visualizar", 31));
        
        // Corrigido para "CONFIGURAÇÕES" combinando com o controller
        lista.add(new MenuFirma("CONFIGURAÇÕES", "Configuração da Conta", MenuChave.ADMIN_TENANT_CONFIG, "tenant", "configurar", 40));
        lista.add(new MenuFirma("CONFIGURAÇÕES", "Parâmetros do Sistema", MenuChave.ADMIN_PARAMETROS, "parametro", "configurar", 41));
        lista.add(new MenuFirma("CONFIGURAÇÕES", "Configuração do Banco", MenuChave.CONFIGURACAO_BANCO, "banco", "configurar", 42));
        
        lista.add(new MenuFirma("AUDITORIA", "Rastreador de Logs", MenuChave.AUDITORIA_LOGS, "auditoria", "logs", 50));
        lista.add(new MenuFirma("AUDITORIA", "Agendador de Tarefas", MenuChave.AUDITORIA_JOBS, "auditoria", "jobs", 51));

        lista.add(new MenuFirma("FINANCEIRO", "Fluxo de Caixa", MenuChave.FINANCEIRO_FLUXO, "financeiro", "visualizar", 60));

        return lista;
    }
}