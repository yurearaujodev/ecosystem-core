package br.com.yat.ecosystemcore.ui.modules.perfil;

import br.com.yat.ecosystemcore.domain.entity.CheckablePermissaoItem;
import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.domain.entity.Permissao;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.PerfilService;
import br.com.yat.ecosystemcore.service.external.PermissaoService;
import br.com.yat.ecosystemcore.ui.core.ContextAware;
import br.com.yat.ecosystemcore.ui.core.ScreenLifecycle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.*;

public class PermissaoConsultaController implements Initializable, ScreenLifecycle, ContextAware {

    @FXML private TableView<Perfil> tblPerfis;
    @FXML private TableColumn<Perfil, String> colPerfilNome;
    @FXML private TableColumn<Perfil, String> colPerfilChave;

    @FXML private Label lblPerfilSelecionado;
    @FXML private Button btnSalvar;
    @FXML private TreeView<CheckablePermissaoItem> treePermissoes;

    private final PerfilService perfilService = new PerfilService();
    private final PermissaoService permissaoService = new PermissaoService();

    private final List<CheckBoxTreeItem<CheckablePermissaoItem>> itensFolhaTree = new ArrayList<>();
    private Perfil perfilSelecionadoAtual;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarComponentes();
        configurarListeners();
        carregarPerfis();
    }

    private void configurarComponentes() {
        colPerfilNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPerfilChave.setCellValueFactory(new PropertyValueFactory<>("chaveIdentificadora"));
        
        // Define que a árvore renderizará CheckBoxes nativos
        treePermissoes.setCellFactory(CheckBoxTreeCell.forTreeView());
    }

    private void configurarListeners() {
        // Listener de seleção da tabela de Perfis
        tblPerfis.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                focarNoPerfil(newSelection);
            } else {
                limparPainelDireito();
            }
        });
    }

    @FXML
    public void carregarPerfis() {
        String tenantId = SessionManager.getTenantAtual().getId();
        limparPainelDireito();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                var lista = perfilService.listarPerfisPorTenant(tenantId);
                Platform.runLater(() -> tblPerfis.setItems(FXCollections.observableArrayList(lista)));
            } catch (Exception e) {
                mostrarAlerta("Erro", "Não foi possível carregar os perfis: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void focarNoPerfil(Perfil perfil) {
        this.perfilSelecionadoAtual = perfil;
        lblPerfilSelecionado.setText("Perfil Ativo: " + perfil.getNome());
        btnSalvar.setDisable(false);
        treePermissoes.setDisable(false);
        
        String tenantId = SessionManager.getTenantAtual().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                // 1. Busca todas as permissões globais/tenant e as permissões que este perfil específico possui
                List<Permissao> todasPermissoes = permissaoService.listarPermissoesDisponiveis(tenantId);
                List<Long> idsPermissoesDoPerfil = permissaoService.obterIdsPermissoesDoPerfil(perfil.getId());

                Platform.runLater(() -> construirArvore(todasPermissoes, idsPermissoesDoPerfil));

            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Erro", "Erro ao recuperar permissões: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    private void construirArvore(List<Permissao> todas, List<Long> ativasDoPerfil) {
        itensFolhaTree.clear();
        
        CheckBoxTreeItem<CheckablePermissaoItem> rootNode = new CheckBoxTreeItem<>(new CheckablePermissaoItem("Módulos do Sistema"));
        rootNode.setExpanded(true);

        // Agrupa permissões pelo campo "modulo"
        Map<String, List<Permissao>> agrupadasPorModulo = new LinkedHashMap<>();
        for (Permissao perm : todas) {
            agrupadasPorModulo.computeIfAbsent(perm.getModulo(), k -> new ArrayList<>()).add(perm);
        }

        // Monta a estrutura de nós
        for (Map.Entry<String, List<Permissao>> entrada : agrupadasPorModulo.entrySet()) {
            CheckBoxTreeItem<CheckablePermissaoItem> moduloNode = new CheckBoxTreeItem<>(new CheckablePermissaoItem(entrada.getKey()));
            moduloNode.setExpanded(true); // Deixa os módulos expandidos por padrão

            for (Permissao perm : entrada.getValue()) {
                CheckablePermissaoItem itemWrapper = new CheckablePermissaoItem(perm);
                
                if (ativasDoPerfil.contains(perm.getId())) {
                    itemWrapper.setSelecionado(true);
                }

                CheckBoxTreeItem<CheckablePermissaoItem> folhaNode = new CheckBoxTreeItem<>(itemWrapper);
                
                // Vincula a propriedade de seleção bidirecional do CheckBoxTreeItem com o nosso DTO wrapper
                folhaNode.selectedProperty().bindBidirectional(itemWrapper.selecionadoProperty());
                
                moduloNode.getChildren().add(folhaNode);
                itensFolhaTree.add(folhaNode);
            }
            rootNode.getChildren().add(moduloNode);
        }

        treePermissoes.setRoot(rootNode);
        treePermissoes.setShowRoot(false); // Esconde o nó mestre "Módulos do Sistema" para visualização limpa
    }

    @FXML
    private void onSalvar() {
        if (perfilSelecionadoAtual == null) return;

        List<Long> idsParaSalvar = new ArrayList<>();
        for (CheckBoxTreeItem<CheckablePermissaoItem> itemNode : itensFolhaTree) {
            if (itemNode.isSelected() && itemNode.getValue().isFolha()) {
                idsParaSalvar.add(itemNode.getValue().getPermissao().getId());
            }
        }

        Long perfilId = perfilSelecionadoAtual.getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                permissaoService.salvarPermissoesDoPerfil(perfilId, idsParaSalvar);
                Platform.runLater(() -> mostrarAlerta("Sucesso", "Permissões do perfil atualizadas com sucesso!", Alert.AlertType.INFORMATION));
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Erro de Banco", "Falha ao gravar alterações: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    private void limparPainelDireito() {
        this.perfilSelecionadoAtual = null;
        lblPerfilSelecionado.setText("Selecione um perfil para editar...");
        btnSalvar.setDisable(true);
        treePermissoes.setRoot(null);
        treePermissoes.setDisable(true);
        itensFolhaTree.clear();
    }

    private void mostrarAlerta(String titulo, String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    // Interceptadores de ciclo de vida do NavigationManager
    @Override public void onContextChanged() { carregarPerfis(); }
    @Override public void onShow() { carregarPerfis(); }
    @Override public void onHide() {}
    @Override public void onDestroy() {}
}
