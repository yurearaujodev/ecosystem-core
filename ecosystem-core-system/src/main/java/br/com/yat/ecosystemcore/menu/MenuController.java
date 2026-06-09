package br.com.yat.ecosystemcore.menu;

import br.com.yat.ecosystemcore.app.ApplicationContext;
import br.com.yat.ecosystemcore.application.menu.MenuProvider;
import br.com.yat.ecosystemcore.application.menu.MenuProviderFactory;
import br.com.yat.ecosystemcore.application.menu.MenuUsuarioContext;
import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import br.com.yat.ecosystemcore.shared.context.SessionContext;
import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.shared.context.UserContext;
import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.core.NavigationManager;
import br.com.yat.ecosystemcore.modules.usuario.service.UsuarioService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

	@FXML
	private MenuBar menuBar;
	@FXML
	private StackPane conteudoCentral;

	@FXML
	private Label lblUsuarioLogado;
	@FXML
	private Label lblHora;
	@FXML
	private Label lblTempoAcesso;
	@FXML
	private Label lblStatusBanco;
	@FXML
	private ImageView imgUsuario;
	@FXML
	private ImageView imgData;
	@FXML
	private ImageView imgLogo;
	@FXML
	private ImageView imgStatusBanco;
	@FXML
	private ComboBox<EmpresaUsuarioDetalheDTO> cbSeletorEmpresa;

	@FXML
	private HBox breadcrumbBar;

	@FXML
	private StackPane commandPaletteOverlay;
	@FXML
	private TextField txtCommandSearch;
	@FXML
	private ListView<String> lstCommands;
	
	@FXML
    private StackPane rootContainer;

	private NavigationManager navigationManager;
	private final MenuProvider menuProvider = MenuProviderFactory.create();
	private final UsuarioService usuarioService = ApplicationContext.getUsuarioService();

	private long segundosLogado = 0;
	private Timeline relogioCorporativo;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		 System.out.println("rootPane = " + rootPane);
		this.navigationManager = new NavigationManager(conteudoCentral);

		navigationManager.setBreadcrumbListener(event -> {

			breadcrumbBar.getChildren().clear();

			Label home = new Label("Home");
			home.setStyle("-fx-text-fill: #6c757d;");

			Label atual = new Label(event.chave().name());
			atual.setStyle("-fx-font-weight: bold;");

			breadcrumbBar.getChildren().addAll(home, new Label(">"), atual);
		});

		exibirDadosSessao();
		configurarSeletorEmpresa();
		iniciarRelogioSistema();
		montarMenu();

		configurarCommandPalette();
		configurarAtalhosGlobais();
	}

	private void configurarCommandPalette() {

		commandPaletteOverlay.setOnMouseClicked(e -> {
			if (e.getTarget() == commandPaletteOverlay) {
				commandPaletteOverlay.setVisible(false);
				commandPaletteOverlay.setManaged(false);
			}
		});

		inicializarComandos();
	}

	private void configurarSeletorEmpresa() {

		SessionContext ctx = SessionScope.get();
		if (ctx == null)
			return;

		Long usuarioId = ctx.getUsuarioId();
		if (usuarioId == null)
			return;

		AppExecutors.execute(() -> {
			try {
				List<EmpresaUsuarioDetalheDTO> vinculos = usuarioService.listarVinculosEmpresa(usuarioId);

				Platform.runLater(() -> {
					cbSeletorEmpresa.getItems().setAll(vinculos);

					Long empresaAtual = ctx.getEmpresaAtivaId();

					if (empresaAtual != null) {
						cbSeletorEmpresa.getSelectionModel().select(vinculos.stream()
								.filter(v -> empresaAtual.equals(v.getEmpresaId())).findFirst().orElse(null));
					}
				});

			} catch (Exception e) {
				e.printStackTrace(); // 🔥 ESSENCIAL pra você enxergar isso
			}
		});

		cbSeletorEmpresa.setOnAction(e -> {
			EmpresaUsuarioDetalheDTO selecao = cbSeletorEmpresa.getValue();
			if (selecao == null)
				return;

			AppExecutors.execute(() -> {
				try {
					SessionScope.setEmpresa(selecao.getEmpresaId());

					Platform.runLater(() -> {
						navigationManager.onGlobalContextChanged();
					});

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		});
	}

	@FXML
	private BorderPane rootPane;

	@FXML
	private void onHome() {
		navigationManager.navigatePara(MenuChave.DASHBOARD);
	}

	@FXML
	private void onRefresh() {
		navigationManager.forcarLimpezaCache();
		navigationManager.navigatePara(MenuChave.DASHBOARD);
	}

	@FXML
	private void openCommandPalette() {

		commandPaletteOverlay.setVisible(true);
		commandPaletteOverlay.setManaged(true);

		txtCommandSearch.requestFocus();
	}

	private void inicializarComandos() {

		lstCommands.getItems().setAll("home", "empresa", "logout", "refresh", "usuarios", "cadastros");

		lstCommands.setOnMouseClicked(e -> {
			String cmd = lstCommands.getSelectionModel().getSelectedItem();
			if (cmd != null) {
				executarComando(cmd);
				commandPaletteOverlay.setVisible(false);
				commandPaletteOverlay.setManaged(false);
			}
		});
	}
	
	private void configurarAtalhosGlobais() {
        // Define a combinação: CTRL + K (ou COMMAND + K se rodar no Mac)
        KeyCombination atalhoPalette = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);

        // Aplica o listener diretamente na janela raiz
        rootContainer.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (atalhoPalette.match(event)) {
                openCommandPalette();
                event.consume(); // Evita que o evento dispare outras funções nativas do sistema
            }
        });
    }

	@FXML
	private void onCommandKey(KeyEvent event) {

		switch (event.getCode()) {

		case ESCAPE -> {
			commandPaletteOverlay.setVisible(false);
			commandPaletteOverlay.setManaged(false);
		}

		case ENTER -> {
			String cmd = txtCommandSearch.getText();

			if (cmd == null || cmd.isBlank())
				return;

			executarComando(cmd);

			commandPaletteOverlay.setVisible(false);
			commandPaletteOverlay.setManaged(false);
		}
		}
	}

	public void atualizarBreadcrumb(String... path) {

		breadcrumbBar.getChildren().clear();

		for (int i = 0; i < path.length; i++) {

			Label lbl = new Label(path[i]);
			lbl.setStyle("-fx-text-fill: #495057; -fx-font-weight: bold;");

			breadcrumbBar.getChildren().add(lbl);

			if (i < path.length - 1) {
				breadcrumbBar.getChildren().add(new Label(">"));
			}
		}
	}

	private void executarComando(String cmd) {

		if (cmd == null)
			return;

		cmd = cmd.toLowerCase();

		switch (cmd) {

		case "logout" -> onLogout();

		case "empresa" -> cbSeletorEmpresa.requestFocus();

		case "home" -> navigationManager.navigatePara(MenuChave.DASHBOARD);

		default -> System.out.println("Comando desconhecido: " + cmd);
		}
	}

	private void montarMenu() {

		SessionContext ctx = SessionScope.get();
		if (ctx == null) {
			System.err.println("Sessão inválida - menu ignorado");
			return;
		}

		MenuUsuarioContext context = MenuUsuarioContext.fromSession();
		if (context == null || context.usuarioId() == null) {
			System.err.println("Contexto inválido - menu ignorado");
			return;
		}

		Map<String, Menu> menus = new LinkedHashMap<>();

		for (MenuPermitidoDTO dto : menuProvider.carregarMenus(context)) {

			MenuChave chave = dto.resolverChaveNavegacao().orElse(null);
			if (chave == null)
				continue;

			Menu menuPai = menus.computeIfAbsent(dto.moduloNome(), Menu::new);

			MenuItem item = new MenuItem(dto.menuNome());
			item.setUserData(chave);
			item.setOnAction(this::onNavigate);

			menuPai.getItems().add(item);
		}

		menuBar.getMenus().setAll(menus.values());
	}

	private void exibirDadosSessao() {

		UserContext user = Sessao.user();

		if (user != null) {
			lblUsuarioLogado.setText(user.getEmail());
		} else {
			lblUsuarioLogado.setText("Usuário Anônimo");
		}

		lblStatusBanco.setText("CONECTADO (HIKARI)");
		lblStatusBanco.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2B8A3E;");
	}

	private void iniciarRelogioSistema() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

		relogioCorporativo = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
			lblHora.setText(LocalDateTime.now().format(formatter));
			segundosLogado++;
			long h = segundosLogado / 3600;
			long m = (segundosLogado % 3600) / 60;
			long s = segundosLogado % 60;
			lblTempoAcesso.setText(String.format("%02d:%02d:%02d", h, m, s));
		}));

		relogioCorporativo.setCycleCount(Animation.INDEFINITE);
		relogioCorporativo.play();
	}

	@FXML
	private void onNavigate(ActionEvent event) {
		MenuItem item = (MenuItem) event.getSource();
		if (item.getUserData() instanceof MenuChave chave) {
			navigationManager.navigatePara(chave);
		}
	}

	@FXML
	public void onLogout() {

		if (relogioCorporativo != null) {
			relogioCorporativo.stop();
		}

		navigationManager.forcarLimpezaCache();

		Sessao.logout();

		SessionScope.close();

		Platform.exit();
	}

	private SessionContext session() {
		return SessionScope.get();
	}

	public boolean hasSession() {
		return session() != null;
	}
}