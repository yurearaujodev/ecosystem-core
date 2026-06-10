package br.com.yat.ecosystemcore.modules.seguranca.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.modules.seguranca.dto.MfaConfigDTO;
import br.com.yat.ecosystemcore.modules.seguranca.service.MfaService;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.util.QrCodeGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;

public class MfaConfigController {

	@FXML
	private VBox paneDesativado, paneConfigurando, paneAtivado;
	@FXML
	private ImageView imgQrCode;
	@FXML
	private TextField txtSecretCopy, txtToken;
	@FXML
	private ProgressIndicator loadingIndicator;
	@FXML
	private Button btnIniciar, btnConfirmar;

	private final MfaService mfaService = ApplicationContext.getMfaService();
	private MfaConfigDTO mfaTemporario;

	@FXML
	public void initialize() {
		verificarEstadoAtualMfa();
	}

	private void verificarEstadoAtualMfa() {
		setLoading(true);
		Long usuarioId = Sessao.usuarioId();

		AppExecutors.execute(() -> {
			try {
				boolean ativo = mfaService.isMfaAtivo(usuarioId);
				Platform.runLater(() -> {
					setLoading(false);
					ajustarVisibilidadePaineis(ativo ? EstadoTela.ATIVADO : EstadoTela.DESATIVADO);
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					setLoading(false);
					exibirAlerta("Erro", "Falha ao verificar estado do MFA: " + e.getMessage(), Alert.AlertType.ERROR);
				});
			}
		});
	}

	/**
	 * Dispara o passo 1: Gera as chaves e o QR Code gráfico
	 */
	@FXML
	private void handleIniciarMfa() {
//		if (!Sessao.hasPermission("SEGURANCA.MFA.CONFIGURAR")) {
//		exibirAlerta("Acesso Negado", "Você não possui permissão para configurar MFA.", Alert.AlertType.ERROR);
//		return;
//	}
		setLoading(true);
		Long usuarioId = Sessao.usuarioId();
		String email = Sessao.user().getEmail();

		AppExecutors.execute(() -> {
			try {
				mfaTemporario = mfaService.iniciarConfiguracaoMfa(usuarioId, email);

				WritableImage qrImage = QrCodeGenerator.gerarImagemQrCode(mfaTemporario.qrCodeUrl(), 200);

				Platform.runLater(() -> {
					setLoading(false);
					imgQrCode.setImage(qrImage);
					txtSecretCopy.setText(mfaTemporario.secretBase32());
					ajustarVisibilidadePaineis(EstadoTela.CONFIGURANDO);
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					setLoading(false);
					exibirAlerta("Erro", "Falha ao iniciar configuração: " + e.getMessage(), Alert.AlertType.ERROR);
				});
			}
		});
	}

	/**
	 * Dispara o passo 2: Envia o token digitado para confirmação no banco
	 */
	@FXML
	private void handleConfirmarMfa() {
//		if (!Sessao.hasPermission("SEGURANCA.MFA.CONFIGURAR")) {
//		exibirAlerta("Acesso Negado", "Você não possui permissão para configurar MFA.", Alert.AlertType.ERROR);
//		return;
//	}
		String tokenStr = txtToken.getText();

		if (tokenStr == null || !tokenStr.matches("\\d{6}")) {
			exibirAlerta("Aviso", "Por favor, digite um código numérico válido com 6 dígitos.",
					Alert.AlertType.WARNING);
			return;
		}

		setLoading(true);
		int codigo = Integer.parseInt(tokenStr);
		Long usuarioId = Sessao.usuarioId();

		AppExecutors.execute(() -> {
			try {
				boolean sucesso = mfaService.verificarEAtivarMfa(usuarioId, codigo);
				Platform.runLater(() -> {
					setLoading(false);
					if (sucesso) {
						exibirAlerta("Sucesso", "MFA ativado com sucesso em sua conta!", Alert.AlertType.INFORMATION);
						txtToken.clear();
						ajustarVisibilidadePaineis(EstadoTela.ATIVADO);
					} else {
						exibirAlerta("Erro", "Código inválido ou expirado. Tente novamente.", Alert.AlertType.ERROR);
					}
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					setLoading(false);
					exibirAlerta("Erro", "Erro ao validar o código: " + e.getMessage(), Alert.AlertType.ERROR);
				});
			}
		});
	}

	/**
	 * Permite ao usuário remover a proteção da conta
	 */
	@FXML
	private void handleDesativarMfa() {
//		if (!Sessao.hasPermission("SEGURANCA.MFA.CONFIGURAR")) {
//			exibirAlerta("Acesso Negado", "Você não possui permissão para configurar MFA.", Alert.AlertType.ERROR);
//			return;
//		}
		Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
				"Tem certeza que deseja desativar o MFA? Sua conta ficará menos segura.", ButtonType.YES,
				ButtonType.NO);

		confirmacao.setHeaderText(null);
		confirmacao.showAndWait().ifPresent(response -> {

			if (response == ButtonType.YES) {
				setLoading(true);
				Long usuarioId = Sessao.usuarioId();

				AppExecutors.execute(() -> {
					try {
						mfaService.desativarMfa(usuarioId);
						Platform.runLater(() -> {
							setLoading(false);
							exibirAlerta("Aviso", "Autenticação de dois fatores desativada.",
									Alert.AlertType.INFORMATION);
							ajustarVisibilidadePaineis(EstadoTela.DESATIVADO);
						});
					} catch (Exception e) {
						Platform.runLater(() -> {
							setLoading(false);
							exibirAlerta("Erro", "Falha ao desativar: " + e.getMessage(), Alert.AlertType.ERROR);
						});
					}
				});
			}
		});
	}

	@FXML
	private void handleCancelar() {
		txtToken.clear();
		ajustarVisibilidadePaineis(EstadoTela.DESATIVADO);
	}

	private void ajustarVisibilidadePaineis(EstadoTela estado) {
		paneDesativado.setVisible(estado == EstadoTela.DESATIVADO);
		paneDesativado.setManaged(estado == EstadoTela.DESATIVADO);

		paneConfigurando.setVisible(estado == EstadoTela.CONFIGURANDO);
		paneConfigurando.setManaged(estado == EstadoTela.CONFIGURANDO);

		paneAtivado.setVisible(estado == EstadoTela.ATIVADO);
		paneAtivado.setManaged(estado == EstadoTela.ATIVADO);
	}

	private void setLoading(boolean loading) {
		loadingIndicator.setVisible(loading);
		loadingIndicator.setManaged(loading);
		btnIniciar.setDisable(loading);
		btnConfirmar.setDisable(loading);
	}

	private void exibirAlerta(String titulo, String msg, Alert.AlertType tipo) {
		Alert alert = new Alert(tipo);
		alert.setTitle(titulo);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

	private enum EstadoTela {
		DESATIVADO, CONFIGURANDO, ATIVADO
	}
}