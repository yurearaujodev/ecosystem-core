package br.com.yat.ecosystemcore.ui.modules.pessoa;

import br.com.yat.ecosystemcore.domain.entity.Pessoa;
import br.com.yat.ecosystemcore.service.external.PessoaService;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class PessoaCadastroController {

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
        // 🛠️ MUDANÇA: Captura dinâmica do Tenant da conta que logou
        this.pessoaFoco.setTenantId(SessionManager.getTenantAtual().getId());
        this.pessoaFoco.setAtivo(true);
        this.onSaveCallback = onSaveCallback;
        lblTitulo.setText("Nova Pessoa");
    }

    @FXML
    private void onSalvar() {
        try {
            pessoaFoco.setTipo(cbTipo.getValue());
            pessoaFoco.setNomeRazao(txtNomeRazao.getText());
            pessoaFoco.setApelidoFantasia(txtApelidoFantasia.getText());
            pessoaFoco.setCpfCnpj(txtCpfCnpj.getText());
            pessoaFoco.setTelefone(txtTelefone.getText());

            // 🛠️ MUDANÇA: Passando o ID do usuário real logado mapeado pelo SessionManager
            Long usuarioLogadoId = SessionManager.getUsuarioLogado().getId();
            pessoaService.salvarPessoa(pessoaFoco, usuarioLogadoId);
            
            exibirAlerta("Sucesso", "Pessoa salva com êxito no ecossistema!", Alert.AlertType.INFORMATION);
            
            if (onSaveCallback != null) {
                onSaveCallback.run(); 
            }
            fecharJanela();
            
        } catch (IllegalArgumentException e) {
            exibirAlerta("Validação", e.getMessage(), Alert.AlertType.WARNING);
        } catch (SQLException e) {
            exibirAlerta("Erro de Banco", "Erro ao processar instrução SQL: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onCancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) txtNomeRazao.getScene().getWindow();
        stage.close();
    }

    private void exibirAlerta(String titulo, String conteudo, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}