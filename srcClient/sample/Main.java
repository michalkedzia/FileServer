package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

//    C:\Users\Michał\Desktop\test2
//    C:\Users\Michał\Desktop\TestD

    public static void main(String[] args) {

        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();

    }
}


//    ExecutorService executor = Executors.newFixedThreadPool(10);
//executor.shutdown();
//        try{
//        if(!executor.awaitTermination(800,TimeUnit.MILLISECONDS))
//        {
//        executor.shutdownNow();
//        }
//        }catch(InterruptedException e){
//        executor.shutdownNow();
//        }