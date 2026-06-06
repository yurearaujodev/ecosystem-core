package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;

import java.util.ArrayList;
import java.util.List;

/**
 * Provisório até existir consulta JDBC. Espelha o catálogo semeado em
 * {@code DatabaseMenuSeeder}.
 */
public final class MenuProviderStub implements MenuProvider {

	@Override
	public List<MenuPermitidoDTO> carregarMenus(MenuUsuarioContext context) {
		List<MenuPermitidoDTO> itens = new ArrayList<>();

		// 🏠 MÓDULO: HOME
		itens.add(item("🏠 Dashboard", null, 5, null, "Dashboard", MenuChave.DASHBOARD, 10));

		// 👥 MÓDULO: CADASTROS
		itens.add(item("👥 Cadastros", null, 10, null, "Empresas", MenuChave.CADASTROS_EMPRESAS, 10));
		itens.add(item("👥 Cadastros", null, 10, null, "Pessoas", MenuChave.CADASTROS_PESSOAS, 20));
		itens.add(item("👥 Cadastros", null, 10, null, "Usuários", MenuChave.CADASTROS_USUARIOS, 30));

		// 🔐 MÓDULO: SEGURANÇA
		itens.add(item("🔐 Segurança", null, 20, null, "Perfis", MenuChave.SEGURANCA_PERFIS, 10));
		itens.add(item("🔐 Segurança", null, 20, null, "Permissões", MenuChave.SEGURANCA_PERMISSOES, 20));
		itens.add(item("🔐 Segurança", null, 20, null, "Gerenciamento de Usuários", MenuChave.SEGURANCA_ABA_USUARIO_DETALHE, 30));
		itens.add(item("🔐 Segurança", null, 20, null, "MFA", MenuChave.SEGURANCA_MFA, 40));
		itens.add(item("🔐 Segurança", null, 20, null, "Sessões e Tentativas", MenuChave.SEGURANCA_ABA_SESSÕES_TENTATIVAS, 50));

		// 🏢 MÓDULO: ADMINISTRAÇÃO
		itens.add(item("🏢 Administração", null, 30, null, "Tenants", MenuChave.ADMIN_TENANTS, 10));
		itens.add(item("🏢 Administração", null, 30, null, "Configurações Tenant", MenuChave.ADMIN_ABA_CONFIG_TENANT, 20));
		itens.add(item("🏢 Administração", null, 30, null, "Módulos", MenuChave.ADMIN_MODULOS, 30));
		itens.add(item("🏢 Administração", null, 30, null, "Menus", MenuChave.ADMIN_MENUS, 40));
		itens.add(item("🏢 Administração", null, 30, null, "Tenant x Menus", MenuChave.ADMIN_TENANT_MENUS, 50));

		// 🔑 MÓDULO: LICENCIAMENTO
		itens.add(item("🔑 Licenciamento", null, 40, null, "Aplicativos", MenuChave.LICENCAS_APLICATIVOS, 10));
		itens.add(item("🔑 Licenciamento", null, 40, null, "Emitir e Gerenciar", MenuChave.LICENCAS_GERENCIAMENTO, 20));
		itens.add(item("🔑 Licenciamento", null, 40, null, "Dispositivos Ativos", MenuChave.LICENCAS_DISPOSITIVOS, 30));
		itens.add(item("🔑 Licenciamento", null, 40, null, "Histórico de Revogações", MenuChave.LICENCAS_REVOGACOES, 40));

		// 🔔 MÓDULO: COMUNICAÇÃO
		itens.add(item("🔔 Comunicação", null, 50, null, "Notificações", MenuChave.COMUNICACAO_NOTIFICACOES, 10));

		// 📁 MÓDULO: DOCUMENTOS
		itens.add(item("📁 Documentos", null, 50, null, "Arquivos", MenuChave.DOCUMENTOS_ARQUIVOS, 10));

		// 📜 MÓDULO: AUDITORIA
		itens.add(item("📜 Auditoria", null, 60, null, "Logs e Auditoria", MenuChave.AUDITORIA_ABA_PRINCIPAL, 10));

		// ⚙ MÓDULO: SISTEMA
		itens.add(item("⚙ Sistema", null, 70, null, "Configurações Gerais", MenuChave.SISTEMA_CONFIGURACOES, 10));
		itens.add(item("⚙ Sistema", null, 70, null, "Jobs", MenuChave.SISTEMA_JOBS, 20));
		itens.add(item("⚙ Sistema", null, 70, null, "Outbox Events", MenuChave.SISTEMA_OUTBOX_EVENTS, 30));
		itens.add(item("⚙ Sistema", null, 70, null, "Versão do Schema", MenuChave.SISTEMA_SCHEMA_VERSAO, 40));

		// CONFIGURAÇÕES ADICIONAIS / FLUXOS
		itens.add(item("⚙ Sistema", null, 70, null, "Configuração Banco", MenuChave.CONFIGURACAO_BANCO, 50));
		itens.add(item("💰 Financeiro", null, 80, null, "Fluxo de Caixa", MenuChave.FINANCEIRO_FLUXO, 10));

		return MenuPermitidoDTO.ordenarParaExibicao(itens);
	}

	private static MenuPermitidoDTO item(String moduloNome, String moduloIcone, int moduloOrdem, Long menuSistemaId,
			String menuNome, MenuChave chave, int menuOrdem) {
		return new MenuPermitidoDTO(moduloNome, moduloIcone, moduloOrdem, menuSistemaId, menuNome, chave.name(),
				menuOrdem);
	}
}