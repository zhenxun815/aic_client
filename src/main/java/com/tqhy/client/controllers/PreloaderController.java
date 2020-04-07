package com.tqhy.client.controllers;

import com.tqhy.client.config.Constants;
import com.tqhy.client.utils.PropertyUtils;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yiheng
 * @create 4/15/2019
 * @since 1.0.0
 */

public class PreloaderController {


    Logger logger = LoggerFactory.getLogger(PreloaderController.class);

    private SimpleDoubleProperty preloadProgress = new SimpleDoubleProperty(0.0D);

    private SimpleStringProperty preloadMessage = new SimpleStringProperty();

    /**
     * 初始化进度
     */
    @FXML
    ProgressBar progress_bar_preloader;
    @FXML
    Text text_preloader_desc;
    @FXML
    Text text_preloader_title;

    @FXML
    public void initialize() {

        String language = PropertyUtils.getLanguage();
        if (Constants.LANGUAGE_EN.equals(language)) {
            text_preloader_title.setText("AIC Client Starting...");
            text_preloader_desc.setText("Please wait a few seconds...");
        } else {
            text_preloader_title.setText("AIC客户端程序启动...");
            text_preloader_desc.setText("客户端正在启动，请耐心等待...");
        }

        progress_bar_preloader.progressProperty()
                              .bind(this.preloadProgress);

        text_preloader_desc.textProperty()
                           .bind(this.preloadMessage);
    }

    public double getPreloadProgress() {
        return preloadProgress.get();
    }

    public SimpleDoubleProperty preloadProgressProperty() {
        return preloadProgress;
    }

    public void setPreloadProgress(double preloadProgress) {
        this.preloadProgress.set(preloadProgress);
    }

    public String getPreloadMessage() {
        return preloadMessage.get();
    }

    public SimpleStringProperty preloadMessageProperty() {
        return preloadMessage;
    }

    public void setPreloadMessage(String preloadMessage) {
        this.preloadMessage.set(preloadMessage);
    }
}
