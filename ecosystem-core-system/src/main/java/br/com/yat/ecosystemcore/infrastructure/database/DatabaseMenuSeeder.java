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
	private static final String TENANT_GLOBAL = "00000000-0000-0000-0000-000000000000";
	private static final String TENANT_GLOBAL_NOME = "Escopo Global do Sistema";
	private static final String TENANT_GLOBAL_PLANO = "ENTERPRISE";
	private static final String TENANT_GLOBAL_STATUS = "ACTIVE";

	// Matriz de ações padrão do ecossistema RBAC
	private static final String[] CRUD_COMPLETO = { "visualizar", "cadastrar", "editar", "deletar" };
	private static final String[] APENAS_LEITURA = { "consultar" };
	private static final String[] TELA_CONFIG = { "visualizar", "configurar" };

	private static class MenuFirma {
		final String moduloNome;
		final String menuNome;
		final MenuChave chaveEnum;
		final String permModulo;
		final String[] acoesPossiveis; // 🔥 Agora aceita múltiplos escopos de ação para RBAC real
		final int ordem;

		MenuFirma(String moduloNome, String menuNome, MenuChave chaveEnum, String permModulo, String[] acoesPossiveis,
				int ordem) {
			this.moduloNome = moduloNome;
			this.menuNome = menuNome;
			this.chaveEnum = chaveEnum;
			this.permModulo = permModulo;
			this.acoesPossiveis = acoesPossiveis;
			this.ordem = ordem;
		}
	}

	public static void inicializarCargaEstrutural(Connection conn) {
		logger.info("Sincronizando matriz de RBAC e definições de menus com o banco...");
		List<MenuFirma> catalogo = obterCatalogo();

		try {
			garantirTenantGlobal(conn);

			for (MenuFirma item : catalogo) {
				long moduloId = associarModulo(conn, item.moduloNome, item.ordem);
				long menuId = associarMenu(conn, moduloId, item.menuNome, item.chaveEnum.name(), item.ordem);

				// 🔥 Laço que desmembra as ações na tabela de permissões de forma granular
				for (String acao : item.acoesPossiveis) {
					long permissaoId = associarPermissao(conn, item.permModulo, acao, item.menuNome);
					vincularPermissaoMenu(conn, permissaoId, menuId);
				}
			}

			logger.info("Carga de segurança RBAC e menus finalizada com sucesso.");

		} catch (SQLException e) {
			logger.error("Erro fatal durante o seed dos menus estruturais", e);
		}
	}

	private static void garantirTenantGlobal(Connection conn) throws SQLException {
		String sql = """
				    INSERT INTO tenant (id, nome_conta, plano, status) VALUES (?, ?, ?, ?)
				    ON DUPLICATE KEY UPDATE nome_conta = VALUES(nome_conta), plano = VALUES(plano), status = VALUES(status)
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
				if (rs.next())
					return rs.getLong("id");
			}
		}

		String insert = """
				    INSERT INTO modulo_sistema (uuid_publico, nome, icone, ordem, ativo) VALUES (?, ?, 'default-icon', ?, 1)
				    ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id), nome = VALUES(nome), ordem = VALUES(ordem)
				""";
		try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, UUID.randomUUID().toString());
			stmt.setString(2, nome);
			stmt.setInt(3, ordem);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next())
					return rs.getLong(1);
			}
		}
		throw new SQLException("Impossível criar módulo: " + nome);
	}

	private static long associarMenu(Connection conn, long moduloId, String nome, String acaoComando, int ordem)
			throws SQLException {
		String query = "SELECT id FROM menu_sistema WHERE modulo_id = ? AND nome = ?";
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setLong(1, moduloId);
			stmt.setString(2, nome);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next())
					return rs.getLong("id");
			}
		}

		String insert = """
				    INSERT INTO menu_sistema (uuid_publico, modulo_id, menu_pai_id, nome, acao_comando, ordem, ativo) VALUES (?, ?, NULL, ?, ?, ?, 1)
				    ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id), acao_comando = VALUES(acao_comando), ordem = VALUES(ordem)
				""";
		try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, UUID.randomUUID().toString());
			stmt.setLong(2, moduloId);
			stmt.setString(3, nome);
			stmt.setString(4, acaoComando);
			stmt.setInt(5, ordem);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next())
					return rs.getLong(1);
			}
		}
		throw new SQLException("Impossível criar menu: " + nome);
	}

	private static long associarPermissao(Connection conn, String modulo, String acao, String desc)
			throws SQLException {
		String query = "SELECT id FROM permissao WHERE tenant_id = ? AND modulo = ? AND acao = ?";
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, TENANT_GLOBAL);
			stmt.setString(2, modulo);
			stmt.setString(3, acao);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next())
					return rs.getLong("id");
			}
		}

		String insert = """
				    INSERT INTO permissao (uuid_publico, tenant_id, modulo, acao, descricao) VALUES (?, ?, ?, ?, ?)
				    ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id), descricao = VALUES(descricao)
				""";
		try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, UUID.randomUUID().toString());
			stmt.setString(2, TENANT_GLOBAL);
			stmt.setString(3, modulo);
			stmt.setString(4, acao);
			stmt.setString(5, "Permissão para " + acao.toUpperCase() + " no recurso " + desc);
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next())
					return rs.getLong(1);
			}
		}
		throw new SQLException("Impossível criar permissão para: " + modulo + ":" + acao);
	}

	private static void vincularPermissaoMenu(Connection conn, long permissaoId, long menuId) throws SQLException {
		String insert = """
				    INSERT INTO permissao_menu (permissao_id, menu_sistema_id) VALUES (?, ?)
				    ON DUPLICATE KEY UPDATE permissao_id = VALUES(permissao_id)
				""";
		try (PreparedStatement stmt = conn.prepareStatement(insert)) {
			stmt.setLong(1, permissaoId);
			stmt.setLong(2, menuId);
			stmt.executeUpdate();
		}
	}

	private static List<MenuFirma> obterCatalogo() {
		List<MenuFirma> lista = new ArrayList<>();

		// 🏠 MÓDULO: HOME
		lista.add(new MenuFirma("🏠 HOME", "Dashboard", MenuChave.DASHBOARD, "dashboard", APENAS_LEITURA, 10));

		// 👥 MÓDULO: CADASTROS (CRUD Completo liberado para controle refinado)
		lista.add(new MenuFirma("👥 CADASTROS", "Empresas", MenuChave.CADASTROS_EMPRESAS, "empresa", CRUD_COMPLETO, 20));
		lista.add(new MenuFirma("👥 CADASTROS", "Pessoas", MenuChave.CADASTROS_PESSOAS, "pessoa", CRUD_COMPLETO, 21));
		lista.add(new MenuFirma("👥 CADASTROS", "Usuários", MenuChave.CADASTROS_USUARIOS, "usuario", CRUD_COMPLETO, 22));

		// 🔐 MÓDULO: SEGURANÇA
		lista.add(new MenuFirma("🔐 SEGURANÇA", "Perfis", MenuChave.SEGURANCA_PERFIS, "perfil", CRUD_COMPLETO, 30));
		lista.add(new MenuFirma("🔐 SEGURANÇA", "Permissões", MenuChave.SEGURANCA_PERMISSOES, "permissao", CRUD_COMPLETO, 31));
		lista.add(new MenuFirma("🔐 SEGURANÇA", "Gerenciar Usuários", MenuChave.SEGURANCA_ABA_USUARIO_DETALHE, "usuario_seguranca", TELA_CONFIG, 32));
		lista.add(new MenuFirma("🔐 SEGURANÇA", "MFA", MenuChave.SEGURANCA_MFA, "mfa", TELA_CONFIG, 33));
		lista.add(new MenuFirma("🔐 SEGURANÇA", "Sessões e Tentativas", MenuChave.SEGURANCA_ABA_SESSÕES_TENTATIVAS, "monitoramento", APENAS_LEITURA, 34));

		// 🏢 MÓDULO: ADMINISTRAÇÃO
		lista.add(new MenuFirma("🏢 ADMINISTRAÇÃO", "Tenants", MenuChave.ADMIN_TENANTS, "tenant", CRUD_COMPLETO, 40));
		lista.add(new MenuFirma("🏢 ADMINISTRAÇÃO", "Configurações Tenant", MenuChave.ADMIN_ABA_CONFIG_TENANT,"tenant_config", TELA_CONFIG, 41));
		lista.add(new MenuFirma("🏢 ADMINISTRAÇÃO", "Módulos", MenuChave.ADMIN_MODULOS, "modulo", CRUD_COMPLETO, 42));
		lista.add(new MenuFirma("🏢 ADMINISTRAÇÃO", "Menus", MenuChave.ADMIN_MENUS, "menu", CRUD_COMPLETO, 43));
		lista.add(new MenuFirma("🏢 ADMINISTRAÇÃO", "Tenant x Menus", MenuChave.ADMIN_TENANT_MENUS, "tenant_menu",TELA_CONFIG, 44));

		// 🔑 MÓDULO: LICENCIAMENTO
		lista.add(new MenuFirma("🔑 LICENCIAMENTO", "Aplicativos", MenuChave.LICENCAS_APLICATIVOS, "licenca_aplicativo", CRUD_COMPLETO, 45));
		lista.add(new MenuFirma("🔑 LICENCIAMENTO", "Emitir e Gerenciar", MenuChave.LICENCAS_GERENCIAMENTO, "licenca", CRUD_COMPLETO, 46));
		lista.add(new MenuFirma("🔑 LICENCIAMENTO", "Dispositivos Ativos", MenuChave.LICENCAS_DISPOSITIVOS, "ativacao_dispositivo", CRUD_COMPLETO, 47));
		lista.add(new MenuFirma("🔑 LICENCIAMENTO", "Histórico de Revogações", MenuChave.LICENCAS_REVOGACOES, "revogacao_licenca", APENAS_LEITURA, 48));

		// 🔔 MÓDULO: COMUNICAÇÃO
		lista.add(new MenuFirma("🔔 COMUNICAÇÃO", "Notificações", MenuChave.COMUNICACAO_NOTIFICACOES, "notificacao", CRUD_COMPLETO, 50));

		// 📁 MÓDULO: DOCUMENTOS
		lista.add(new MenuFirma("📁 DOCUMENTOS", "Arquivos", MenuChave.DOCUMENTOS_ARQUIVOS, "arquivo", CRUD_COMPLETO, 60));

		// 📜 MÓDULO: AUDITORIA
		lista.add(new MenuFirma("📜 AUDITORIA", "Logs e Auditoria", MenuChave.AUDITORIA_ABA_PRINCIPAL, "auditoria",	APENAS_LEITURA, 70));

		// ⚙ MÓDULO: SISTEMA
		lista.add(new MenuFirma("⚙ SISTEMA", "Configurações Gerais", MenuChave.SISTEMA_CONFIGURACOES, "sistema", TELA_CONFIG, 80));
		lista.add(new MenuFirma("⚙ SISTEMA", "Jobs", MenuChave.SISTEMA_JOBS, "jobs", APENAS_LEITURA, 81));
		lista.add(new MenuFirma("⚙ SISTEMA", "Outbox Events", MenuChave.SISTEMA_OUTBOX_EVENTS, "outbox", APENAS_LEITURA, 82));
		lista.add(new MenuFirma("⚙ SISTEMA", "Versão do Schema", MenuChave.SISTEMA_SCHEMA_VERSAO, "schema", 	APENAS_LEITURA, 83));
		lista.add(new MenuFirma("⚙ SISTEMA", "Configuração Banco", MenuChave.CONFIGURACAO_BANCO, "banco", TELA_CONFIG, 84));

		// 💰 MÓDULO: FINANCEIRO
		lista.add(new MenuFirma("💰 FINANCEIRO", "Fluxo de Caixa", MenuChave.FINANCEIRO_FLUXO, "financeiro", 	CRUD_COMPLETO, 90));

		return lista;
	}
}