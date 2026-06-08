package br.com.yat.ecosystemcore.app;

import br.com.yat.ecosystemcore.shared.current.AppExecutors;
import br.com.yat.ecosystemcore.shared.database.ConnectionFactory;
import br.com.yat.ecosystemcore.ui.modules.login.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AppLauncher extends Application {

	@Override
    public void init() {
        // Na primeira execução com o banco limpo, nós NÃO chamamos o seeder aqui no init.
        // O seeder agora roda de dentro da tela de Setup, junto com a criação do usuário, numa transação única.
        System.out.println("Iniciando YAT Ecosystem Core... Verificando integridade do ecossistema.");
    }
    
    @Override
    public void start(Stage stage) throws Exception {

        // 🧠 BOOTSTRAP DO SISTEMA (AQUI)
        ApplicationContext.init();

        boolean ecossistemaPronto = verificarEcossistemaInicializado();

        FXMLLoader loader;

        if (ecossistemaPronto) {
            loader = new FXMLLoader(getClass().getResource("/ui/modules/login-view.fxml"));
            stage.setTitle("YAT Ecosystem - Autenticação Requerida");
        } else {
            loader = new FXMLLoader(getClass().getResource("/ui/modules/setup-global-ecosystem-view.fxml"));
            stage.setTitle("YAT Master Systems - Configuração Global do Ecossistema");
        }

        Parent root = loader.load();

        if (ecossistemaPronto) {
            LoginController loginController = loader.getController();
         // Caso seu NavigationManager necessite do stage atual para trocar de telas:
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
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

    @Override
    public void stop() {
        System.out.println("Encerrando recursos e pools de threads...");
        
        // 1. Desliga os pools assíncronos primeiro para evitar submissões tardias
        AppExecutors.shutdown();
        
        // 2. Mata o pool de conexões com o banco de dados de forma segura
        ConnectionFactory.shutdown();
        
        System.out.println("Ecosystem Core finalizado com sucesso.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}