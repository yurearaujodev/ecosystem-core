package br.com.yat.ecosystemcore.ui.modules.perfil;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao; // 🔒 Importação corrigida para a infraestrutura ativa
import br.com.yat.ecosystemcore.service.external.PerfilService;
import br.com.yat.ecosystemcore.ui.core.ContextAware;
import br.com.yat.ecosystemcore.ui.core.ScreenLifecycle;
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

        // 🔍 Filtro em tempo real refinado, limpo e sem a "escada" de if/else
        txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(perfil -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String query = newValue.toLowerCase().trim();

                // Transforma as propriedades textuais em um fluxo de dados e verifica se alguma bate com a busca
                return java.util.stream.Stream.of(
                        perfil.getNome(), 
                        perfil.getChaveIdentificadora(), 
                        perfil.getDescricao()
                )
                .filter(java.util.Objects::nonNull) // Ignora campos nulos no banco com segurança
                .map(String::toLowerCase)
                .anyMatch(campo -> campo.contains(query)); // Retorna true se houver match em qualquer campo
            });
        });
    }

    @FXML
    public void carregarDados() {
        // 🔒 VALIDAÇÃO DE SEGURANÇA: Bloqueia a execução assíncrona se não houver sessão ativa
        if (!Sessao.isActive() || Sessao.tenant() == null) {
            System.err.println("⚠️ TENTATIVA DE CONSULTA NEGADA: Nenhuma sessão ativa para o Tenant.");
            masterData.clear();
            return;
        }

        String tenantId = Sessao.tenant().getId();

        // Faz o fetch assíncrono usando o Pool de banco do AppExecutors (Sem travar o JavaFX)
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                var lista = perfilService.listarPerfisPorTenant(tenantId);
                
                Platform.runLater(() -> {
                    masterData.setAll(lista); 
                    txtPesquisa.clear(); // Limpa a barra de buscas para reexibir a lista completa atualizada
                });
            } catch (Exception e) {
                Platform.runLater(() -> 
                    mostrarAlerta("Erro", "Falha ao carregar perfis: " + e.getMessage(), Alert.AlertType.ERROR)
                );
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