package br.com.yat.ecosystemcore.ui.modules.login;

import br.com.yat.ecosystemcore.application.usuario.AutenticacaoUseCase;
import br.com.yat.ecosystemcore.application.usuario.UseCaseFactory;
import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.ui.core.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;
    @FXML private ProgressIndicator progressIndicator;

    private NavigationManager navigationManager;

    private final AutenticacaoUseCase autenticacaoUseCase =
            UseCaseFactory.autenticacao();

    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
    }

    @FXML
    public void handleLogin() {

        String email = txtEmail.getText().trim();
        
        char[] senhaDisponivel = br.com.yat.ecosystemcore.util.PasswordExtractor.extrair(txtSenha);
        String senhaPura = new String(senhaDisponivel);

        if (email.isEmpty() || senhaPura.isEmpty()) {
            mostrar("Campos obrigatórios", "Preencha login e senha", Alert.AlertType.WARNING);
            java.util.Arrays.fill(senhaDisponivel, ' '); 
            return;
        }

        setLoading(true);

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                SessaoDTO sessao = autenticacaoUseCase.autenticar(email, senhaPura);
                java.util.Arrays.fill(senhaDisponivel, ' ');

                Platform.runLater(() -> {
                    setLoading(false);

                    SessionManager.iniciarSessao(
                            sessao.getUsuario(),
                            sessao.getTenant(),
                            sessao.getEmpresa()
                    );

                    mostrar("Sucesso", "Login realizado!", Alert.AlertType.INFORMATION);

                    // 🚀 TRANSIÇÃO LIMPA: Fecha a tela de login e abre o Painel Principal do Sistema
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/menu/menu-view.fxml"));
                        Parent rootMestre = loader.load();

                        // Captura o Stage atual a partir do próprio botão do formulário
                        javafx.stage.Stage stageAtual = (javafx.stage.Stage) btnEntrar.getScene().getWindow();
                        javafx.scene.Scene novaScene = new javafx.scene.Scene(rootMestre);
                        
                        stageAtual.setScene(novaScene);
                        stageAtual.setMaximized(true); // Abre em tela cheia de forma profissional
                        stageAtual.setResizable(true);
                        stageAtual.centerOnScreen();

                    } catch (java.io.IOException e) {
                        mostrar("Erro de Sistema", "Não foi possível carregar o painel principal.", Alert.AlertType.ERROR);
                        e.printStackTrace();
                    }
                });

            } catch (Exception ex) {
                java.util.Arrays.fill(senhaDisponivel, ' ');
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrar("Erro", ex.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
    }

    private void setLoading(boolean state) {
        btnEntrar.setDisable(state);
        progressIndicator.setVisible(state);
    }

    private void mostrar(String titulo, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void setNavigationManager(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }
}