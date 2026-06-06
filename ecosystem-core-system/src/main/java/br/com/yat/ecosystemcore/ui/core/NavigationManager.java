package br.com.yat.ecosystemcore.ui.core;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.ui.modules.banco.ConfiguracaoBancoController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationManager implements Navigator {

    private static final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
    private static final int LIMITE_MAX_CACHE = 15;

    private final StackPane containerCentral;
    private final Map<MenuChave, String> rotas = new EnumMap<>(MenuChave.class);
    private final LinkedHashMap<String, ScreenNode> cacheTelas;

    private ScreenNode telaAtual;

    public NavigationManager(StackPane containerCentral) {
        this.containerCentral = containerCentral;
        this.cacheTelas = criarCacheLru();
        configurarRotas();
    }

    private LinkedHashMap<String, ScreenNode> criarCacheLru() {
        return new LinkedHashMap<>(LIMITE_MAX_CACHE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ScreenNode> eldest) {
                if (size() <= LIMITE_MAX_CACHE) {
                    return false;
                }
                destroyScreen(eldest.getValue());
                logger.debug("Cache LRU evict: {}", eldest.getKey());
                return true;
            }
        };
    }

    private void configurarRotas() {
        // 🏠 Home
 //       rotas.put(MenuChave.DASHBOARD, "/ui/modules/dashboard/DashboardView.fxml");
        rotas.put(MenuChave.DASHBOARD, "/ui/modules/home.fxml");

        // 👥 Cadastros (Consulta + Novo)
        rotas.put(MenuChave.CADASTROS_EMPRESAS, "/ui/modules/empresa-consulta-view.fxml");
        rotas.put(MenuChave.CADASTROS_PESSOAS, "/ui/modules/pessoa-lista.fxml");
        rotas.put(MenuChave.CADASTROS_USUARIOS, "/ui/modules/usuario-gerenciamento.fxml");

        // 🔐 Segurança
        rotas.put(MenuChave.SEGURANCA_PERFIS, "/ui/modules/perfil-consulta-view.fxml");
        rotas.put(MenuChave.SEGURANCA_PERMISSOES, "/ui/modules/permissao-consulta-view.fxml");
        rotas.put(MenuChave.SEGURANCA_ABA_USUARIO_DETALHE, "/ui/modules/usuario-gerenciamento-abas-view.fxml"); // (Usuário x Empresa, Permissões Extras)
        rotas.put(MenuChave.SEGURANCA_MFA, "/ui/modules/mfa-config-view.fxml");
        rotas.put(MenuChave.SEGURANCA_ABA_SESSÕES_TENTATIVAS, "/ui/modules/monitoramento-sessoes-abas-view.fxml"); // (Sessões, Tentativas, Dispositivos)

        // 🏢 Administração
        rotas.put(MenuChave.ADMIN_TENANTS, "/ui/modules/administracao/TenantConsultaView.fxml");
        rotas.put(MenuChave.ADMIN_ABA_CONFIG_TENANT, "/ui/modules/administracao/ConfiguracaoTenantAbasView.fxml"); // (Configs, Features, Parâmetros)
        rotas.put(MenuChave.ADMIN_MODULOS, "/ui/modules/administracao/ModuloConsultaView.fxml");
        rotas.put(MenuChave.ADMIN_MENUS, "/ui/modules/administracao/MenuConsultaView.fxml");
        rotas.put(MenuChave.ADMIN_TENANT_MENUS, "/ui/modules/administracao/TenantMenusView.fxml");
        
        // 🔑 Licenciamento (Refatorado focado exclusivamente no seu modelo de 5 tabelas)
        rotas.put(MenuChave.LICENCAS_APLICATIVOS, "/ui/modules/licenciamento/LicencaAplicativoConsultaView.fxml");
        rotas.put(MenuChave.LICENCAS_GERENCIAMENTO, "/ui/modules/licenciamento/LicencaGerenciamentoAbasView.fxml"); // Agrupa os dados de 'licenca' com o JSON de 'licenca_payload'
        rotas.put(MenuChave.LICENCAS_DISPOSITIVOS, "/ui/modules/licenciamento/LicencaDispositivosView.fxml");
        rotas.put(MenuChave.LICENCAS_REVOGACOES, "/ui/modules/licenciamento/LicencaRevogacaoHistoricoView.fxml");

        // 🔔 Comunicação
        rotas.put(MenuChave.COMUNICACAO_NOTIFICACOES, "/ui/modules/comunicacao/NotificacoesView.fxml");

        // 📁 Documentos
        rotas.put(MenuChave.DOCUMENTOS_ARQUIVOS, "/ui/modules/documentos/ArquivosView.fxml");

        // 📜 Auditoria
        rotas.put(MenuChave.AUDITORIA_ABA_PRINCIPAL, "/ui/modules/auditoria/AuditoriaAbasView.fxml"); // (Logs e Configuração Auditoria)

        // ⚙ Sistema (Somente Consulta)
        rotas.put(MenuChave.SISTEMA_CONFIGURACOES, "/ui/modules/sistema-configuracoes-view.fxml");
        rotas.put(MenuChave.SISTEMA_JOBS, "/ui/modules/sistema-jobs-view.fxml");
        rotas.put(MenuChave.SISTEMA_OUTBOX_EVENTS, "/ui/modules/sistema-outbox-events-view.fxml");
        rotas.put(MenuChave.SISTEMA_SCHEMA_VERSAO, "/ui/modules/sistema-schema-versao-view.fxml");

        // Rotas Legadas / Auxiliares
        rotas.put(MenuChave.CONFIGURACAO_BANCO, "/ui/modules/configuracao-banco.fxml");
        rotas.put(MenuChave.FINANCEIRO_FLUXO, "/ui/modules/financeiro/FluxoCaixaView.fxml");
    }

    @Override
    public void navigatePara(MenuChave chave) {
        String pathFxml = rotas.get(chave);

        if (pathFxml == null) {
            logger.warn("Rota não encontrada: {}", chave);
            return;
        }

        try {
            if (telaAtual != null) {
                invokeHide(telaAtual);
            }

            ScreenNode novaTela = obterOuCarregar(pathFxml);
            containerCentral.getChildren().setAll(novaTela.getView());
            telaAtual = novaTela;

            invokeShow(novaTela);
        } catch (Exception e) {
            logger.error("Falha na navegação para {}", chave, e);
        }
    }

    /**
     * Obtém do cache (atualizando ordem LRU) ou carrega a tela pela primeira vez.
     */
    private ScreenNode obterOuCarregar(String pathFxml) throws IOException {
        ScreenNode cached = cacheTelas.remove(pathFxml);
        if (cached == null) {
            cached = carregarTela(pathFxml);
        }
        cacheTelas.put(pathFxml, cached);
        return cached;
    }

    private ScreenNode carregarTela(String pathFxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(pathFxml));
        Parent view = loader.load();
        Object controller = loader.getController();

        if (controller instanceof ConfiguracaoBancoController c) {
            c.setNavigator(this);
        }

        return new ScreenNode(view, controller);
    }

    private void invokeShow(ScreenNode node) {
        if (node.getController() instanceof ScreenLifecycle lifecycle) {
            lifecycle.onShow();
        }
    }

    private void invokeHide(ScreenNode node) {
        if (node.getController() instanceof ScreenLifecycle lifecycle) {
            lifecycle.onHide();
        }
    }

    private void destroyScreen(ScreenNode node) {
        if (node.getController() instanceof ScreenLifecycle lifecycle) {
            lifecycle.onDestroy();
        }
    }

    public void forcarLimpezaCache() {
        if (telaAtual != null) {
            invokeHide(telaAtual);
            telaAtual = null;
        }

        cacheTelas.values().forEach(this::destroyScreen);
        cacheTelas.clear();
        containerCentral.getChildren().clear();
        logger.debug("Cache de telas limpo completamente.");
    }
    
    public Object getControllerAtual() {
        return (telaAtual != null) ? telaAtual.getController() : null;
    }
}
