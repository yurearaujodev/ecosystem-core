package br.com.yat.ecosystemcore.ui.modules.usuario.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.ui.modules.usuario.entity.Usuario;
import br.com.yat.ecosystemcore.ui.modules.usuario.service.UsuarioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class UsuarioController {

	private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

	@FXML
	private TableView<Usuario> tblUsuarios;
	@FXML
	private TableColumn<Usuario, Long> colId;
	@FXML
	private TableColumn<Usuario, String> colUuid;
	@FXML
	private TableColumn<Usuario, String> colEmail;
	@FXML
	private TableColumn<Usuario, Long> colPessoaId;
	@FXML
	private TableColumn<Usuario, String> colStatus;
	@FXML
	private TextField txtFiltroEmail;

	@FXML
	private TableView<EmpresaUsuarioDetalheDTO> tblEmpresasVinculadas;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, Long> colEmpresaId;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, String> colEmpresaNome;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, Long> colPerfilId;
	@FXML
	private TableColumn<EmpresaUsuarioDetalheDTO, String> colPerfilNome;

	private final ObservableList<EmpresaUsuarioDetalheDTO> detailData = FXCollections.observableArrayList();
	private final ObservableList<Usuario> masterData = FXCollections.observableArrayList();
	private final UsuarioService usuarioService = ApplicationContext.getUsuarioService();

	@FXML
	public void initialize() {
		// Inicialização de colunas mantendo a integridade do seu layout FXML original
		colId.setCellValueFactory(new PropertyValueFactory<>("id"));
		colUuid.setCellValueFactory(new PropertyValueFactory<>("uuidPublico"));
		colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
		colPessoaId.setCellValueFactory(new PropertyValueFactory<>("pessoaId"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
		tblUsuarios.setItems(masterData);

		colEmpresaId.setCellValueFactory(new PropertyValueFactory<>("empresaId"));
		colEmpresaNome.setCellValueFactory(new PropertyValueFactory<>("empresaNome"));
		colPerfilId.setCellValueFactory(new PropertyValueFactory<>("perfilId"));
		colPerfilNome.setCellValueFactory(new PropertyValueFactory<>("perfilNome"));
		tblEmpresasVinculadas.setItems(detailData);

		tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
			if (newVal != null) {
				carregarEmpresasDoUsuario(newVal.getId());
			} else {
				detailData.clear();
			}
		});

		carregarDados();
	}

	private void carregarEmpresasDoUsuario(Long usuarioId) {
		AppExecutors.execute(() -> {
			try {
				List<EmpresaUsuarioDetalheDTO> vinculos = usuarioService.listarVinculosEmpresa(usuarioId);
				Platform.runLater(() -> {
					detailData.clear();
					detailData.addAll(vinculos);
				});
			} catch (Exception e) {
				logger.error("Erro ao carregar vínculos do usuário {}", usuarioId, e);
				Platform.runLater(() -> mostrarAlerta("Erro", e.getMessage(), Alert.AlertType.ERROR));
			}
		});
	}

	private void carregarDados() {
		if (!Sessao.isActive()) {
			mostrarAlerta("Sessão", "Sessão inválida ou expirada.", Alert.AlertType.WARNING);
			return;
		}
		AppExecutors.execute(() -> {
			try {
				List<Usuario> usuarios = usuarioService.listarTodos();
				Platform.runLater(() -> {
					masterData.clear();
					masterData.addAll(usuarios);
				});
			} catch (Exception e) {
				logger.error("Erro ao carregar lista de usuários", e);
				Platform.runLater(() -> mostrarAlerta("Erro", e.getMessage(), Alert.AlertType.ERROR));
			}
		});
	}

	@FXML
	void onAtualizarTabela() {
		carregarDados();
	}

	@FXML
	void onFiltrar() {
		String termo = txtFiltroEmail.getText();
		if (termo == null || termo.isBlank()) {
			tblUsuarios.setItems(masterData);
			return;
		}
		var filtrados = masterData.stream().filter(u -> u.getEmail().toLowerCase().contains(termo.toLowerCase()))
				.toList();
		tblUsuarios.setItems(FXCollections.observableArrayList(filtrados));
	}

	@FXML
	void onDeletarUsuario() {
		if (!Sessao.isActive()) {
			mostrarAlerta("Sessão", "Sessão inválida ou expirada.", Alert.AlertType.WARNING);
			return;
		}
		Usuario selecionado = tblUsuarios.getSelectionModel().getSelectedItem();
		if (selecionado == null) {
			mostrarAlerta("Aviso", "Selecione um usuário para remover.", Alert.AlertType.WARNING);
			return;
		}
		AppExecutors.execute(() -> {
			try {
				usuarioService.deletarUsuario(selecionado.getId());
				Platform.runLater(() -> {
					mostrarAlerta("Sucesso", "Usuário removido com sucesso.", Alert.AlertType.INFORMATION);
					carregarDados();
				});
			} catch (Exception e) {
				logger.error("Falha ao deletar usuário", e);
				Platform.runLater(() -> mostrarAlerta("Erro", e.getMessage(), Alert.AlertType.ERROR));
			}
		});
	}

	@FXML
	void onNovoUsuario() {
		abrirDialogCadastro(null, null);
	}

	@FXML
	void onAlterarUsuario() {
		Usuario usuarioSelecionado = tblUsuarios.getSelectionModel().getSelectedItem();
		if (usuarioSelecionado == null) {
			mostrarAlerta("Seleção Necessária", "Selecione um operador na tabela.", Alert.AlertType.WARNING);
			return;
		}

		Long perfilIdAtual = tblEmpresasVinculadas.getItems().isEmpty() ? null
				: tblEmpresasVinculadas.getItems().get(0).getPerfilId();

		abrirDialogCadastro(usuarioSelecionado, perfilIdAtual);
	}

	private void abrirDialogCadastro(Usuario usuario, Long perfilIdAtual) {
		try {
			var fxmlResource = getClass().getResource("/ui/modules/usuario-cadastro-dialog.fxml");
			FXMLLoader loader = new FXMLLoader(fxmlResource);
			Stage dialogStage = new Stage();
			dialogStage.setTitle(usuario == null ? "Novo Operador" : "Alterar Dados do Operador");
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			dialogStage.setScene(new Scene(loader.load()));

			UsuarioCadastroDialogController controller = loader.getController();
			controller.setStage(dialogStage);

			if (usuario != null) {
				controller.setUsuarioParaEdicao(usuario, perfilIdAtual);
			}

			dialogStage.showAndWait();
			carregarDados();
		} catch (IOException e) {
			logger.error("Erro de I/O ao abrir modal de cadastro", e);
			mostrarAlerta("Erro de UI", "Não foi possível carregar a janela: " + e.getMessage(), Alert.AlertType.ERROR);
		}
	}

	private void mostrarAlerta(String titulo, String msg, Alert.AlertType tipo) {
		Alert a = new Alert(tipo);
		a.setTitle(titulo);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}
}