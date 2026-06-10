package br.com.yat.ecosystemcore.modules.seguranca.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.modules.seguranca.entity.Perfil;
import br.com.yat.ecosystemcore.modules.seguranca.service.PerfilService;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PerfilCadastroController {

	@FXML
	private Label lblTitulo;
	@FXML
	private TextField txtNome;
	@FXML
	private TextField txtChave;
	@FXML
	private TextArea txtDescricao;
	@FXML
	private Button btnExcluir;

	private final PerfilService perfilService = ApplicationContext.getPerfilService();
	private Perfil perfilEdicao;
	private boolean salvoComSucesso = false;

	@FXML
	public void initialize() {
		// Por padrão, esconde o botão de exclusão. Ele só aparece se for uma edição
		// ativa.
		btnExcluir.setVisible(false);
	}

	public void setPerfilParaEdicao(Perfil perfil) {
		if (perfil == null)
			return;

		this.perfilEdicao = perfil;
		lblTitulo.setText("Editar Perfil #" + perfil.getId());
		txtNome.setText(perfil.getNome());
		txtChave.setText(perfil.getChaveIdentificadora());
		txtChave.setDisable(true); // Evita alteração da constraint de negócio do sistema
		txtDescricao.setText(perfil.getDescricao());
		btnExcluir.setVisible(true);
	}

	public boolean isSalvoComSucesso() {
		return salvoComSucesso;
	}

	@FXML
	private void onSalvar() {
		String nome = txtNome.getText();
		String chave = txtChave.getText();
		String descricao = txtDescricao.getText();

		if (nome == null || nome.isBlank() || chave == null || chave.isBlank()) {
			mostrarAlerta("Validação", "Os campos Nome e Chave são obrigatórios.", Alert.AlertType.WARNING);
			return;
		}

		boolean ehEdicao = (perfilEdicao != null);
		Perfil perfil = ehEdicao ? perfilEdicao : new Perfil();
		perfil.setNome(nome.trim());
		perfil.setChaveIdentificadora(chave.trim().toUpperCase());
		perfil.setDescricao(descricao != null ? descricao.trim() : "");

		AppExecutors.execute(() -> {
			try {
				if (ehEdicao) {
					perfilService.atualizarPerfil(perfil);
				} else {
					perfilService.cadastrarPerfil(perfil);
				}

				Platform.runLater(() -> {
					salvoComSucesso = true;
					fecharJanela();
				});
			} catch (Exception e) {
				Platform.runLater(
						() -> mostrarAlerta("Erro", "Erro ao salvar perfil: " + e.getMessage(), Alert.AlertType.ERROR));
			}
		});
	}

	@FXML
	private void onExcluir() {
		if (perfilEdicao == null)
			return;

		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Deseja realmente remover este perfil permanentemente?",
				ButtonType.YES, ButtonType.NO);
		alert.setHeaderText(null);
		alert.showAndWait().ifPresent(resposta -> {
			if (resposta == ButtonType.YES) {

				Long id = perfilEdicao.getId();

				AppExecutors.execute(() -> {
					try {
						perfilService.excluirPerfil(id);
						Platform.runLater(() -> {
							salvoComSucesso = true;
							fecharJanela();
						});
					} catch (Exception e) {
						Platform.runLater(() -> mostrarAlerta("Erro", "Falha ao deletar: " + e.getMessage(),
								Alert.AlertType.ERROR));
					}
				});
			}
		});
	}

	@FXML
	private void onCancelar() {
		fecharJanela();
	}

	private void fecharJanela() {
		Stage stage = (Stage) txtNome.getScene().getWindow();
		if (stage != null) {
			stage.close();
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