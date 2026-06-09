package br.com.yat.ecosystemcore.modules.empresa.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.modules.empresa.entity.Empresa;
import br.com.yat.ecosystemcore.modules.empresa.service.EmpresaService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;

public class EmpresaConsultaController {

	@FXML
	private TextField txtPesquisa;
	@FXML
	private Button btnNovaEmpresa;
	@FXML
	private TableView<Empresa> tblEmpresas;
	@FXML
	private TableColumn<Empresa, Long> colId;
	@FXML
	private TableColumn<Empresa, String> colCnpj;
	@FXML
	private TableColumn<Empresa, String> colRazaoSocial;
	@FXML
	private TableColumn<Empresa, String> colNomeFantasia;
	@FXML
	private TableColumn<Empresa, String> colCidade;
	@FXML
	private TableColumn<Empresa, Void> colAcoes;

	private final EmpresaService empresaService = ApplicationContext.getEmpresaService();
	private final ObservableList<Empresa> masterData = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		// 1. Criamos o label com estilo de texto visível e definimos na tabela
		Label placeholderLabel = new Label("Nenhuma empresa cadastrada para esta conta corporativa.");
		placeholderLabel.setStyle("-fx-text-fill: #6C757D; -fx-font-size: 14px;");
		tblEmpresas.setPlaceholder(placeholderLabel);

		// 2. Configuramos as colunas normalmente
		configurarColunas();

		// 3. Buscamos do banco (Despachado assincronamente)
		carregarDados();

		// 4. Montamos o filtro
		configurarFiltroPesquisa();

		// 5. Força o redesenho completo dos nós visuais da tabela no boot
		tblEmpresas.requestLayout();
	}

	private void configurarColunas() {
		colId.setCellValueFactory(new PropertyValueFactory<>("id"));
		colCnpj.setCellValueFactory(new PropertyValueFactory<>("cnpj"));
		colRazaoSocial.setCellValueFactory(new PropertyValueFactory<>("razaoSocial"));
		colNomeFantasia.setCellValueFactory(new PropertyValueFactory<>("nomeFantasia"));

		colCidade.setCellValueFactory(cellData -> {
			Empresa emp = cellData.getValue();

			String cidade = emp.getCidade() != null ? emp.getCidade() : "";
			String estado = emp.getEstado() != null ? emp.getEstado() : "";

			final String resultadoCidadeUf = (cidade.isEmpty() || estado.isEmpty()) ? cidade + estado
					: cidade + "/" + estado;

			return javafx.beans.binding.Bindings.createStringBinding(() -> resultadoCidadeUf);
		});

		adicionarBotoesAcao();
	}

	private void carregarDados() {

		// ⚡ Busca pesada roda no Executor exclusivo de banco, sem congelar a interface
		AppExecutors.execute(() -> {
			try {
				List<Empresa> empresas = empresaService.listarEmpresasDoTenantAtivo();

				Platform.runLater(() -> {
					masterData.clear();
					if (empresas != null && !empresas.isEmpty()) {
						masterData.addAll(empresas);
					}
				});

			} catch (Exception e) {
				System.err.println("❌ ERRO GRAVE NO CARREGAMENTO:");
				e.printStackTrace();

				Platform.runLater(() -> exibirAlertaError("Erro de Carregamento",
						"Não foi possível carregar a listagem.", e.getMessage()));
			}
		});
	}

	private void configurarFiltroPesquisa() {
		FilteredList<Empresa> filteredData = new FilteredList<>(masterData, p -> true);

		txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> {
			filteredData.setPredicate(empresa -> {
				if (newValue == null || newValue.trim().isEmpty()) {
					return true;
				}

				String lowerCaseFilter = newValue.toLowerCase().trim();
				if (empresa.getRazaoSocial().toLowerCase().contains(lowerCaseFilter))
					return true;
				if (empresa.getCnpj().contains(lowerCaseFilter))
					return true;
				if (empresa.getUuidPublico().contains(lowerCaseFilter))
					return true;
				return false;
			});
		});

		tblEmpresas.setItems(filteredData);
		tblEmpresas.refresh();
	}

	private void adicionarBotoesAcao() {
		colAcoes.setCellFactory(new Callback<>() {
			@Override
			public TableCell<Empresa, Void> call(final TableColumn<Empresa, Void> param) {
				return new TableCell<>() {
					private final Button btnEditar = new Button("✏️");
					private final Button btnExcluir = new Button("🗑️");
					private final HBox container = new HBox(8, btnEditar, btnExcluir);

					{
						btnEditar.setStyle(
								"-fx-background-color: #FFC107; -fx-text-fill: black; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 8 4 8;");
						btnExcluir.setStyle(
								"-fx-background-color: #DC3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 8 4 8;");

						btnEditar.setOnAction(event -> {
							Empresa empresa = getTableView().getItems().get(getIndex());
							onEditarEmpresa(empresa);
						});

						btnExcluir.setOnAction(event -> {
							Empresa empresa = getTableView().getItems().get(getIndex());
							onExcluirEmpresa(empresa);
						});
					}

					@Override
					protected void updateItem(Void item, boolean empty) {
						super.updateItem(item, empty);
						setGraphic(empty ? null : container);
					}
				};
			}
		});
	}

	@FXML
	void onNovaEmpresa(ActionEvent event) {
		abrirDialogCadastro(new Empresa());
	}

	private void onEditarEmpresa(Empresa empresa) {
		abrirDialogCadastro(empresa);
	}

	private void abrirDialogCadastro(Empresa empresa) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/modules/empresa-cadastro-dialog.fxml"));
			Parent root = loader.load();

			EmpresaCadastroController controller = loader.getController();
			controller.setEmpresaAlvo(empresa);

			Stage dialogStage = new Stage();
			dialogStage.setTitle(empresa.getId() == null ? "Cadastrar Empresa" : "Modificar Empresa");
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(tblEmpresas.getScene().getWindow());
			dialogStage.setScene(new Scene(root));

			dialogStage.setResizable(false);
			dialogStage.showAndWait();

			if (controller.isSalvoComSucesso()) {
				carregarDados();
			}
		} catch (IOException e) {
			exibirAlertaError("Erro de Infraestrutura", "Não foi possível carregar o arquivo visual do formulário.",
					e.getMessage());
		}
	}

	private void onExcluirEmpresa(Empresa empresa) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Confirmação de Exclusão");
		alert.setHeaderText("Deseja realmente arquivar esta empresa?");
		alert.setContentText(
				"Empresa: " + empresa.getRazaoSocial() + "\nEsta ação poderá ser revertida por administradores.");

		if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			// ⚡ EXCLUSÃO ASSÍNCRONA: Executa fora da thread principal para manter a
			// interface fluida
			AppExecutors.execute(() -> {
				try {
					empresaService.excluirEmpresa(empresa.getId());
					carregarDados();
				} catch (Exception e) {
					Platform.runLater(() -> exibirAlertaError("Falha na Operação",
							"Não foi possível remover logicamente o registro.", e.getMessage()));
				}
			});
		}
	}

	private void exibirAlertaError(String titulo, String header, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(titulo);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}