package br.com.yat.ecosystemcore.ui.menu;

import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.ui.core.NavigationManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import java.net.URL;
import java.util.*;

/**
 * Controlador do Menu Principal e da moldura do sistema (BorderPane).
 */
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

    private NavigationManager navigationManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicializa o gerenciador passando o container onde as telas internas vão renderizar
        this.navigationManager = new NavigationManager(conteudoCentral);
        montarMenuOrientadoABanco();
    }

    private void montarMenuOrientadoABanco() {
        List<MenuDinamicoDTO> menusDoBanco = carregarMockMenusBanco();
        Map<String, Menu> categoriasVisuais = new LinkedHashMap<>();

        for (MenuDinamicoDTO dto : menusDoBanco) {
            try {
                MenuChave chave = MenuChave.valueOf(dto.getChaveEnumString());
                Menu menuPai = categoriasVisuais.computeIfAbsent(dto.getCategoria(), Menu::new);

                MenuItem itemSubmenu = new MenuItem(dto.getSubmenuNome());
                itemSubmenu.setUserData(chave); 
                itemSubmenu.setOnAction(this::onNavegar);

                menuPai.getItems().add(itemSubmenu);
            } catch (IllegalArgumentException e) {
                System.err.println("[DB-MENU-ERROR] Chave dinâmica inválida na Enum: " + dto.getChaveEnumString());
            }
        }

        menuBar.getMenus().setAll(categoriasVisuais.values());
    }

    @FXML
    private void onNavegar(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        if (item.getUserData() instanceof MenuChave) {
            navigationManager.navigatePara((MenuChave) item.getUserData());
        }
    }

    @FXML
    public void onLogout() {
        System.out.println("[SaaS] Encerrando o ecossistema com segurança e limpando cache.");
        navigationManager.forcarLimpezaCache(); 
        Platform.exit();
    }

    private List<MenuDinamicoDTO> carregarMockMenusBanco() {
        List<MenuDinamicoDTO> lista = new ArrayList<>();
        
        // MÓDULO: CADASTROS
        lista.add(new MenuDinamicoDTO("CADASTROS", "Empresas Clientes", "CADASTROS_EMPRESA"));
        lista.add(new MenuDinamicoDTO("CADASTROS", "Clientes e Fornecedores", "CADASTROS_PESSOA"));
        lista.add(new MenuDinamicoDTO("CADASTROS", "Usuários do Sistema", "CADASTROS_USUARIO"));
        
        // MÓDULO: SEGURANÇA
        lista.add(new MenuDinamicoDTO("SEGURANÇA", "Perfis de Acesso (RBAC)", "SEGURANÇA_PERFIL"));
        lista.add(new MenuDinamicoDTO("SEGURANÇA", "Permissões Ativas", "SEGURANÇA_PERMISSAO"));
        
        // MÓDULO: ADMINISTRAÇÃO
        lista.add(new MenuDinamicoDTO("ADMINISTRAÇÃO", "Configuração da Conta", "ADMIN_TENANT_CONFIG"));
        lista.add(new MenuDinamicoDTO("ADMINISTRAÇÃO", "Parâmetros Operacionais", "ADMIN_PARAMETROS"));
        lista.add(new MenuDinamicoDTO("ADMINISTRAÇÃO", "Configuração do Banco", "CONFIGURACAO_BANCO"));
        
        // MÓDULO: AUDITORIA
        lista.add(new MenuDinamicoDTO("AUDITORIA", "Rastreador de Logs", "AUDITORIA_LOGS"));
        lista.add(new MenuDinamicoDTO("AUDITORIA", "Agendador de Tarefas (Jobs)", "AUDITORIA_JOBS"));
        
        return lista;
    }
    
    /**
     * Atualiza o indicador visual do banco de dados no rodapé do sistema (Thread-Safe).
     */
    public void atualizarStatusBanco(boolean online) {
        Platform.runLater(() -> {
            if (online) {
                lblStatusBanco.setText("ONLINE");
                lblStatusBanco.setStyle("-fx-text-fill: #2B8A3E; -fx-font-weight: bold;"); // Verde corporativo
                // Se possuir uma fábrica de ícones ou imagens:
                // imgStatusBanco.setImage(new Image(getClass().getResourceAsStream("/ui/icons/banco-ok.png")));
            } else {
                lblStatusBanco.setText("OFFLINE");
                lblStatusBanco.setStyle("-fx-text-fill: #C92A2A; -fx-font-weight: bold;"); // Vermelho corporativo
                // imgStatusBanco.setImage(new Image(getClass().getResourceAsStream("/ui/icons/banco-erro.png")));
            }
        });
    }

    /**
     * Define o texto dinâmico da label de tempo de acesso.
     */
    public void setTempoAcesso(String tempo) {
        Platform.runLater(() -> lblTempoAcesso.setText(tempo));
    }

    /**
     * Define o texto dinâmico da label de hora corrente.
     */
    public void setHora(String horaTexto) {
        Platform.runLater(() -> lblHora.setText(horaTexto));
    }

    /**
     * Altera o nome do usuário ativo exibido no painel lateral.
     */
    public void setNomeUsuario(String nome) {
        Platform.runLater(() -> lblUsuarioLogado.setText(nome));
    }
}
