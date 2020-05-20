package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Glowna klasa programu. Startuje cala aplikacje JavaFx.
 */
public class Main extends Application {

    /**
     * Glowna metoda aplikacji
     *
     * @param args argumenty wejsciowe programu
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Wczytuje panel logowania dla klienta.
     *
     * @param primaryStage scena
     * @throws java.io.IOException
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        primaryStage.setTitle("Client");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();
    }
}
