package com.tqhy.client;

import com.tqhy.client.unique.AlreadyLockedException;
import com.tqhy.client.unique.JUnique;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.SystemUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;

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
        stage.setMinWidth(1080D);
        stage.setMinHeight(600D);
        stage.setOnCloseRequest(event -> System.exit(0));

        FXMLUtils.loadWindow(stage, "/static/fxml/main.fxml");
    }

    @Override
    public void init() throws Exception {
        super.init();
        Platform.setImplicitExit(false);
        springContext = SpringApplication.run(ClientApplication.class);

        initLibPath();
    }

    private void initLibPath() {
        String arc = SystemUtils.getArc();
        logger.info("system arc is: " + arc);

        String dllToCopy = SystemUtils.SYS_ARC_64.equals(arc) ? "/bin/opencv_java_64bit.dll" : "/bin/opencv_java_32bit.dll";
        File destDll = FileUtils.getLocalFile("/", "opencv_java.dll");

        boolean copyResource = FileUtils.copyResource(dllToCopy, destDll.getAbsolutePath());
        if (!copyResource) {
            stop();
        }
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
        String appId = "TQHY-AIC-CLIENT";
        boolean alreadyRunning;
        try {
            JUnique.acquireLock(appId, message -> {
                System.out.println("get message: " + message);
                return null;
            });
            alreadyRunning = false;
        } catch (AlreadyLockedException e) {
            alreadyRunning = true;
        }

        if (alreadyRunning) {
            for (int i = 0; i < args.length; i++) {
                JUnique.sendMessage(appId, "call_window");
            }
        } else {

            launch(args);
        }
    }


}
