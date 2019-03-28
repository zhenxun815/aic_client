package com.tqhy.client.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static com.tqhy.client.ClientApplication.springContext;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
public class FXMLUtils {


    /**
     *
     * 打开新窗口
     *
     * @param stage
     * @param url
     * @return
     */
    public static Stage loadWindow(Stage stage, String url) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(FXMLUtils.class.getResource(url));
            fxmlLoader.setControllerFactory(springContext::getBean);
            Parent parentNode = fxmlLoader.load();
            Scene scene = new Scene(parentNode);
            stage.setScene(scene);
            stage.show();
            return stage;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
