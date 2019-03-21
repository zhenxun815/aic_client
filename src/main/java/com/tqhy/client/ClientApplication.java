package com.tqhy.client;

import com.tqhy.client.utils.NetworkUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Yiheng
 * @create 1/29/2019
 * @since 1.0.0
 */
@SpringBootApplication
public class ClientApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private Parent rootNode;
    private FXMLLoader fxmlLoader;


    @Override
    public void start(Stage primaryStage) throws Exception {
        rootNode = fxmlLoader.load();
        primaryStage.setMinWidth(800D);
        primaryStage.setMinHeight(600D);
        primaryStage.getIcons().add(new Image("/static/img/logo_title.png"));
        //primaryStage.getIcons().add(new Image(NetworkUtils.toExternalForm("/static/img/logo_title.png")));
        primaryStage.setScene(new Scene(rootNode));
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
    }

    @Override
    public void init() throws Exception {
        super.init();
        Platform.setImplicitExit(false);
        springContext = SpringApplication.run(ClientApplication.class);
        fxmlLoader = new FXMLLoader(getClass().getResource("/static/fxml/main.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
    }

    @Override
    public void stop() {
        springContext.stop();
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }


}
