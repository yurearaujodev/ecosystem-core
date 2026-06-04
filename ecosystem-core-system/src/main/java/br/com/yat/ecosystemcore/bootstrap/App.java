package br.com.yat.ecosystemcore.bootstrap;

import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory;
import br.com.yat.ecosystemcore.ui.modules.login.LoginController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class App extends Application {

	@Override
    public void init() {
        // Na primeira execução com o banco limpo, nós NÃO chamamos o seeder aqui no init.
        // O seeder agora roda de dentro da tela de Setup, junto com a criação do usuário, numa transação única.
        System.out.println("Iniciando YAT Ecosystem Core... Verificando integridade do ecossistema.");
    }
    
//    @Override
//    public void start(Stage stage) throws Exception {
//        boolean ecossistemaPronto = verificarEcossistemaInicializado();
//
//        FXMLLoader loader;
//        if (ecossistemaPronto) {
//            // Se o ecossistema já está configurado e o dono já fez o setup, abre a tela principal
//            loader = new FXMLLoader(getClass().getResource("/ui/menu/menu-view.fxml"));
//            stage.setTitle("YAT Ecosystem Core");
//        } else {
//            // Se o banco está limpo, abre o instalador mestre para criar o ecossistema e o usuário admin
//            loader = new FXMLLoader(getClass().getResource("/ui/modules/setup-global-ecosystem-view.fxml"));
//            stage.setTitle("YAT Master Systems - Configuração Global do Ecossistema 🚀");
//        }
//
//        Parent root = loader.load();
//        Scene scene = new Scene(root);
//        stage.setScene(scene);
//        stage.setMaximized(true);
//        stage.show();
//    }
    
    @Override
    public void start(Stage stage) throws Exception {
        boolean ecossistemaPronto = verificarEcossistemaInicializado();

        FXMLLoader loader;
        if (ecossistemaPronto) {
            // 🔒 SEGUNDA ETAPA: Se o ecossistema existe, exige autenticação na tela de Login
            loader = new FXMLLoader(getClass().getResource("/ui/modules/login-view.fxml")); // Ajuste o path se necessário
            stage.setTitle("YAT Ecosystem - Autenticação Requerida");
        } else {
            // 🚀 PRIMEIRA ETAPA: Se o banco está zerado, força o Setup Wizard
            loader = new FXMLLoader(getClass().getResource("/ui/modules/setup-global-ecosystem-view.fxml"));
            stage.setTitle("YAT Master Systems - Configuração Global do Ecossistema");
        }

        Parent root = loader.load();
        
        // Se caiu na tela de login, injete um NavigationManager básico ou mockado se necessário
        if (ecossistemaPronto) {
            LoginController loginController = loader.getController();
            // Caso seu NavigationManager necessite do stage atual para trocar de telas:
            // loginController.setNavigationManager(new NavigationManager(stage));
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false); // Mantém a proporção elegante da caixinha de login
        stage.centerOnScreen();
        stage.show();
    }
    
    private boolean verificarEcossistemaInicializado() {
        String sql = "SELECT COUNT(*) FROM sistema_config WHERE chave = 'CONFIG_GLOBAL_SISTEMA'";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 0; 
            }
        } catch (Exception e) {
            // Se a tabela sistema_config não existir ainda (banco totalmente limpo), 
            // ele vai cair aqui e retornar false, o que está correto para abrir a tela de setup.
            System.out.println("Aviso: Tabela sistema_config não encontrada ou vazia. Redirecionando para o Setup Wizard.");
        }
        return false;
    }
    
//versao quando o sistema for pro cliente
//    @Override
//    public void start(Stage stage) throws Exception {
//        boolean ecossistemaInicializado = verificarSeExisteTenant();
//
//        FXMLLoader loader;
//        if (ecossistemaInicializado) {
//            // Fluxo Normal: O sistema já possui dono e tabelas configuradas. Vai para o Menu/Login.
//            loader = new FXMLLoader(getClass().getResource("/ui/menu/menu-view.fxml"));
//            stage.setTitle("YAT Ecosystem Core");
//        } else {
//            // Fluxo de Instalação: Banco limpo detectado. Abre a Super Tela de Onboarding.
//            loader = new FXMLLoader(getClass().getResource("/ui/modules/tenant/OnboardingTenantView.fxml"));
//            stage.setTitle("YAT Ecosystem Master Setup Wizard 🚀");
//        }
//
//        Parent root = loader.load();
//        Scene scene = new Scene(root);
//        
//        stage.setScene(scene);
//        stage.setMaximized(true);
//        stage.show();
//    }
//    private boolean verificarSeExisteTenant() {
//        String sql = "SELECT COUNT(*) FROM tenant";
//        try (Connection conn = ConnectionFactory.getConnection();
//             PreparedStatement stmt = conn.prepareStatement(sql);
//             ResultSet rs = stmt.executeQuery()) {
//            if (rs.next()) {
//                return rs.getInt(1) > 0; // Se houver mais de 0, o sistema já foi inicializado antes
//            }
//        } catch (Exception e) {
//            System.err.println("Aviso: Não foi possível ler a tabela tenant. Assumindo banco limpo.");
//        }
//        return false;
//    }

    @Override
    public void stop() {
        ConnectionFactory.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}