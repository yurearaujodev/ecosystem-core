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

        // Listener que detecta cliques/seleções na tabela de usuários
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                carregarEmpresasDoUsuarioAssincrono(newSelection.getId());
            } else {
                detailData.clear(); 
            }
        });

        carregarDadosAssincrono();
    }

    private void carregarEmpresasDoUsuarioAssincrono(Long usuarioId) {
        String tenantId = SessionManager.getTenantAtual().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<EmpresaUsuarioDetalheDTO> vinculos = usuarioService.listarVinculosEmpresa(usuarioId, tenantId);
                
                Platform.runLater(() -> {
                    detailData.clear();
                    detailData.addAll(vinculos);
                });
            } catch (Exception e) {
             //   logger.error("Erro ao carregar vínculos da empresa", e);
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Erro: " + e.getMessage());
                    a.show();
                });
            }
        });
    }
    
    private void carregarDadosAssincrono() {
        String tenantId = SessionManager.getTenantAtual().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                List<Usuario> usuarios = usuarioService.listarTodos(tenantId);
                
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
        abrirDialogCadastro(null, null);
    }

    /**
     * 🔥 NOVO MÉTODO: Disparado ao clicar no botão "Alterar Operador"
     */
    @FXML
    void onAlterarUsuario() {
        Usuario usuarioSelecionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (usuarioSelecionado == null) {
            mostrarAlerta("Seleção Necessária", "Selecione um operador na tabela de cima para alterar.", Alert.AlertType.WARNING);
            return;
        }

        // Recupera o perfil vinculado a partir da tabela inferior (se houver algum vínculo ativo)
        Long perfilIdAtual = null;
        if (!tblEmpresasVinculadas.getItems().isEmpty()) {
            perfilIdAtual = tblEmpresasVinculadas.getItems().get(0).getPerfilId();
        }

        // Abre o mesmo modal, mas passando o estado do usuário para edição!
        abrirDialogCadastro(usuarioSelecionado, perfilIdAtual);
    }

    /**
     * Centralizador de abertura da janela de cadastro/edição
     */
    private void abrirDialogCadastro(Usuario usuario, Long perfilIdAtual) {
        try {
            var fxmlResource = getClass().getResource("/ui/modules/usuario-cadastro-dialog.fxml");
            
            if (fxmlResource == null) {
                mostrarAlerta("Erro de Recurso", "O arquivo FXML não foi localizado na raiz de resources.", Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlResource);
            Stage dialogStage = new Stage();
            dialogStage.setTitle(usuario == null ? "Novo Operador Credenciado" : "Alterar Dados do Operador");
            dialogStage.initModality(Modality.APPLICATION_MODAL); 
            dialogStage.setScene(new Scene(loader.load()));

            UsuarioCadastroDialogController controller = loader.getController();
            if (controller != null) {
                controller.setStage(dialogStage);
                
                // Se o usuário foi passado, o modal entra automaticamente em modo de alteração!
                if (usuario != null) {
                    controller.setUsuarioParaEdicao(usuario, perfilIdAtual);
                }
            }
            
            dialogStage.showAndWait();
            carregarDadosAssincrono(); // Recarrega a tabela principal ao fechar a janela
        } catch (IOException e) {
            e.printStackTrace();
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