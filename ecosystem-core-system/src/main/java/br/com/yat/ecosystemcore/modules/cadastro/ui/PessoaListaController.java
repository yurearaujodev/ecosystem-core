package br.com.yat.ecosystemcore.modules.cadastro.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.modules.cadastro.entity.Pessoa;
import br.com.yat.ecosystemcore.modules.cadastro.service.PessoaService;
import br.com.yat.ecosystemcore.modules.navegacao.service.ContextAware;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PessoaListaController implements ContextAware {

	@FXML
	private TextField txtPesquisa;
	@FXML
	private TableView<Pessoa> tblPessoas;
	@FXML
	private TableColumn<Pessoa, Long> colId;
	@FXML
	private TableColumn<Pessoa, String> colTipo;
	@FXML
	private TableColumn<Pessoa, String> colNome;
	@FXML
	private TableColumn<Pessoa, String> colCpfCnpj;
	@FXML
	private TableColumn<Pessoa, String> colTelefone;
	@FXML
	private TableColumn<Pessoa, String> colStatus;

	private final PessoaService pessoaService = ApplicationContext.getPessoaService();
	private final ObservableList<Pessoa> masterData = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		configurarColunas();
		carregarDados();
	}

	@Override
	public void onContextChanged() {
		carregarDados();
	}

	private void configurarColunas() {
		// Usando expressões lambda explícitas para evitar reflexão pesada e erros em
		// tempo de execução
		colId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
		colTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTipo()));
		colNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomeRazao()));
		colCpfCnpj.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCpfCnpj()));
		colTelefone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTelefone()));

		colStatus.setCellValueFactory(cellData -> {
			boolean ativo = cellData.getValue().isAtivo();
			return new SimpleStringProperty(ativo ? "Ativo" : "Inativo");
		});
	}

	private void carregarDados() {
		AppExecutors.supplyAsync(pessoaService::listarTodas).thenAccept(pessoas -> Platform.runLater(() -> {
			masterData.clear();
			masterData.setAll(pessoas);
			tblPessoas.setItems(masterData);
			// tblPessoas.refresh();
			aplicarFiltroAtual();
		})).exceptionally(ex -> {
			Platform.runLater(() -> exibirAlerta("Erro",
					ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), Alert.AlertType.ERROR));
			return null;
		});
	}

	private void aplicarFiltroAtual() {

		String termo = txtPesquisa.getText();

		if (termo == null || termo.isBlank()) {
			tblPessoas.setItems(masterData);
			return;
		}

		onPesquisar();
	}

	@FXML
	private void onPesquisar() {
		String termo = txtPesquisa.getText();

		if (termo == null || termo.isBlank()) {
			tblPessoas.setItems(masterData);
			return;
		}

		String busca = termo.toLowerCase().trim();

		tblPessoas.setItems(
				masterData.filtered(p -> (p.getNomeRazao() != null && p.getNomeRazao().toLowerCase().contains(busca))
						|| (p.getCpfCnpj() != null && p.getCpfCnpj().contains(busca))
						|| (p.getApelidoFantasia() != null && p.getApelidoFantasia().toLowerCase().contains(busca))));
	}

	@FXML
	private void onNovo() {
		abrirFormulario(null);
	}

	@FXML
	private void onAlterar() {
		Pessoa selecionada = tblPessoas.getSelectionModel().getSelectedItem();
		if (selecionada == null) {
			boxAvisoSelecao();
			return;
		}
		abrirFormulario(selecionada);
	}

	private void abrirFormulario(Pessoa pessoa) {
		try {
			var loader = new FXMLLoader(getClass().getResource("/ui/modules/pessoa-cadastro.fxml"));
			Parent root = loader.load();
			PessoaCadastroController controller = loader.getController();

			if (pessoa == null) {
				controller.iniciarInclusao(this::carregarDados);
			} else {
				controller.iniciarEdicao(pessoa, this::carregarDados);
			}
			Stage stage = new Stage();
			stage.setScene(new Scene(root));
			stage.setTitle(pessoa == null ? "Nova Pessoa" : "Alterar Pessoa");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.showAndWait();
		} catch (Exception e) {
			String mensagemErro = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
			exibirAlerta("Erro", "Erro ao carregar layout do formulário: " + mensagemErro, Alert.AlertType.ERROR);
		}
	}

	@FXML
	private void onExcluir() {
		Pessoa pessoa = tblPessoas.getSelectionModel().getSelectedItem();
		if (pessoa == null) {
			boxAvisoSelecao();
			return;
		}

		Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
				"Deseja mesmo arquivar a pessoa " + pessoa.getNomeRazao() + "?", ButtonType.YES, ButtonType.NO);

		confirmacao.showAndWait().ifPresent(resposta -> {
			if (resposta != ButtonType.YES) {
				return;
			}

			AppExecutors.runAsync(() -> pessoaService.deletarPessoa(pessoa.getId())).thenRun(() -> {
				Platform.runLater(() -> {
					carregarDados();
					exibirAlerta("Sucesso", "Pessoa excluída com sucesso!", Alert.AlertType.INFORMATION);
				});
			}).exceptionally(ex -> {
				Platform.runLater(() -> exibirAlerta("Erro",
						ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), Alert.AlertType.ERROR));
				return null;
			});
		});
	}

	private void boxAvisoSelecao() {
		exibirAlerta("Aviso", "Selecione uma pessoa na tabela para realizar esta ação.", Alert.AlertType.WARNING);
	}

	private void exibirAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
		Alert alert = new Alert(tipo);
		alert.setTitle(titulo);
		alert.setHeaderText(null);
		alert.setContentText(mensagem);
		alert.showAndWait();
	}
}