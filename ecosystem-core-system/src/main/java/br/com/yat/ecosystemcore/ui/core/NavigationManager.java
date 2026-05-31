package br.com.yat.ecosystemcore.ui.core;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.util.*;

public class NavigationManager {

    private final StackPane containerCentral;
    private final Map<MenuChave, String> rotas = new EnumMap<>(MenuChave.class);
    private final Map<String, ScreenNode> cacheTelas = new HashMap<>();
    
    private ScreenNode telaAtual;
    private static final int LIMITE_MAX_CACHE = 15;

    public NavigationManager(StackPane containerCentral) {
        this.containerCentral = containerCentral;
        configurarRotas();
    }
    
    private void configurarRotas() {
        // =========================================================================
        // MOCK DE ROTAS: Apontando temporariamente para a única view que existe
        // para validar se os cliques do menu gerado por banco estão disparando a navegação!
        // =========================================================================
        String caminhoTemporario = "/ui/modules/empresa/EmpresaView.fxml";

        rotas.put(MenuChave.CADASTROS_EMPRESA, caminhoTemporario);
        rotas.put(MenuChave.CADASTROS_PESSOA, caminhoTemporario);
        rotas.put(MenuChave.CADASTROS_USUARIO, caminhoTemporario);
        
        rotas.put(MenuChave.SEGURANÇA_PERFIL, caminhoTemporario);
        rotas.put(MenuChave.SEGURANÇA_PERMISSAO, caminhoTemporario);
        
        rotas.put(MenuChave.ADMIN_TENANT_CONFIG, caminhoTemporario);
        rotas.put(MenuChave.ADMIN_PARAMETROS, caminhoTemporario);
        
        rotas.put(MenuChave.AUDITORIA_LOGS, caminhoTemporario);
        rotas.put(MenuChave.AUDITORIA_JOBS, caminhoTemporario);
    }

    /* DICA PROFISSIONAL: Quando você criar as telas reais lá na frente, 
       basta descomentar este bloco e colocar os caminhos corretos de cada FXML!
       
    private void configurarRotasReais() {
        rotas.put(MenuChave.CADASTROS_EMPRESA, "/ui/modules/empresa/EmpresaView.fxml");
        rotas.put(MenuChave.CADASTROS_PESSOA, "/ui/modules/pessoa/PessoaView.fxml");
        rotas.put(MenuChave.CADASTROS_USUARIO, "/ui/modules/usuario/UsuarioView.fxml");
        rotas.put(MenuChave.SEGURANÇA_PERFIL, "/ui/modules/seguranca/PerfilView.fxml");
        rotas.put(MenuChave.SEGURANÇA_PERMISSAO, "/ui/modules/seguranca/PermissaoView.fxml");
        rotas.put(MenuChave.ADMIN_TENANT_CONFIG, "/ui/modules/admin/TenantConfigView.fxml");
        rotas.put(MenuChave.ADMIN_PARAMETROS, "/ui/modules/admin/ParametrosView.fxml");
        rotas.put(MenuChave.AUDITORIA_LOGS, "/ui/modules/auditoria/LogsView.fxml");
        rotas.put(MenuChave.AUDITORIA_JOBS, "/ui/modules/auditoria/JobsView.fxml");
    }
    */

    public void navigatePara(MenuChave chave) {
        String pathFxml = rotas.get(chave);
        if (pathFxml == null) {
            System.out.println("⚠️ Rota não encontrada para a chave: " + chave);
            return;
        }

        if (telaAtual != null && telaAtual.getController() instanceof ScreenLifecycle) {
            ((ScreenLifecycle) telaAtual.getController()).onHide();
        }

        if (cacheTelas.size() >= LIMITE_MAX_CACHE && !cacheTelas.containsKey(pathFxml)) {
            evictCache();
        }

        ScreenNode proximaTela = cacheTelas.computeIfAbsent(pathFxml, path -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent view = loader.load();
                Object controller = loader.getController();
                return new ScreenNode(view, controller);
            } catch (IOException e) {
                throw new RuntimeException("Falha ao instanciar o FXML: " + path, e);
            }
        });

        containerCentral.getChildren().setAll(proximaTela.getView());
        this.telaAtual = proximaTela;

        if (proximaTela.getController() instanceof ScreenLifecycle) {
            ((ScreenLifecycle) proximaTela.getController()).onShow();
        }
    }

    private void evictCache() {
        String primeiraChave = cacheTelas.keySet().iterator().next();
        ScreenNode removida = cacheTelas.remove(primeiraChave);
        if (removida != null && removida.getController() instanceof ScreenLifecycle) {
            ((ScreenLifecycle) removida.getController()).onDestroy();
        }
        System.gc();
    }

    public void forcarLimpezaCache() {
        cacheTelas.values().forEach(node -> {
            if (node.getController() instanceof ScreenLifecycle) {
                ((ScreenLifecycle) node.getController()).onDestroy();
            }
        });
        cacheTelas.clear();
    }
}