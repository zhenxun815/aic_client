package com.tqhy.client.utils;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.controllers.PreloaderController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.*;

import java.io.File;
import java.io.IOException;

import static com.tqhy.client.ClientApplication.springContext;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
public class FXMLUtils {

    /**
     * 打开新窗口
     *
     * @param url
     * @return
     */
    public static Stage loadPopWindow(String url) {
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        return loadWindow(stage, url);
    }

    /**
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
            loadScene(stage, parentNode);
            stage.show();
            return stage;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 打开新窗口
     *
     * @param stage
     * @param url
     * @return
     */
    public static PreloaderController loadPreloader(Stage stage, String url) {
        try {
            FXMLLoader loader = new FXMLLoader(FXMLUtils.class.getResource(url));
            Parent parentNode = loader.load();
            loadScene(stage, parentNode);
            stage.show();
            PreloaderController preloaderController = loader.getController();
            return preloaderController;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 向{@link Stage Stage}加载{@link Scene Scene}
     *
     * @param stage
     * @param parentNode
     */
    private static void loadScene(Stage stage, Parent parentNode) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double visualWidth = visualBounds.getWidth();
        double visualHeight = visualBounds.getHeight();
        Scene scene = new Scene(parentNode, visualWidth, visualHeight, Color.TRANSPARENT);
        scene.getStylesheets().add(NetworkUtils.toExternalForm("/static/css/fx_root.css"));
        stage.setScene(scene);
        stage.getIcons().add(new Image(NetworkUtils.toExternalForm("/static/img/logo_title_light.png")));
    }

    /**
     * 是否展示子元素节点
     *
     * @param parent
     * @param child
     * @param display 为true则添加,false则移除
     */
    public static void displayChildNode(Pane parent, Node child, boolean display) {
        if (display) {
            if (!parent.getChildren().contains(child)) {
                parent.getChildren().add(child);
            }
        } else {
            if (parent.getChildren().contains(child)) {
                parent.getChildren().remove(child);
            }
        }
    }

    /**
     * 弹出选择文件夹窗口并返回选择路径
     *
     * @param window 若为null则使用主窗口{@link ClientApplication#stage}对象
     * @return
     */
    public static File chooseDir(Window window) {
        DirectoryChooser downloadDirChooser = new DirectoryChooser();
        return downloadDirChooser.showDialog(null == window ? ClientApplication.stage : window);
    }
}
