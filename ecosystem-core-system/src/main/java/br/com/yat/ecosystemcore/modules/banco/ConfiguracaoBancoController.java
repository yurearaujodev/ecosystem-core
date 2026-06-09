package br.com.yat.ecosystemcore.modules.banco;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

import br.com.yat.ecosystemcore.application.DatabaseSetupFacade;
import br.com.yat.ecosystemcore.domain.dto.DatabaseForm;
import br.com.yat.ecosystemcore.domain.entity.DatabaseConfig;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.shared.util.PasswordExtractor;
import br.com.yat.ecosystemcore.core.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ConfiguracaoBancoController implements Initializable {

    private enum ConnectionState {
        NAO_TESTADO,
        SUCESSO,
        FALHA
    }

    @FXML private TextField txtNomeBanco;
    @FXML private TextField txtEnderecoIp;
    @FXML private TextField txtPorta;
    @FXML private TextField txtRoot;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatusConexao;
    @FXML private Button btnTestar;
    @FXML private Button btnSalvar;
    
    @FXML private StackPane rootContainer;

    private final DatabaseSetupFacade facade = new DatabaseSetupFacade();
    private ConnectionState estadoAtual = ConnectionState.NAO_TESTADO;
    private Navigator navigator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarFiltroPorta();
        carregarDados();
        registrarOuvintes();
    }

    private void configurarFiltroPorta() {
        UnaryOperator<TextFormatter.Change> filtro = change -> 
            change.getControlNewText().matches("\\d{0,5}") ? change : null;
        txtPorta.setTextFormatter(new TextFormatter<>(filtro));
    }

    private void registrarOuvintes() {
        txtNomeBanco.textProperty().addListener((obs, v, n) -> resetarStatusTeste());
        txtEnderecoIp.textProperty().addListener((obs, v, n) -> resetarStatusTeste());
        txtPorta.textProperty().addListener((obs, v, n) -> resetarStatusTeste());
        txtRoot.textProperty().addListener((obs, v, n) -> resetarStatusTeste());
        
        // PROBLEMA 1 FIXED: Listener específico para a senha monitorando se ficou vazia
        txtPassword.textProperty().addListener((obs, v, novaSenha) -> {
            resetarStatusTeste();
            if (novaSenha == null || novaSenha.trim().isEmpty()) {
                mudarEstadoConexao(ConnectionState.NAO_TESTADO);
            }
        });
    }

    private void resetarStatusTeste() {
        mudarEstadoConexao(ConnectionState.NAO_TESTADO);
        atualizarStatusVisual("NÃO TESTADO", "status-nao-testado");
    }

    private void mudarEstadoConexao(ConnectionState novoEstado) {
        this.estadoAtual = novoEstado;
        atualizarBotaoSalvar();
    }

    private void atualizarBotaoSalvar() {
        btnSalvar.setDisable(this.estadoAtual != ConnectionState.SUCESSO);
    }

    private DatabaseForm extrairCamposTextoBrutos() {
        return new DatabaseForm(
            txtEnderecoIp.getText(),
            txtPorta.getText(),
            txtNomeBanco.getText(),
            txtRoot.getText()
        );
    }

    private void carregarDados() {
        try {
            DatabaseConfig config = facade.carregarConfiguracaoExistente();
            if (config != null) {
                txtEnderecoIp.setText(config.ip()); 
                txtPorta.setText(config.porta());      
                txtNomeBanco.setText(config.banco());  
                txtRoot.setText(config.usuario());
                atualizarStatusVisual("NÃO TESTADO", "status-nao-testado");
            }
        } catch (Exception e) {
            atualizarStatusVisual("ERRO AO CARREGAR CONFIGURAÇÃO", "status-erro");
        }
    }

    @FXML
    public void testar() {
        try {
            char[] senha = PasswordExtractor.extrair(txtPassword);
            
            setUiBloqueada(true);
            // UX FIXED: Alterado para status-aguardando
            atualizarStatusVisual("CONECTANDO AO BANCO...", "status-aguardando");

            facade.testarConexao(extrairCamposTextoBrutos(), senha,
                status -> { 
                    setUiBloqueada(false);
                    if (status.available()) {
                        atualizarStatusVisual("SUCESSO: " + status.message().toUpperCase(), "status-sucesso");
                        mudarEstadoConexao(ConnectionState.SUCESSO);
                    } else {
                        atualizarStatusVisual(status.message().toUpperCase(), "status-erro");
                        mudarEstadoConexao(ConnectionState.FALHA);
                    }
                },
                erro -> { 
                    setUiBloqueada(false);
                    Throwable t = erro.getCause() != null ? erro.getCause() : erro;
                    String msg = t.getMessage() != null ? t.getMessage() : "Erro desconhecido";
                    
                    atualizarStatusVisual("ERRO: " + msg.toUpperCase(), "status-erro");
                    mudarEstadoConexao(ConnectionState.FALHA);
                }
            );
        } catch (IllegalArgumentException e) {
            atualizarStatusVisual(e.getMessage(), "status-erro");
        }
    }
    
    @FXML
    public void salvar() {
        try {
            char[] senha = PasswordExtractor.extrair(txtPassword);
            
            setUiBloqueada(true);
            atualizarStatusVisual("SALVANDO CONFIGURAÇÃO...", "status-aguardando");

            facade.salvarConfiguracao(extrairCamposTextoBrutos(), senha,
                () -> { 
                    // Garante que a alteração visual rode na thread correta do JavaFX
                    javafx.application.Platform.runLater(() -> {
                        setUiBloqueada(false);
                        
                        // ARQUITETURA PROFISSIONAL: Delega a navegação ao Navigator injetado
                        if (this.navigator != null) {
                            // Altere para a chave que você desejar redirecionar (ex: CADASTROS_EMPRESA ou HOME)
                            this.navigator.navigatePara(MenuChave.DASHBOARD);
                        }
                    });
                },
                erro -> { 
                    setUiBloqueada(false);
                    Throwable t = erro.getCause() != null ? erro.getCause() : erro;
                    String msg = t.getMessage() != null ? t.getMessage() : "Erro desconhecido";
                    
                    atualizarStatusVisual("ERRO AO SALVAR: " + msg.toUpperCase(), "status-erro");
                }
            );
        } catch (IllegalArgumentException e) {
            atualizarStatusVisual(e.getMessage(), "status-erro");
        }
    }

//    @FXML
//    public void salvar() {
//        try {
//            char[] senha = PasswordExtractor.extrair(txtPassword);
//            
//            setUiBloqueada(true);
//            // UX FIXED: Alterado para status-aguardando
//            atualizarStatusVisual("SALVANDO CONFIGURAÇÃO...", "status-aguardando");
//
//            facade.salvarConfiguracao(extrairCamposTextoBrutos(), senha,
//                () -> { 
//                    setUiBloqueada(false);
//                    Stage stage = (Stage) btnSalvar.getScene().getWindow();
//                    stage.close();
//                },
//                erro -> { 
//                    setUiBloqueada(false);
//                    Throwable t = erro.getCause() != null ? erro.getCause() : erro;
//                    String msg = t.getMessage() != null ? t.getMessage() : "Erro desconhecido";
//                    
//                    atualizarStatusVisual("ERRO AO SALVAR: " + msg.toUpperCase(), "status-erro");
//                }
//            );
//        } catch (IllegalArgumentException e) {
//            atualizarStatusVisual(e.getMessage(), "status-erro");
//        }
//    }

    private void setUiBloqueada(boolean bloqueada) {
        btnTestar.setDisable(bloqueada);
        txtNomeBanco.setDisable(bloqueada);
        txtEnderecoIp.setDisable(bloqueada);
        txtPorta.setDisable(bloqueada);
        txtRoot.setDisable(bloqueada);
        txtPassword.setDisable(bloqueada);
        
        if (bloqueada) {
            btnSalvar.setDisable(true);
        } else {
            atualizarBotaoSalvar();
        }
    }

    private void atualizarStatusVisual(String texto, String classeCss) {
        lblStatusConexao.setText(texto.toUpperCase());
        lblStatusConexao.getStyleClass().clear();
        lblStatusConexao.getStyleClass().addAll("label", classeCss);
    }
    
	public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }
}
