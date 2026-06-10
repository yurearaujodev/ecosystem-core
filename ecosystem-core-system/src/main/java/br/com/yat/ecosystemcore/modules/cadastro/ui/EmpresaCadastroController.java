package br.com.yat.ecosystemcore.modules.cadastro.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Empresa;
import br.com.yat.ecosystemcore.modules.cadastro.service.EmpresaService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EmpresaCadastroController {

	@FXML
	private Label lblTituloFormulario;
	@FXML
	private TextField txtCnpj;
	@FXML
	private TextField txtInscricaoEstadual;
	@FXML
	private TextField txtRazaoSocial;
	@FXML
	private TextField txtNomeFantasia;
	@FXML
	private TextField txtTelefone;
	@FXML
	private TextField txtCep;
	@FXML
	private TextField txtLogradouro;
	@FXML
	private TextField txtNumero;
	@FXML
	private TextField txtBairro;
	@FXML
	private TextField txtCidade;
	@FXML
	private TextField txtEstado;
	@FXML
	private Button btnCancelar;
	@FXML
	private Button btnSalvar;

	private final EmpresaService empresaService = ApplicationContext.getEmpresaService();
	private Empresa empresaAlvo;
	private boolean salvoComSucesso = false;

	/**
	 * Prepara o formulário recebendo uma entidade vazia (Inclusão) ou populada
	 * (Edição).
	 */
	public void setEmpresaAlvo(Empresa empresa) {
		this.empresaAlvo = empresa;

		if (empresa.getId() == null) {
			lblTituloFormulario.setText("🏢 Nova Unidade de Negócio");
		} else {
			lblTituloFormulario.setText("✏️ Editar: " + empresa.getNomeFantasia());
			preencherCamposDaTela();
		}
	}

	@FXML
	void onSalvar(ActionEvent event) {

		try {

			sincronizarCamposParaEntidade();

			empresaService.salvarEmpresa(empresaAlvo);

			salvoComSucesso = true;

			fecharJanela();

		} catch (IllegalArgumentException e) {

			exibirAlertaAviso("Validação de Campos", e.getMessage());

		} catch (IllegalStateException e) {

			exibirAlertaAviso("Sessão Inválida", e.getMessage());

		} catch (Exception e) {

			System.err.println("❌ ERRO AO PERSISTIR EMPRESA NO BANCO:");

			e.printStackTrace();

			exibirAlertaErro("Falha Operacional", "Erro ao persistir registro no banco.", e.getMessage());
		}
	}

	public boolean isSalvoComSucesso() {
		return salvoComSucesso;
	}

	private void preencherCamposDaTela() {
		txtCnpj.setText(empresaAlvo.getCnpj());
		txtInscricaoEstadual.setText(empresaAlvo.getInscricaoEstadual());
		txtRazaoSocial.setText(empresaAlvo.getRazaoSocial());
		txtNomeFantasia.setText(empresaAlvo.getNomeFantasia());
		txtTelefone.setText(empresaAlvo.getTelefone());
		txtCep.setText(empresaAlvo.getCep());
		txtLogradouro.setText(empresaAlvo.getLogradouro());
		txtNumero.setText(empresaAlvo.getNumero());
		txtBairro.setText(empresaAlvo.getBairro());
		txtCidade.setText(empresaAlvo.getCidade());
		txtEstado.setText(empresaAlvo.getEstado());
	}

	private void sincronizarCamposParaEntidade() {
		String cnpjLimpo = txtCnpj.getText() != null ? txtCnpj.getText().replaceAll("\\D", "") : "";

		empresaAlvo.setCnpj(cnpjLimpo);
		empresaAlvo.setInscricaoEstadual(txtInscricaoEstadual.getText());
		empresaAlvo.setRazaoSocial(txtRazaoSocial.getText());
		empresaAlvo.setNomeFantasia(txtNomeFantasia.getText());
		empresaAlvo.setTelefone(txtTelefone.getText());
		empresaAlvo.setCep(txtCep.getText());
		empresaAlvo.setLogradouro(txtLogradouro.getText());
		empresaAlvo.setNumero(txtNumero.getText());
		empresaAlvo.setBairro(txtBairro.getText());
		empresaAlvo.setCidade(txtCidade.getText());
		empresaAlvo.setEstado(txtEstado.getText());
	}

	@FXML
	void onCancelar(ActionEvent event) {
		fecharJanela();
	}

	private void fecharJanela() {
		Stage stage = (Stage) btnCancelar.getScene().getWindow();
		stage.close();
	}

	private void exibirAlertaAviso(String cabecalho, String mensagem) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Atenção");
		alert.setHeaderText(cabecalho);
		alert.setContentText(mensagem);
		alert.showAndWait();
	}

	private void exibirAlertaErro(String cabecalho, String prompt, String detalhe) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Erro");
		alert.setHeaderText(cabecalho);
		alert.setContentText(prompt + "\n\nDetalhes: " + detalhe);
		alert.showAndWait();
	}
}