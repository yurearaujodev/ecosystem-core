package br.com.yat.ecosystemcore.modules.usuario.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.application.system.dto.AtualizarDetalhesUsuarioCommand;
import br.com.yat.ecosystemcore.application.system.dto.UsuarioSegurancaConfigDTO;
import br.com.yat.ecosystemcore.modules.usuario.dto.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.modules.usuario.service.EmpresaUsuarioService;
import br.com.yat.ecosystemcore.modules.usuario.service.SalvarDetalhesSegurancaUsuarioUseCase;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.database.TransactionManager;
import br.com.yat.ecosystemcore.modules.navegacao.service.ScreenLifecycle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioGerenciamentoAbasController implements ScreenLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(UsuarioGerenciamentoAbasController.class);

	@FXML
	private ComboBox<CustomItem> comboUsuarios;
	@FXML
	private TabPane tabPaneCentral;

	// Aba 1 componentes
	@FXML
	private ComboBox<CustomItem> comboEmpresas;
	@FXML
	private ComboBox<CustomItem> comboPerfis;
	@FXML
	private TableView<EmpresaUsuarioDetalheDTO> tableVinculos;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, String> colEmpresaNome;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, String> colPerfilNome;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, Void> colAcoes;

	// Aba 2 componentes
	@FXML
	private ListView<CheckBoxListCellData> listViewPermissoesExtras;

	// Aba 3 componentes
	@FXML
	private CheckBox chkRequerNovaSenha;
	@FXML
	private CheckBox chkAceitaForaEmpresa;
	@FXML
	private CheckBox chkMultiplasSessoes;
	@FXML
	private TextField txtIpEstatico;

	@FXML
	private ProgressIndicator progressLoader;
	@FXML
	private Button btnSalvar;

	private final EmpresaUsuarioService service = ApplicationContext.getEmpresaUsuarioService();
	private final SalvarDetalhesSegurancaUsuarioUseCase salvarUseCase = ApplicationContext
			.getSalvarDetalhesSegurancaUsuarioUseCase();
	private final ObservableList<EmpresaUsuarioDetalheDTO> vinculosTabela = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		configurarTabelas();
		configurarListViewPermissoes();

		// Listener reativo: Carrega os dados das tabelas assim que um usuário é
		// selecionado
		comboUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
			if (novo != null) {
				carregarDadosDoUsuario(novo.id());
			} else {
				tabPaneCentral.setDisable(true);
			}
		});
	}

	private void configurarTabelas() {
		colEmpresaNome.setCellValueFactory(new PropertyValueFactory<>("empresaNome"));
		colPerfilNome.setCellValueFactory(new PropertyValueFactory<>("perfilNome"));
		tableVinculos.setItems(vinculosTabela);

		// Criação dinâmica do botão de remoção em linha na tabela (JavaFX Nativo)
		colAcoes.setCellFactory(param -> new TableCell<>() {
			private final Button btn = new Button("Remover");
			{
				btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 3 8 3 8;");
				btn.setOnAction(event -> {
					EmpresaUsuarioDetalheDTO item = getTableView().getItems().get(getIndex());
					vinculosTabela.remove(item);
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btn);
			}
		});
	}

	private void configurarListViewPermissoes() {
		// 🛠️ FIX: Configura a CellFactory para renderizar o CheckBox real na UI do
		// JavaFX
		listViewPermissoesExtras.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(CheckBoxListCellData item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
				} else {
					setGraphic(item.getCheckBox());
				}
			}
		});
	}

	@Override
	public void onShow() {
		setCarregando(true);
		AppExecutors.runAsync(() -> {
			List<CustomItem> usuarios = service.buscarTodosUsuariosAtivos();
			List<CustomItem> empresas = service.buscarEmpresasDoTenant();
			List<CustomItem> perfis = service.buscarPerfisDisponiveis();
			List<CheckBoxListCellData> permissoes = service.buscarTodasPermissoes();

			Platform.runLater(() -> {

				comboUsuarios.setItems(FXCollections.observableArrayList(usuarios != null ? usuarios : List.of()));

				comboEmpresas.setItems(FXCollections.observableArrayList(empresas != null ? empresas : List.of()));

				comboPerfis.setItems(FXCollections.observableArrayList(perfis != null ? perfis : List.of()));

				listViewPermissoesExtras
						.setItems(FXCollections.observableArrayList(permissoes != null ? permissoes : List.of()));

				setCarregando(false);
			});

		}).exceptionally(ex -> {

			logger.error("Erro ao carregar dados", ex);

			Platform.runLater(() -> {
				setCarregando(false);
				mostrarAlerta("Erro de Carregamento",
						ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
			});

			return null;
		});
	}

	private void carregarDadosDoUsuario(Long usuarioId) {
		setCarregando(true);
		tabPaneCentral.setDisable(true);

		AppExecutors.runAsync(() -> {
			List<EmpresaUsuarioDetalheDTO> vinculos = service.listarVinculosDoUsuario(usuarioId);
			List<Long> idsExtras = service.listarIdsPermissoesExtrasDoUsuario(usuarioId);
			UsuarioSegurancaConfigDTO segConfig = service.buscarConfiguracoesDeSeguranca(usuarioId);
			Platform.runLater(() -> {
				vinculosTabela.setAll(vinculos);
				for (CheckBoxListCellData cell : listViewPermissoesExtras.getItems()) {
					cell.setSelected(idsExtras.contains(cell.id()));
				}
				chkRequerNovaSenha.setSelected(segConfig.requerNovaSenha());
				chkAceitaForaEmpresa.setSelected(segConfig.aceitaAcessoForaEmpresa());
				chkMultiplasSessoes.setSelected(segConfig.permitirMultiplasSessoes());
				txtIpEstatico.setText(segConfig.ipEstaticoObrigatorio());

				tabPaneCentral.setDisable(false);
				setCarregando(false);
			});

		}).exceptionally(ex -> {
			logger.error("Falha carregando usuário {}", usuarioId, ex);
			Platform.runLater(() -> {
				setCarregando(false);
				mostrarAlerta("Erro", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
			});

			return null;
		});
	}

	@FXML
	private void handleAdicionarVinculo() {
		CustomItem emp = comboEmpresas.getSelectionModel().getSelectedItem();
		CustomItem perf = comboPerfis.getSelectionModel().getSelectedItem();

		if (emp == null || perf == null) {
			mostrarAlerta("Campos Obrigatórios", "Selecione tanto a Empresa quanto o Perfil para vincular.");
			return;
		}

		boolean jaExiste = vinculosTabela.stream().anyMatch(v -> v.getEmpresaId().equals(emp.id()));
		if (jaExiste) {
			mostrarAlerta("Vínculo Existente", "Este usuário já possui um perfil mapeado nesta empresa.");
			return;
		}

		vinculosTabela.add(new EmpresaUsuarioDetalheDTO(emp.id(), emp.nome(), perf.id(), perf.nome()));
	}

	@FXML
	private void handleSalvar() {
		CustomItem usr = comboUsuarios.getSelectionModel().getSelectedItem();
		if (usr == null)
			return;

		setCarregando(true);

		List<Long> extrasMarcados = new ArrayList<>();
		for (CheckBoxListCellData item : listViewPermissoesExtras.getItems()) {
			if (item.isSelected())
				extrasMarcados.add(item.id());
		}

		UsuarioSegurancaConfigDTO dtoConfig = new UsuarioSegurancaConfigDTO(chkRequerNovaSenha.isSelected(),
				chkAceitaForaEmpresa.isSelected(), txtIpEstatico.getText(), chkMultiplasSessoes.isSelected());

		AtualizarDetalhesUsuarioCommand command = new AtualizarDetalhesUsuarioCommand(usr.id(),
				new ArrayList<>(vinculosTabela), extrasMarcados, dtoConfig);

		AppExecutors.runAsync(() -> {
			salvarUseCase.execute(command);
		}).thenRun(() -> Platform.runLater(() -> {
			setCarregando(false);
			mostrarInformativo("Sucesso", "Todas as políticas e vínculos de segurança foram atualizados.");
		})).exceptionally(ex -> {
			logger.error("Falha ao salvar dados do usuário", ex);

			Platform.runLater(() -> {
				setCarregando(false);
				mostrarAlerta("Erro de Persistência",
						ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
			});

			return null;
		});
	}

	@FXML
	private void handleCancelar() {
		comboUsuarios.getSelectionModel().clearSelection();
	}

	@Override
	public void onHide() {
		logger.debug("Ocultando tela de gestão fina de segurança.");
	}

	@Override
	public void onDestroy() {
		logger.debug("Destruindo tela e liberando escuta do JavaFX.");
	}

	private void setCarregando(boolean l) {
		progressLoader.setVisible(l);
		btnSalvar.setDisable(l);
	}

	private void mostrarAlerta(String tit, String msg) {
		Alert a = new Alert(Alert.AlertType.WARNING);
		a.setTitle(tit);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}

	private void mostrarInformativo(String tit, String msg) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(tit);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}

	public record CustomItem(Long id, String nome) {
		@Override
		public String toString() {
			return nome;
		}
	}

	public static class CheckBoxListCellData {
		private final Long id;
		private final CheckBox checkBox;

		public CheckBoxListCellData(Long id, String chave, String desc) {
			this.id = id;
			this.checkBox = new CheckBox(chave + " (" + desc + ")");
		}

		public Long id() {
			return id;
		}

		public boolean isSelected() {
			return checkBox.isSelected();
		}

		public void setSelected(boolean val) {
			checkBox.setSelected(val);
		}

		// 🛠️ Auxiliar exposto para a CellFactory renderizar graficamente o CheckBox
		public CheckBox getCheckBox() {
			return checkBox;
		}

		@Override
		public String toString() {
			return checkBox.getText();
		}
	}
}