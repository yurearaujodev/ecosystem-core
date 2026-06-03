package br.com.yat.ecosystemcore.bootstrap;

import br.com.yat.ecosystemcore.infrastructure.database.ConnectionFactory;
import br.com.yat.ecosystemcore.infrastructure.database.DatabaseMenuSeeder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;

public class App extends Application {

	@Override
	public void init() {
	    try (Connection conn = ConnectionFactory.getConnection()) {
	        DatabaseMenuSeeder.inicializarCargaEstrutural(conn);
	    } catch (Exception e) {
	        System.err.println("Erro init DB");
	        Platform.exit();
	    }
	}

	@Override
	public void start(Stage stage) throws Exception {

	    FXMLLoader loader = new FXMLLoader(
	        getClass().getResource("/ui/menu/menu-view.fxml")
	    );

	    Parent root = loader.load();

	    Scene scene = new Scene(root);

	    stage.setScene(scene);
	    stage.setTitle("YAT Ecosystem Core");
	    stage.setMaximized(true);
	    stage.show();
	}

    @Override
    public void stop() {
        ConnectionFactory.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}