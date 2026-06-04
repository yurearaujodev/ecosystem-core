package br.com.yat.ecosystemcore.ui.modules.usuario;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.entity.Usuario;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.UsuarioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class UsuarioController {

    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, Long> colId;
    @FXML private TableColumn<Usuario, String> colUuid;
    @FXML private TableColumn<Usuario, String> colEmail;
    @FXML private TableColumn<Usuario, Long> colPessoaId;
    @FXML private TableColumn<Usuario, String> colStatus;
    @FXML private TableColumn<Usuario, Integer> colVersao;
    @FXML private TextField txtFiltroEmail;
    
    @FXML private TableView<EmpresaUsuarioDetalheDTO> tblEmpresasVinculadas;
    @FXML private TableColumn<EmpresaUsuarioDetalheDTO, Long> colEmpresaId;
    @FXML private TableColumn<EmpresaUsuarioDetalheDTO, String> colEmpresaNome;
    @FXML private TableColumn<EmpresaUsuarioDetalheDTO, Long> colPerfilId;
    @FXML private TableColumn<EmpresaUsuarioDetalheDTO, String> colPerfilNome;

    private final ObservableList<EmpresaUsuarioDetalheDTO> detailData = FXCollections.observableArrayList();

    private final UsuarioService usuarioService = new UsuarioService();
    private final ObservableList<Usuario> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configuração da Tabela Master (Usuários)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUuid.setCellValueFactory(new PropertyValueFactory<>("uuidPublico"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPessoaId.setCellValueFactory(new PropertyValueFactory<>("pessoaId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tblUsuarios.setItems(masterData);

        // Configuração da Tabela Detail (Empresas Vinculadas)
        colEmpresaId.setCellValueFactory(new PropertyValueFactory<>("empresaId"));
        colEmpresaNome.setCellValueFactory(new PropertyValueFactory<>("empresaNome"));
        colPerfilId.setCellValueFactory(new PropertyValueFactory<>("perfilId"));
        colPerfilNome.setCellValueFactory(new PropertyValueFactory<>("perfilNome"));
        tblEmpresasVinculadas.setItems(detailData);

        // 🔥 O SEGREDO: Listener que detecta cliques/seleções na tabela de usuários
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
        	System.out.println("Clique detectado! Usuário ID: " + (newSelection != null ? newSelection.getId() : "null"));
        	if (newSelection != null) {
                carregarEmpresasDoUsuarioAssincrono(newSelection.getId());
            } else {
                detailData.clear(); // Limpa se nenhum usuário estiver selecionado
            }
        });

        carregarDadosAssincrono();
    }
    private void carregarEmpresasDoUsuarioAssincrono(Long usuarioId) {
        String tenantId = SessionManager.getTenantAtual().getId();

        // Desvia a busca do JOIN para a Thread Pool de banco de dados
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<EmpresaUsuarioDetalheDTO> vinculos = usuarioService.listarVinculosEmpresa(usuarioId, tenantId);
                
                System.out.println("Query retornou " + (vinculos != null ? vinculos.size() : "null") + " registros.");
                // Devolve a lista preenchida de forma segura para a thread JavaFX
                Platform.runLater(() -> {
                    detailData.clear();
                    detailData.addAll(vinculos);
                });
            } catch (Exception e) {
            	e.printStackTrace(); // Isso vai imprimir o erro real no console da sua IDE
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Erro: " + e.getMessage());
                    a.show();
                });
            }
        });
    }
    
//    @FXML
//    public void initialize() {
//        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
//        colUuid.setCellValueFactory(new PropertyValueFactory<>("uuidPublico"));
//        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
//        colPessoaId.setCellValueFactory(new PropertyValueFactory<>("pessoaId"));
//        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
//        colVersao.setCellValueFactory(new PropertyValueFactory<>("version"));
//
//        tblUsuarios.setItems(masterData);
//        carregarDadosAssincrono();
//    }
//
    private void carregarDadosAssincrono() {
        String tenantId = SessionManager.getTenantAtual().getId();

        // Executa a query em background para não congelar o JavaFX
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<Usuario> usuarios = usuarioService.listarTodos(tenantId);
                
                // Atualiza os componentes visuais de volta na UI Thread
                Platform.runLater(() -> {
                    masterData.clear();
                    masterData.addAll(usuarios);
                });
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Erro", "Erro ao carregar dados: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    @FXML
    void onAtualizarTabela() {
        carregarDadosAssincrono();
    }

    @FXML
    void onFiltrar() {
        String termo = txtFiltroEmail.getText();
        if (termo == null || termo.isBlank()) {
            carregarDadosAssincrono();
            return;
        }
        var filtrados = masterData.stream()
                .filter(u -> u.getEmail().toLowerCase().contains(termo.toLowerCase()))
                .toList();
        tblUsuarios.setItems(FXCollections.observableArrayList(filtrados));
    }

    @FXML
    void onDeletarUsuario() {
        Usuario selecionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta("Aviso", "Selecione um usuário para remover o acesso.", Alert.AlertType.WARNING);
            return;
        }

        String tenantId = SessionManager.getTenantAtual().getId();
        Long usuarioLogadoId = SessionManager.getUsuarioLogado().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                usuarioService.deletarUsuario(selecionado.getId(), tenantId, usuarioLogadoId);
                Platform.runLater(() -> {
                    mostrarAlerta("Sucesso", "Acesso revogado com sucesso.", Alert.AlertType.INFORMATION);
                    carregarDadosAssincrono();
                });
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Erro", "Falha na exclusão: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    @FXML
    void onNovoUsuario() {
        try {
            // 🔥 CORREÇÃO: Adicionada a barra '/' no início para torná-lo um caminho absoluto a partir de resources
            var fxmlResource = getClass().getResource("/ui/modules/usuario-cadastro-dialog.fxml");
            
            if (fxmlResource == null) {
                mostrarAlerta("Erro de Recurso", "O arquivo FXML não foi localizado na raiz de resources.", Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlResource);
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Novo Operador Credenciado");
            dialogStage.initModality(Modality.APPLICATION_MODAL); // Impede clicar na tela de trás enquanto esta estiver aberta
            dialogStage.setScene(new Scene(loader.load()));

            UsuarioCadastroDialogController controller = loader.getController();
            if (controller != null) {
                controller.setStage(dialogStage);
            }
            
            dialogStage.showAndWait();
            carregarDadosAssincrono(); // Recarrega de forma assíncrona ao fechar a janela
        } catch (IOException e) {
            e.printStackTrace(); // Ajuda a ver no console se houver outro erro dentro do FXML de cadastro
            mostrarAlerta("Erro de UI", "Não foi possível carregar a janela de diálogo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}