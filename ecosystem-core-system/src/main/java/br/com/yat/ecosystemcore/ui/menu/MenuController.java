package br.com.yat.ecosystemcore.ui.menu;

import br.com.yat.ecosystemcore.application.menu.MenuProvider;
import br.com.yat.ecosystemcore.application.menu.MenuProviderFactory;
import br.com.yat.ecosystemcore.application.menu.MenuUsuarioContext;
import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;
import br.com.yat.ecosystemcore.ui.core.NavigationManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    @FXML private MenuBar menuBar;
    @FXML private StackPane conteudoCentral;

    private NavigationManager navigationManager;
    private final MenuProvider menuProvider = MenuProviderFactory.create();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.navigationManager = new NavigationManager(conteudoCentral);
        montarMenu();
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

    @FXML
    private void onNavigate(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();

        if (item.getUserData() instanceof MenuChave chave) {
            navigationManager.navigatePara(chave);
        }
    }

    @FXML
    public void onLogout() {
        navigationManager.forcarLimpezaCache();
        Platform.exit();
    }
}
