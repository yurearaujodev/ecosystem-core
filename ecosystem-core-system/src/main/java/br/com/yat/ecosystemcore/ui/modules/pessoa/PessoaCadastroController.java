package br.com.yat.ecosystemcore.ui.modules.pessoa;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.service.external.PessoaService;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class PessoaCadastroController {

    private static final Logger logger = LoggerFactory.getLogger(PessoaCadastroController.class);

    @FXML private Label lblTitulo;
    @FXML private Label lblNomeRazao;
    @FXML private Label lblApelidoFantasia;
    @FXML private Label lblCpfCnpj;
    
    @FXML private ComboBox<String> cbTipo;
    @FXML private TextField txtNomeRazao;
    @FXML private TextField txtApelidoFantasia;
    @FXML private TextField txtCpfCnpj;
    @FXML private TextField txtTelefone;

    private final PessoaService pessoaService = new PessoaService();
    private Pessoa pessoaFoco;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList("FISICA", "JURIDICA"));
        cbTipo.setValue("FISICA");
        cbTipo.valueProperty().addListener((obs, antigo, novo) -> alterarLabelsFormulario(novo));
        alterarLabelsFormulario("FISICA");
    }

    private void alterarLabelsFormulario(String tipo) {
        if ("JURIDICA".equals(tipo)) {
            lblNomeRazao.setText("Razão Social *:");
            lblApelidoFantasia.setText("Nome Fantasia:");
            lblCpfCnpj.setText("CNPJ *:");
            txtCpfCnpj.setPromptText("00.000.000/0000-00");
        } else {
            lblNomeRazao.setText("Nome Completo *:");
            lblApelidoFantasia.setText("Apelido / Vulgo:");
            lblCpfCnpj.setText("CPF *:");
            txtCpfCnpj.setPromptText("000.000.000-00");
        }
    }

    public void iniciarEdicao(Pessoa pessoa, Runnable onSaveCallback) {
        this.pessoaFoco = pessoa;
        this.onSaveCallback = onSaveCallback;
        
        lblTitulo.setText("Alterar Cadastro de Pessoa");
        cbTipo.setValue(pessoa.getTipo());
        txtNomeRazao.setText(pessoa.getNomeRazao());
        txtApelidoFantasia.setText(pessoa.getApelidoFantasia());
        txtCpfCnpj.setText(pessoa.getCpfCnpj());
        txtTelefone.setText(pessoa.getTelefone());
    }

    public void iniciarInclusao(Runnable onSaveCallback) {
        this.pessoaFoco = new Pessoa();
        
        if (Sessao.isActive() && Sessao.tenant() != null) {
            this.pessoaFoco.setTenantId(Sessao.tenant().getId());
        }
        
        this.pessoaFoco.setAtivo(true);
        this.onSaveCallback = onSaveCallback;
        lblTitulo.setText("Nova Pessoa");
    }

    @FXML
    private void onSalvar() {
        // Desabilita temporariamente os campos para evitar cliques duplos (Double Submit)
        setComponentesDisabilitados(true);

        // Captura os dados da tela
        pessoaFoco.setTipo(cbTipo.getValue());
        pessoaFoco.setNomeRazao(txtNomeRazao.getText());
        pessoaFoco.setApelidoFantasia(txtApelidoFantasia.getText());
        pessoaFoco.setCpfCnpj(txtCpfCnpj.getText());
        pessoaFoco.setTelefone(txtTelefone.getText());

        // ⚡ OTIMIZADO: Despacha o processamento pesado para o Pool do Banco
        AppExecutors.getDatabaseExecutor().submit(() -> {
            try {
                // A própria camada de serviço gerencia a transação interna e auditoria de Sessao
                pessoaService.salvarPessoa(pessoaFoco); 
                
                // Retorna para a Thread da UI para atualizar a tela e fechar a janela
                Platform.runLater(() -> {
                    exibirAlerta("Sucesso", "Pessoa salva com êxito no ecossistema!", Alert.AlertType.INFORMATION);
                    if (onSaveCallback != null) {
                        onSaveCallback.run(); 
                    }
                    fecharJanela();
                });

            } catch (IllegalArgumentException e) {
                logger.warn("Falha de validação ao salvar pessoa: {}", e.getMessage());
                Platform.runLater(() -> {
                    exibirAlerta("Validação", e.getMessage(), Alert.AlertType.WARNING);
                    setComponentesDisabilitados(false);
                });
            } catch (SQLException e) {
                logger.error("Erro de persistência SQL ao salvar cadastro de pessoa", e);
                Platform.runLater(() -> {
                    exibirAlerta("Erro de Banco", "Erro ao processar instrução SQL: " + e.getMessage(), Alert.AlertType.ERROR);
                    setComponentesDisabilitados(false);
                });
            } catch (Exception e) {
                logger.error("Erro inesperado no salvamento assíncrono", e);
                Platform.runLater(() -> {
                    exibirAlerta("Erro Crítico", "Falha interna: " + e.getMessage(), Alert.AlertType.ERROR);
                    setComponentesDisabilitados(false);
                });
            }
        });
    }

    @FXML
    private void onCancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        if (txtNomeRazao.getScene() != null && txtNomeRazao.getScene().getWindow() != null) {
            Stage stage = (Stage) txtNomeRazao.getScene().getWindow();
            stage.close();
        }
    }

    private void setComponentesDisabilitados(boolean desabilitar) {
        txtNomeRazao.setDisable(desabilitar);
        txtApelidoFantasia.setDisable(desabilitar);
        txtCpfCnpj.setDisable(desabilitar);
        txtTelefone.setDisable(desabilitar);
        cbTipo.setDisable(desabilitar);
    }

    private void exibirAlerta(String titulo, String conteudo, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}