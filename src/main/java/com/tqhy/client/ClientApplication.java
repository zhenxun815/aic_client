package com.tqhy.client;

import com.tqhy.client.utils.FXMLUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

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
        stage.setOnCloseRequest(event -> System.exit(0));
        FXMLUtils.loadWindow(ClientApplication.stage, "/static/fxml/main.fxml");
        initPrimaryStageSize();

    }

    /**
     * 初始最大化窗口,固定窗体大小
     */
    private void initPrimaryStageSize() {
        stage.setMaximized(true);
        stage.centerOnScreen();
        double height = stage.getHeight();
        double width = stage.getWidth();
        stage.setMaxHeight(height);
        stage.setMaxWidth(width);
        stage.setResizable(false);
    }

    /**
     * 创建系统托盘图标
     */
    private void initSystemTray() {
        try {
            System.setProperty("java.awt.headless", "false");
            Toolkit.getDefaultToolkit();
            if (!java.awt.SystemTray.isSupported()) {
                logger.info("系统不支持托盘图标,程序退出..");
                Platform.exit();
            }
            //PopupMenu popupMenu = createPopMenu(stage);

            SystemTray systemTray = SystemTray.getSystemTray();
            String iconPath = ClientApplication.class.getResource("/static/img/logo_systray.png").toExternalForm();
            URL imageLoc = new URL(iconPath);
            java.awt.Image image = ImageIO.read(imageLoc);
            //final TrayIcon trayIcon = new TrayIcon(image, "打开悬浮窗",popupMenu);
            final TrayIcon trayIcon = new TrayIcon(image);

            systemTray.add(trayIcon);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws Exception {
        super.init();
        Platform.setImplicitExit(false);
        springContext = SpringApplication.run(ClientApplication.class);
        initSystemTray();
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
