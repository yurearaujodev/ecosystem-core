package br.com.yat.ecosystemcore.ui.menu;

import br.com.yat.ecosystemcore.application.menu.MenuProvider;
import br.com.yat.ecosystemcore.application.menu.MenuProviderFactory;
import br.com.yat.ecosystemcore.application.menu.MenuUsuarioContext;
import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.entity.Empresa;
import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.infrastructure.concurrent.AppExecutors;
import br.com.yat.ecosystemcore.infrastructure.security.Sessao;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    @FXML private MenuBar menuBar;
    @FXML private StackPane conteudoCentral;
    
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
    private final UsuarioService usuarioService = new UsuarioService();
    
    private long segundosLogado = 0;
    private Timeline relogioCorporativo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.navigationManager = new NavigationManager(conteudoCentral);
        
        montarMenu();
        exibirDadosSessao();
        iniciarRelogioSistema();
        configurarSeletorEmpresa();
    }

    private void configurarSeletorEmpresa() {
        // 🔒 Centralizado para usar a classe estável Sessao
        if (!Sessao.isActive() || Sessao.usuario() == null) return;

        // Captura o ID do usuário conectado no contexto atual para passar ao serviço
        Long usuarioLogadoId = Sessao.usuario().getId();

        // Dispara a busca pesada de banco no Pool assíncrono para nunca travar a renderização (UI Thread) do JavaFX
        AppExecutors.getDatabaseExecutor().execute(() -> {
            try {
                // ⚡ CORRIGIDO: Passando o ID exigido pela assinatura do método na UsuarioService
                // Ao especificar o parâmetro, o Java infere corretamente o tipo List<EmpresaUsuarioDetalheDTO>
                List<EmpresaUsuarioDetalheDTO> vinculos = usuarioService.listarVinculosEmpresa(usuarioLogadoId);
                
                Platform.runLater(() -> {
                    cbSeletorEmpresa.getItems().setAll(vinculos);
                    
                    // 🔒 Captura segura através da nova fachada corporativa
                    Empresa empresaSessao = Sessao.empresa();
                    if (empresaSessao != null) {
                        cbSeletorEmpresa.getSelectionModel().select(
                            vinculos.stream()
                                .filter(v -> v.getEmpresaId().equals(empresaSessao.getId()))
                                .findFirst().orElse(null)
                        );
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Evento gatilho acionado quando o usuário clica para alternar de filial corporativa
        cbSeletorEmpresa.setOnAction(e -> {
            EmpresaUsuarioDetalheDTO selecao = cbSeletorEmpresa.getValue();
            if (selecao != null) {
                AppExecutors.getDatabaseExecutor().execute(() -> {
                    try {
                        Empresa empresaCompleta = empresaService.buscarPorId(selecao.getEmpresaId());
                        
                        // 🚀 Atualiza o contexto mutável de filiais corporativas de forma limpa e monitorada
                        br.com.yat.ecosystemcore.infrastructure.security.SessionScope.trocarEmpresa(empresaCompleta);
                        
                        // Alerta de forma reativa a tela/view que está aberta que o contexto mudou
                        Platform.runLater(() -> {
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
        // 🔒 MODIFICADO: Atualizado para o padrão intuitivo Sessao
        if (Sessao.isActive() && Sessao.usuario() != null) {
            String email = Sessao.usuario().getEmail();
            lblUsuarioLogado.setText(email);
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
        
        // 🔒 MODIFICADO: Invoca o encerramento seguro. Isso limpa a memória local
        // e dispara a invalidação no cache Caffeine além de atualizar o banco com 'revogado_em'
        Sessao.logout(); 
        
        Platform.exit();
    }
}