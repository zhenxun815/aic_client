package com.tqhy.client;

import com.tqhy.client.utils.FXMLUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    static Logger logger = LoggerFactory.getLogger(ClientApplication.class);
    public static ConfigurableApplicationContext springContext;
    public static Stage stage;


    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        initPrimaryStageSize();
        stage.setOnCloseRequest(event -> System.exit(0));

        FXMLUtils.loadWindow(stage, "/static/fxml/main.fxml");
    }

    /**
     * 初始最大化窗口,固定窗体大小
     */
    private void initPrimaryStageSize() {

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double minX = visualBounds.getMinX();
        double minY = visualBounds.getMinY();
        //double maxX = visualBounds.getMaxX();
        //double maxY = visualBounds.getMaxY();
        double width = visualBounds.getWidth();
        double height = visualBounds.getHeight();

        //logger.info("minX: " + minX + ", minY: " + minY);
        //logger.info("maxX: " + maxX + ", maxY: " + maxY);
        //logger.info("width: " + width + ", height: " + height);
        //stage.setX(minX);
        //stage.setY(minY);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    @Override
    public void init() throws Exception {
        super.init();
        Platform.setImplicitExit(false);
        springContext = SpringApplication.run(ClientApplication.class);
    }


    @Override
    public void stop() {
        springContext.stop();
        try {
            super.stop();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }


}
