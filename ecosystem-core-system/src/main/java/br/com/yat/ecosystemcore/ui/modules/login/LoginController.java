package br.com.yat.ecosystemcore.ui.modules.login;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.application.system.dto.TenantComboDTO;
import br.com.yat.ecosystemcore.application.usuario.AutenticacaoUseCase;
import br.com.yat.ecosystemcore.application.usuario.dto.SessaoDTO;
import br.com.yat.ecosystemcore.shared.context.SessionContext;
import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.shared.context.UserContext;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;
import br.com.yat.ecosystemcore.shared.security.UserContextProvider;
import br.com.yat.ecosystemcore.ui.core.NavigationManager;
import br.com.yat.ecosystemcore.util.PasswordExtractor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginController {

	@FXML
	private ComboBox<TenantComboDTO> cmbTenant;
	@FXML
	private TextField txtEmail;
	@FXML
	private PasswordField txtSenha;
	@FXML
	private Button btnEntrar;
	@FXML
	private ProgressIndicator progressIndicator;

	private NavigationManager navigationManager;
	private final AutenticacaoUseCase autenticacaoUseCase = ApplicationContext.getAutenticacaoUseCase();

	@FXML
	public void initialize() {
		progressIndicator.setVisible(false);
		carregarTenantsDisponiveis();
	}

	private void carregarTenantsDisponiveis() {
		AppExecutors.getDatabaseExecutor().execute(() -> {
			String sql = "SELECT DISTINCT tenant_id, nome_fantasia FROM empresa WHERE ativo = 1 ORDER BY nome_fantasia ASC";
			List<TenantComboDTO> tenants = new ArrayList<>();

			try (Connection conn = ConnectionFactory.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql);
					ResultSet rs = stmt.executeQuery()) {

				while (rs.next()) {
					String tId = rs.getString("tenant_id");
					String nome = rs.getString("nome_fantasia");
					tenants.add(new TenantComboDTO(tId, nome));
				}

				Platform.runLater(() -> {
					cmbTenant.getItems().addAll(tenants);
					if (!cmbTenant.getItems().isEmpty()) {
						cmbTenant.getSelectionModel().selectFirst();
					}
				});

			} catch (Exception e) {
				Platform.runLater(() -> mostrar("Erro de Infraestrutura",
						"Não foi possível carregar o catálogo corporativo de empresas.", Alert.AlertType.ERROR));
			}
		});
	}

	@FXML
	public void handleLogin() {
		String email = txtEmail.getText().trim();
		TenantComboDTO tenantSelecionado = cmbTenant.getSelectionModel().getSelectedItem();
		char[] senhaDisponivel = PasswordExtractor.extrair(txtSenha);
		if (email.isEmpty() || tenantSelecionado == null || senhaDisponivel == null || senhaDisponivel.length == 0) {
			mostrar("Campos obrigatórios", "Preencha tudo", Alert.AlertType.WARNING);
			return;
		}
		setLoading(true);
		String tenantId = tenantSelecionado.tenantId();
		AppExecutors.execute(() -> {
			try {
				SessionContext contexto = autenticacaoUseCase.autenticar(email, senhaDisponivel, tenantId);

				Platform.runLater(() -> {
					SessionScope.open(contexto);
					abrirMenuPrincipal();
					setLoading(false);

				});

			} catch (Exception ex) {
				Platform.runLater(() -> {
					setLoading(false);
					mostrar("Erro", ex.getMessage(), Alert.AlertType.ERROR);
				});
			} finally {
				Arrays.fill(senhaDisponivel, '\0');
			}
		});
	}

	private void abrirMenuPrincipal() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/menu/menu-view.fxml"));

			Parent root = loader.load();

			Stage stage = (Stage) btnEntrar.getScene().getWindow();
			stage.setScene(new Scene(root));

			stage.setResizable(true);
			stage.setMaximized(true);
			stage.centerOnScreen();
		} catch (Exception e) {
			mostrar("Erro", "Falha ao abrir menu", Alert.AlertType.ERROR);
		}
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