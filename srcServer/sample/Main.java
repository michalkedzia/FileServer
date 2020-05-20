package sample;//package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Glowna klasa serwera
 */
public class Main extends Application {

    /**
     * Startuje aplikacje klienta
     *
     * @param args parematry wejsciowe
     */
    public static void main(String[] args) {

        launch(args);
    }

    /**
     * Uruchamia aplikacje serwera i GUI
     *
     * @param primaryStage glowna scena wyswietlana przez serwer
     * @throws java.io.IOException
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        ServerClass s = new ServerClass();
        s.startServer(loader.getController());
    }
}





