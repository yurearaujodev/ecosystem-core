package br.com.yat.ecosystemcore.ui.modules.pessoa.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.ui.modules.pessoa.entity.Pessoa;
import br.com.yat.ecosystemcore.ui.modules.pessoa.service.PessoaService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class PessoaCadastroController {

//	private static final Logger logger = LoggerFactory.getLogger(PessoaCadastroController.class);

	@FXML
	private Label lblTitulo;
	@FXML
	private Label lblNomeRazao;
	@FXML
	private Label lblApelidoFantasia;
	@FXML
	private Label lblCpfCnpj;

	@FXML
	private ComboBox<String> cbTipo;
	@FXML
	private TextField txtNomeRazao;
	@FXML
	private TextField txtApelidoFantasia;
	@FXML
	private TextField txtCpfCnpj;
	@FXML
	private TextField txtTelefone;

	private final PessoaService pessoaService = ApplicationContext.getPessoaService();
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
		this.pessoaFoco.setAtivo(true);
		this.onSaveCallback = onSaveCallback;

		if (Sessao.isActive()) {
			this.pessoaFoco.setTenantId(Sessao.tenantId());
		}

		txtNomeRazao.setText("");
		txtApelidoFantasia.setText("");
		txtCpfCnpj.setText("");
		txtTelefone.setText("");

		lblTitulo.setText("Nova Pessoa");
		cbTipo.setValue("FISICA");
		alterarLabelsFormulario("FISICA");

		setComponentesDisabilitados(false);
	}

	@FXML
	private void onSalvar() {
		setComponentesDisabilitados(true);
		pessoaFoco.setTipo(cbTipo.getValue());
		pessoaFoco.setNomeRazao(txtNomeRazao.getText());
		pessoaFoco.setApelidoFantasia(txtApelidoFantasia.getText());
		pessoaFoco.setCpfCnpj(txtCpfCnpj.getText());
		pessoaFoco.setTelefone(txtTelefone.getText());

		AppExecutors.runAsync(() -> pessoaService.salvarPessoa(pessoaFoco)).thenRun(() -> Platform.runLater(() -> {
			exibirAlerta("Sucesso", "Pessoa salva com êxito no ecossistema!", Alert.AlertType.INFORMATION);
			if (onSaveCallback != null) {
				onSaveCallback.run();
			}
			fecharJanela();
		})).exceptionally(ex -> {
			Platform.runLater(() -> {
				Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
				if (causa instanceof IllegalArgumentException iae) {
					exibirAlerta("Validação", iae.getMessage(), Alert.AlertType.WARNING);
				} else {
					exibirAlerta("Erro", causa.getMessage(), Alert.AlertType.ERROR);
				}
				setComponentesDisabilitados(false);
			});
			return null;
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