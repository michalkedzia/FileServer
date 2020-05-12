package sample;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private TreeView<String> treeView;

    @FXML
    private Pane pane;

    @FXML
    private Label label;

    private TreeItem<String> treeRoot;

    public Controller() {

    }

    void printChildren(TreeItem<String> root) {
        System.out.println("Current Parent :" + root.getValue());
        int i = 0;
        for (TreeItem<String> child : root.getChildren()) {
            if (child.getChildren().isEmpty()) {
                System.out.println(child.getValue() + i);
            } else {
//                printChildren(child);
            }
            i++;
        }
    }


    public TreeItem<String> getTreeRoot() {
        return treeRoot;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        treeRoot = new TreeItem<String>();
        this.treeView.setRoot(treeRoot);


//        TreeItem<String> s1= new TreeItem<>("q");
//        TreeItem<String> ss1= new TreeItem<String>("qq");
//        TreeItem<String> ss2= new TreeItem<String>("qqq");
//        TreeItem<String> ss3= new TreeItem<String>("qqqq");
//        TreeItem<String> ss4= new TreeItem<String>("qqqqq");
//        s1.getChildren().addAll(ss1 ,ss2,ss3,ss4);
//
//        printChildren(s1);
//        s1.getChildren().remove(2);
//        TreeItem<String> s2= new TreeItem<>("w");
//        TreeItem<String> s3= new TreeItem<>("e");
//        treeRoot.getChildren().addAll(s1,s2,s3);


    }
}
