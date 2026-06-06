package br.com.yat.ecosystemcore.ui.modules.usuario;

import br.com.yat.ecosystemcore.application.system.dto.MfaConfigDTO;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao; // Ajustado para usar sua classe utilitária de sessão
import br.com.yat.ecosystemcore.service.external.MfaService;
import br.com.yat.ecosystemcore.util.QrCodeGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;

public class MfaConfigController {

    @FXML private VBox paneDesativado, paneConfigurando, paneAtivado;
    @FXML private ImageView imgQrCode;
    @FXML private TextField txtSecretCopy, txtToken;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button btnIniciar, btnConfirmar;

    private final MfaService mfaService = new MfaService();
    private MfaConfigDTO mfaTemporario;

    @FXML
    public void initialize() {
        verificarEstadoAtualMfa();
    }

    /**
     * Consulta o banco de forma assíncrona para saber como renderizar a tela
     */
    private void verificarEstadoAtualMfa() {
        setLoading(true);
        Long usuarioId = Sessao.usuario().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
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
        setLoading(true);
        var usuario = Sessao.usuario();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                mfaTemporario = mfaService.iniciarConfiguracaoMfa(usuario.getId(), usuario.getEmail());
                
                // Gera os pixels da imagem em background thread
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
        String tokenStr = txtToken.getText();
        if (tokenStr == null || !tokenStr.matches("\\d{6}")) {
            exibirAlerta("Aviso", "Por favor, digite um código numérico válido com 6 dígitos.", Alert.AlertType.WARNING);
            return;
        }

        setLoading(true);
        int codigo = Integer.parseInt(tokenStr);
        Long usuarioId = Sessao.usuario().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
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
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja desativar o MFA? Sua conta ficará menos segura.", ButtonType.YES, ButtonType.NO);
        confirmacao.setHeaderText(null);
        confirmacao.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                setLoading(true);
                Long usuarioId = Sessao.usuario().getId();

                AppExecutors.getDatabaseExecutor().execute(() -> {
                    try {
                        mfaService.desativarMfa(usuarioId);
                        Platform.runLater(() -> {
                            setLoading(false);
                            exibirAlerta("Aviso", "Autenticação de dois fatores desativada.", Alert.AlertType.INFORMATION);
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