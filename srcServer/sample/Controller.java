package sample;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.jar.Manifest;

public class Controller implements Initializable {

    @FXML
    private TreeView<String> treeView;

    @FXML
    private VBox pane;

    @FXML
    private Label label;

    private TreeItem<String> treeRoot;


    public VBox getPane() {
        return pane;
    }

    public TreeItem<String> getTreeRoot() {
        return treeRoot;
    }

    /**
     * Tworzy liste klinetow zalogowanych na serwerze
     *
     * @param resourceBundle resourceBundle
     * @param url            url
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        treeRoot = new TreeItem<String>();
        this.treeView.setRoot(treeRoot);

    }
}
