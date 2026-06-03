package br.com.yat.ecosystemcore.ui.modules.tenant;

import br.com.yat.ecosystemcore.application.tenant.OnboardingTenantUseCase;
import br.com.yat.ecosystemcore.application.tenant.OnboardingTenantUseCaseFactory;
import br.com.yat.ecosystemcore.application.tenant.dto.OnboardingTenantCommand;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnboardingTenantController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingTenantController.class);

    private final OnboardingTenantUseCase onboardingUseCase = OnboardingTenantUseCaseFactory.create();

    @FXML private TextField txtNomeConta;
    @FXML private ComboBox<String> cbPlano;
    @FXML private TextField txtRazaoSocial;
    @FXML private TextField txtNomeFantasia;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtTelefoneEmpresa;
    @FXML private TextField txtCidade;
    @FXML private TextField txtEstado;

    @FXML private TextField txtNomeAdmin;
    @FXML private TextField txtCpfAdmin;
    @FXML private TextField txtTelefoneAdmin;
    @FXML private TextField txtEmailAdmin;
    @FXML private PasswordField txtSenhaAdmin;
    @FXML private PasswordField txtConfirmarSenhaAdmin;

    @FXML private Button btnRegistrar;

    @FXML
    public void initialize() {
        cbPlano.getItems().addAll("BASIC", "PRO", "ENTERPRISE");
        cbPlano.setValue("BASIC");
    }

    @FXML
    public void handleRegistrar() {
        if (!validarCampos()) {
            return;
        }

        setLoading(true);

        OnboardingTenantCommand command = montarCommand();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                onboardingUseCase.executar(command);

                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarAlerta("Sucesso", "Ecossistema inicializado!",
                            "O Tenant e a Empresa Matriz foram gerados com sucesso.", AlertType.INFORMATION);
                    limparFormulario();
                });
            } catch (Exception e) {
                logger.error("Falha crítica ao realizar onboarding do Tenant", e);
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarAlerta("Erro no Banco", "Não foi possível salvar o registro", e.getMessage(), AlertType.ERROR);
                });
            }
        });
    }

    private OnboardingTenantCommand montarCommand() {
        return new OnboardingTenantCommand(
                txtNomeConta.getText().trim(),
                cbPlano.getValue(),
                txtRazaoSocial.getText().trim(),
                txtNomeFantasia.getText().trim(),
                txtCnpj.getText().trim(),
                txtTelefoneEmpresa.getText().trim(),
                txtCidade.getText().trim(),
                txtEstado.getText().trim(),
                txtNomeAdmin.getText().trim(),
                txtCpfAdmin.getText().trim(),
                txtTelefoneAdmin.getText().trim(),
                txtEmailAdmin.getText().trim(),
                txtSenhaAdmin.getText()
        );
    }

    private boolean validarCampos() {
        if (txtNomeConta.getText().isBlank() || txtRazaoSocial.getText().isBlank()
                || txtCnpj.getText().isBlank() || txtEmailAdmin.getText().isBlank()
                || txtSenhaAdmin.getText().isBlank()) {

            mostrarAlerta("Campos Obrigatórios", "Atenção",
                    "Preencha os campos principais do Tenant, Empresa e Administrador.", AlertType.WARNING);
            return false;
        }

        if (!txtSenhaAdmin.getText().equals(txtConfirmarSenhaAdmin.getText())) {
            mostrarAlerta("Senhas Divergentes", "Erro de digitação",
                    "A senha e a confirmação de senha não coincidem.", AlertType.WARNING);
            return false;
        }

        if (txtSenhaAdmin.getText().length() < 6) {
            mostrarAlerta("Senha Fraca", "Segurança", "A senha deve conter no mínimo 6 caracteres.", AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        btnRegistrar.setDisable(loading);
        if (btnRegistrar.getScene() != null) {
            btnRegistrar.getScene().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
        }
    }

    private void mostrarAlerta(String titulo, String cabecalho, String conteudo, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }

    private void limparFormulario() {
        txtNomeConta.clear();
        txtRazaoSocial.clear();
        txtNomeFantasia.clear();
        txtCnpj.clear();
        txtTelefoneEmpresa.clear();
        txtCidade.clear();
        txtEstado.clear();
        txtNomeAdmin.clear();
        txtCpfAdmin.clear();
        txtTelefoneAdmin.clear();
        txtEmailAdmin.clear();
        txtSenhaAdmin.clear();
        txtConfirmarSenhaAdmin.clear();
    }
}
