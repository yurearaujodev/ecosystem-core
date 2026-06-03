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
        String senha = txtSenha.getText();

        if (email.isEmpty() || senha.isEmpty()) {
            mostrar("Campos obrigatórios", "Preencha login e senha", Alert.AlertType.WARNING);
            return;
        }

        setLoading(true);

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {

                SessaoDTO sessao = autenticacaoUseCase.autenticar(email, senha);

                Platform.runLater(() -> {

                    setLoading(false);

                    SessionManager.iniciarSessao(
                            sessao.getUsuario(),
                            sessao.getTenant(),
                            sessao.getEmpresa()
                    );

                    mostrar("Sucesso", "Login realizado!", Alert.AlertType.INFORMATION);

                    navigationManager.navigatePara(MenuChave.HOME);
                });

            } catch (Exception ex) {

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