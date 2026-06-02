package br.com.yat.ecosystemcore.bootstrap;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class App extends Application {

	@Override
    public void start(Stage primaryStage) {
        try {
            // Carrega o FXML da moldura principal localizado na pasta resources
            Parent root = FXMLLoader.load(getClass().getResource("/ui/menu/menu-view.fxml"));
            Scene scene = new Scene(root);
            
            primaryStage.setTitle("YAT Ecosystem Core");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}