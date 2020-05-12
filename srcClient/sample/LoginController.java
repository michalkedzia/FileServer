package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LoginController {


    @FXML
    private Pane pane;

    @FXML
    private Button button;

    @FXML
    private TextField userName;

    @FXML
    private TextField path;


    public Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @FXML
    void getNameAndPath() {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("sample.fxml"));
        Pane paneApp = null;
        try {
            paneApp = loader.load();
        } catch (IOException e) {
            e.printStackTrace();

        }

        ConnectionClass connectionClass = new ConnectionClass();

        Controller mainController = loader.getController();
        mainController.setUserNameAndPath(userName.getText(), path.getText());

        mainController.setConnectionClass(connectionClass);
        mainController.getConnectionClass().init(userName.getText());

        mainController.initUserFileAndServerFile(); // test


        mainController.initObservableListFiles();

        pane.getChildren().clear();
        pane.getChildren().add(paneApp);


        pane.getScene().getWindow().setOnCloseRequest(e -> {
            mainController.onShutdownApp();
        });


    }
}
