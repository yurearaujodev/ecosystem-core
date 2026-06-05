package br.com.yat.ecosystemcore.ui.modules.perfil;

import br.com.yat.ecosystemcore.domain.entity.Perfil;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.PerfilService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PerfilCadastroController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtNome;
    @FXML private TextField txtChave;
    @FXML private TextArea txtDescricao;
    @FXML private Button btnExcluir;

    private final PerfilService perfilService = new PerfilService();
    private Perfil perfilEdicao;
    private boolean salvoComSucesso = false;

    public void setPerfilParaEdicao(Perfil perfil) {
        this.perfilEdicao = perfil;
        lblTitulo.setText("Editar Perfil #" + perfil.getId());
        txtNome.setText(perfil.getNome());
        txtChave.setText(perfil.getChaveIdentificadora());
        txtChave.setDisable(true); 
        txtDescricao.setText(perfil.getDescricao());
        btnExcluir.setVisible(true); // Ativa o botão de exclusão apenas se for edição
    }

    public boolean isSalvoComSucesso() {
        return salvoComSucesso;
    }

    @FXML
    private void onSalvar() {
        String nome = txtNome.getText();
        String chave = txtChave.getText();
        String descricao = txtDescricao.getText();

        if (nome == null || nome.isBlank() || chave == null || chave.isBlank()) {
            mostrarAlerta("Validação", "Os campos Nome e Chave são obrigatórios.", Alert.AlertType.WARNING);
            return;
        }

        Long usuarioLogadoId = SessionManager.getUsuarioLogado().getId();
        String tenantId = SessionManager.getTenantAtual().getId();

        boolean ehEdicao = (perfilEdicao != null);
        Perfil perfil = ehEdicao ? perfilEdicao : new Perfil();
        perfil.setNome(nome.trim());
        perfil.setChaveIdentificadora(chave.trim().toUpperCase());
        perfil.setDescricao(descricao != null ? descricao.trim() : "");
        perfil.setTenantId(tenantId);

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                if (ehEdicao) {
                    perfilService.atualizarPerfil(perfil, usuarioLogadoId);
                } else {
                    perfilService.cadastrarPerfil(perfil, usuarioLogadoId);
                }

                Platform.runLater(() -> {
                    salvoComSucesso = true;
                    fecharJanela();
                });
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Erro", "Erro ao salvar perfil: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    @FXML
    private void onExcluir() {
        if (perfilEdicao == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Deseja realmente remover este perfil permanentemente?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.YES) {
                Long id = perfilEdicao.getId();
                String tenantId = SessionManager.getTenantAtual().getId();
                Long usuarioLogadoId = SessionManager.getUsuarioLogado().getId();

                AppExecutors.getDatabaseExecutor().execute(() -> {
                    try {
                        perfilService.excluirPerfil(id, tenantId, usuarioLogadoId);
                        Platform.runLater(() -> {
                            salvoComSucesso = true; // Força refresh da tabela de trás
                            fecharJanela();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> mostrarAlerta("Erro", "Falha ao deletar: " + e.getMessage(), Alert.AlertType.ERROR));
                    }
                });
            }
        });
    }

    @FXML
    private void onCancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) txtNome.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}