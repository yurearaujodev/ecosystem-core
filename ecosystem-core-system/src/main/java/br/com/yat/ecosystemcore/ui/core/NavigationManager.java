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

        rotas.put(MenuChave.HOME, "/ui/modules/home.fxml");

        rotas.put(MenuChave.CADASTROS_EMPRESA, "/ui/modules/empresa/EmpresaView.fxml");
        rotas.put(MenuChave.CADASTROS_PESSOA, "/ui/modules/pessoa/PessoaView.fxml");
        rotas.put(MenuChave.CADASTROS_USUARIO, "/ui/modules/usuario/UsuarioView.fxml");

        rotas.put(MenuChave.SEGURANCA_PERFIL, "/ui/modules/seguranca/PerfilView.fxml");
        rotas.put(MenuChave.SEGURANCA_PERMISSAO, "/ui/modules/seguranca/PermissaoView.fxml");

        rotas.put(MenuChave.ADMIN_TENANT_CONFIG, "/ui/modules/onboarding-tenant-view.fxml");
        rotas.put(MenuChave.ADMIN_PARAMETROS, "/ui/modules/admin/ParametrosView.fxml");

        rotas.put(MenuChave.CONFIGURACAO_BANCO, "/ui/modules/configuracao-banco.fxml");

        rotas.put(MenuChave.AUDITORIA_LOGS, "/ui/modules/auditoria/LogsView.fxml");
        rotas.put(MenuChave.AUDITORIA_JOBS, "/ui/modules/auditoria/JobsView.fxml");

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
}
