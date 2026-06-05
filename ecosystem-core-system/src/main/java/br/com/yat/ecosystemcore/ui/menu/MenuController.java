package br.com.yat.ecosystemcore.ui.menu;

import br.com.yat.ecosystemcore.application.menu.MenuProvider;
import br.com.yat.ecosystemcore.application.menu.MenuProviderFactory;
import br.com.yat.ecosystemcore.application.menu.MenuUsuarioContext;
import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import br.com.yat.ecosystemcore.service.external.EmpresaService;
import br.com.yat.ecosystemcore.service.external.UsuarioService;
import br.com.yat.ecosystemcore.ui.core.ContextAware;
import br.com.yat.ecosystemcore.ui.core.NavigationManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    @FXML private MenuBar menuBar;
    @FXML private StackPane conteudoCentral;
    
    // 🏷️ Componentes do Painel Lateral e Barra de Status vinculados ao FXML
    @FXML private Label lblUsuarioLogado;
    @FXML private Label lblHora;
    @FXML private Label lblTempoAcesso;
    @FXML private Label lblStatusBanco;
    @FXML private ImageView imgUsuario;
    @FXML private ImageView imgData;
    @FXML private ImageView imgLogo;
    @FXML private ImageView imgStatusBanco;
    @FXML private ComboBox<EmpresaUsuarioDetalheDTO> cbSeletorEmpresa;

    private NavigationManager navigationManager;
    private final MenuProvider menuProvider = MenuProviderFactory.create();
    private final EmpresaService empresaService = new EmpresaService();
    
    private long segundosLogado = 0;
    private Timeline relogioCorporativo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.navigationManager = new NavigationManager(conteudoCentral);
        
        // 1. Carrega os menus do banco
        montarMenu();
        
        // 2. Captura os dados reais da Sessão Estável e renderiza no painel
        exibirDadosSessao();
        
        // 3. Inicia o Timer em background para a data/hora e tempo de sessão
        iniciarRelogioSistema();
        configurarSeletorEmpresa();
    }

    private void configurarSeletorEmpresa() {
        Long usuarioId = SessionManager.getUsuarioLogado().getId();
        String tenantId = SessionManager.getTenantAtual().getId();

        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                // Reutilizando seu método do repositório
                var vinculos = new UsuarioService().listarVinculosEmpresa(usuarioId, tenantId);
                
                Platform.runLater(() -> {
                    cbSeletorEmpresa.getItems().addAll(vinculos);
                    // Seleciona a que já está na sessão
                    cbSeletorEmpresa.getSelectionModel().select(
                        vinculos.stream()
                            .filter(v -> v.getEmpresaId().equals(SessionManager.getEmpresaFilial().getId()))
                            .findFirst().orElse(null)
                    );
                    
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

     // No topo da classe MenuController
       

        // No seu método configurarSeletorEmpresa() (que discutimos anteriormente):
        cbSeletorEmpresa.setOnAction(e -> {
            EmpresaUsuarioDetalheDTO selecao = cbSeletorEmpresa.getValue();
            if (selecao != null) {
                AppExecutors.getDatabaseExecutor().execute(() -> {
                    try {
                        Empresa empresaCompleta = empresaService.buscarPorId(selecao.getEmpresaId());
                        SessionManager.setEmpresaFilial(empresaCompleta);
                        
                        Platform.runLater(() -> {
                            // Notifica a tela ativa
                            Object ctrl = navigationManager.getControllerAtual();
                            if (ctrl instanceof ContextAware) {
                                ((ContextAware) ctrl).onContextChanged();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
    }

    private void montarMenu() {
        MenuUsuarioContext context = MenuUsuarioContext.fromSession();
        Map<String, Menu> menus = new LinkedHashMap<>();

        for (MenuPermitidoDTO dto : menuProvider.carregarMenus(context)) {
            MenuChave chave = dto.resolverChaveNavegacao().orElse(null);
            if (chave == null) {
                continue;
            }

            Menu menuPai = menus.computeIfAbsent(dto.moduloNome(), Menu::new);

            MenuItem item = new MenuItem(dto.menuNome());
            item.setUserData(chave);
            item.setOnAction(this::onNavigate);

            menuPai.getItems().add(item);
        }

        menuBar.getMenus().setAll(menus.values());
    }

    private void exibirDadosSessao() {
        // 🔒 Validação da Sessão Estável Multi-Tenant
        if (SessionManager.getUsuarioLogado() != null) {
            String email = SessionManager.getUsuarioLogado().getEmail();
            lblUsuarioLogado.setText(email);
        } else {
            lblUsuarioLogado.setText("Usuário Anônimo");
        }
        
        // Atualiza a barra inferior para indicar sucesso de conexão ativa com o Pool Hikari
        lblStatusBanco.setText("CONECTADO (HIKARI)");
        lblStatusBanco.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2B8A3E;");
    }

    private void iniciarRelogioSistema() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        relogioCorporativo = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Atualiza Data e Hora atual
            lblHora.setText(LocalDateTime.now().format(formatter));
            
            // Calcula e formata Tempo de Acesso
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
        Platform.exit();
    }
    
    private void mostrarAlertaErro(String titulo, String mensagem) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.showAndWait();
        });
    }
}