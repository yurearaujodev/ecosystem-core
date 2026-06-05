package br.com.yat.ecosystemcore.ui.modules.perfil;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.PerfilService;
import br.com.yat.ecosystemcore.ui.core.ContextAware;
import br.com.yat.ecosystemcore.ui.core.ScreenLifecycle;
import br.com.yat.ecosystemcore.ui.modules.perfil.PerfilCadastroController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PerfilConsultaController implements Initializable, ScreenLifecycle, ContextAware {

    @FXML private TableView<Perfil> tblPerfis;
    @FXML private TableColumn<Perfil, Long> colId;
    @FXML private TableColumn<Perfil, String> colNome;
    @FXML private TableColumn<Perfil, String> colChave;
    @FXML private TableColumn<Perfil, String> colDescricao;
    @FXML private TextField txtPesquisa;
    @FXML private Button btnAtualizar;

    private final PerfilService perfilService = new PerfilService();
    
    private final ObservableList<Perfil> masterData = FXCollections.observableArrayList();
    private FilteredList<Perfil> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabela();
        configurarListeners();
        carregarDados();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colChave.setCellValueFactory(new PropertyValueFactory<>("chaveIdentificadora"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        filteredData = new FilteredList<>(masterData, p -> true);
        tblPerfis.setItems(filteredData);
    }

    private void configurarListeners() {
        // Duplo clique para editar
        tblPerfis.setRowFactory(tv -> {
            TableRow<Perfil> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    abrirModalCadastro(row.getItem());
                }
            });
            return row;
        });

        // Filtro em tempo real digitado pelo usuário
        txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(perfil -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                if (perfil.getNome() != null && perfil.getNome().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (perfil.getChaveIdentificadora() != null && perfil.getChaveIdentificadora().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (perfil.getDescricao() != null && perfil.getDescricao().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                return false;
            });
        });
    }

    @FXML
    public void carregarDados() {
        String tenantId = SessionManager.getTenantAtual().getId();

        // Faz o fetch assíncrono usando o Pool de banco do AppExecutors
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                var lista = perfilService.listarPerfisPorTenant(tenantId);
                Platform.runLater(() -> {
                    masterData.setAll(lista); 
                    txtPesquisa.clear(); // Limpa a barra de buscas para reexibir a lista completa atualizada
                });
            } catch (Exception e) {
                mostrarAlerta("Erro", "Falha ao carregar perfis: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void onNovoPerfil() {
        abrirModalCadastro(null);
    }

    private void abrirModalCadastro(Perfil perfilAlvo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/modules/perfil-cadastro-view.fxml"));
            Parent root = loader.load();
            
            PerfilCadastroController cadastroController = loader.getController();
            
            if (perfilAlvo != null) {
                cadastroController.setPerfilParaEdicao(perfilAlvo);
            }

            Stage modalStage = new Stage();
            modalStage.setTitle(perfilAlvo == null ? "Novo Perfil" : "Editar Perfil");
            modalStage.initModality(Modality.APPLICATION_MODAL); 
            modalStage.initOwner(tblPerfis.getScene().getWindow());
            modalStage.setScene(new Scene(root));
            modalStage.setResizable(false);
            
            modalStage.showAndWait();
            
            if (cadastroController.isSalvoComSucesso()) {
                carregarDados(); 
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível abrir o formulário.", Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String msg, Alert.AlertType tipo) {
        Platform.runLater(() -> {
            Alert a = new Alert(tipo);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.show();
        });
    }

    @Override public void onContextChanged() { carregarDados(); }
    @Override public void onShow() { carregarDados(); }
    @Override public void onHide() {}
    @Override public void onDestroy() {}
}