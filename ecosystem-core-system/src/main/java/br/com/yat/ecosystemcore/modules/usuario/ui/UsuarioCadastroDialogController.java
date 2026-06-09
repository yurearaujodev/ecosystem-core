package br.com.yat.ecosystemcore.modules.usuario.ui;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.application.usuario.BCryptPasswordEncoder;
import br.com.yat.ecosystemcore.application.usuario.PasswordEncoder;
import br.com.yat.ecosystemcore.domain.entity.*;
import br.com.yat.ecosystemcore.service.external.*;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.util.PasswordExtractor;
import br.com.yat.ecosystemcore.modules.empresa.entity.Empresa;
import br.com.yat.ecosystemcore.modules.empresa.service.EmpresaService;
import br.com.yat.ecosystemcore.modules.pessoa.entity.Pessoa;
import br.com.yat.ecosystemcore.modules.pessoa.service.PessoaService;
import br.com.yat.ecosystemcore.modules.usuario.entity.Usuario;
import br.com.yat.ecosystemcore.modules.usuario.service.UsuarioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UsuarioCadastroDialogController {

	private static final Logger logger = LoggerFactory.getLogger(UsuarioCadastroDialogController.class);

	@FXML
	private TextField txtPessoaNome;
	@FXML
	private ComboBox<Empresa> cmbEmpresa;
	@FXML
	private ComboBox<Perfil> cmbPerfil;
	@FXML
	private TextField txtEmail;
	@FXML
	private PasswordField txtSenha;
	@FXML
	private Button btnSalvar;
	@FXML
	private TextField txtMacAddress;
	@FXML
	private CheckBox chkConsentimento;

	private Stage stage;
	private Pessoa pessoaSelecionada;
	private Usuario usuarioEmEdicao;

	private final UsuarioService usuarioService = ApplicationContext.getUsuarioService();
	private final PerfilService perfilService = new PerfilService();
	private final EmpresaService empresaService = ApplicationContext.getEmpresaService();
	private final PessoaService pessoaService = ApplicationContext.getPessoaService();
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public void setStage(Stage stage) {
		this.stage = stage;
		inicializarCombosDinamicos();
	}

	public void setUsuarioParaEdicao(Usuario usuario, Long perfilIdAtual) {
		this.usuarioEmEdicao = usuario;

		Platform.runLater(() -> {
			btnSalvar.setText("💾 Salvar Alterações");
			txtEmail.setText(usuario.getEmail());
			txtMacAddress.setText(usuario.getMacAddressAutorizado());
			chkConsentimento.setSelected(usuario.isConsentimentoDados());
			txtSenha.setPromptText("(Deixe em branco para manter a atual)");

			AppExecutors.execute(() -> {
				try {
					List<Pessoa> todasPessoas = pessoaService.listarTodas();

					todasPessoas.stream().filter(p -> p.getId().equals(usuario.getPessoaId())).findFirst()
							.ifPresent(p -> Platform.runLater(() -> treePessoaConverter(p)));

					Platform.runLater(() -> {
						cmbEmpresa.getItems().stream().filter(e -> e.getId().equals(usuario.getEmpresaPadraoId()))
								.findFirst().ifPresent(cmbEmpresa.getSelectionModel()::select);
						cmbPerfil.getItems().stream().filter(p -> p.getId().equals(perfilIdAtual)).findFirst()
								.ifPresent(cmbPerfil.getSelectionModel()::select);
					});
				} catch (Exception e) {
					logger.error("Erro ao carregar dados para edição", e);
				}
			});
		});
	}

	private void inicializarCombosDinamicos() {
		cmbPerfil.setConverter(createConverter(Perfil::getNome));
		cmbEmpresa.setConverter(createConverter(Empresa::getRazaoSocial));

		AppExecutors.execute(() -> {
			try {
				List<Perfil> perfis = perfilService.listarPerfisPorTenant(Sessao.tenantId());
				List<Empresa> empresas = empresaService.listarEmpresasDoTenantAtivo();
				Platform.runLater(() -> {
					cmbPerfil.setItems(FXCollections.observableArrayList(perfis));
					cmbEmpresa.setItems(FXCollections.observableArrayList(empresas));
				});
			} catch (Exception e) {
				logger.error("Falha ao inicializar combos", e);
			}
		});
	}

	@FXML
	void onBuscarPessoa() {
		AppExecutors.execute(() -> {
			try {
				List<Pessoa> pessoas = pessoaService.listarTodas();
				Platform.runLater(() -> {
					if (pessoas.isEmpty())
						return;
					ChoiceDialog<Pessoa> dialog = new ChoiceDialog<>(pessoas.get(0), pessoas);
					dialog.showAndWait().ifPresent(this::treePessoaConverter);
				});
			} catch (Exception e) {
				logger.error("Erro ao buscar pessoas", e);
			}
		});
	}

	@FXML
	void onSalvar() {

		if (pessoaSelecionada == null || cmbEmpresa.getSelectionModel().isEmpty()
				|| cmbPerfil.getSelectionModel().isEmpty()) {

			mostrarAlerta("Campos Obrigatórios", "Preencha todos os campos.");
			return;
		}

		Empresa empresaSelecionada = cmbEmpresa.getValue();

		Perfil perfilSelecionado = cmbPerfil.getValue();

		String email = txtEmail.getText().trim();

		char[] senha = PasswordExtractor.extrair(txtSenha);

		String mac = txtMacAddress.getText();

		boolean consentimento = chkConsentimento.isSelected();

		btnSalvar.setDisable(true);

		AppExecutors.execute(() -> {

			try {

				Usuario user = usuarioEmEdicao != null ? usuarioEmEdicao : new Usuario();

				user.setTenantId(Sessao.tenantId());
				user.setPessoaId(pessoaSelecionada.getId());
				user.setEmpresaPadraoId(empresaSelecionada.getId());
				user.setEmail(email);
				user.setMacAddressAutorizado(mac);
				user.setConsentimentoDados(consentimento);

				if (senha != null && senha.length > 0) {
					user.setSenhaHash(passwordEncoder.encode(senha));
				}

				EmpresaUsuarioDetalheDTO vinculo = new EmpresaUsuarioDetalheDTO();

				vinculo.setEmpresaId(empresaSelecionada.getId());

				vinculo.setPerfilId(perfilSelecionado.getId());

				usuarioService.salvarUsuario(user, List.of(vinculo));

				Platform.runLater(stage::close);

			} catch (Exception e) {

				logger.error("Erro ao salvar", e);

				Platform.runLater(() -> {
					btnSalvar.setDisable(false);
					mostrarAlerta("Erro", e.getMessage());
				});
			}
		});
	}

	private <T> StringConverter<T> createConverter(java.util.function.Function<T, String> toString) {
		return new StringConverter<>() {
			public String toString(T object) {
				return object != null ? toString.apply(object) : "";
			}

			public T fromString(String string) {
				return null;
			}
		};
	}

	private void treePessoaConverter(Pessoa p) {
		this.pessoaSelecionada = p;
		this.txtPessoaNome.setText(p.getNomeRazao());
	}

	private void mostrarAlerta(String h, String c) {
		new Alert(Alert.AlertType.ERROR, c).showAndWait();
	}

	@FXML
	void onCancelar() {
		stage.close();
	}
}