package com.rubberhuman.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App Entry Point
 */
public class ClientApp extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // 初始加载登录界面
        scene = new Scene(loadFXML("view/login"), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Network File Manager Client");
        stage.show();
    }

    // 静态方法：切换界面
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        // 根据不同界面调整窗口大小（可选）
        if (fxml.contains("main")) {
            scene.getWindow().setWidth(1000);
            scene.getWindow().setHeight(700);
            scene.getWindow().centerOnScreen();
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}