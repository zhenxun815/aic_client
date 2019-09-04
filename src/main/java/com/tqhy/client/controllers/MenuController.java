package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.config.Constants;
import com.tqhy.client.network.Network;
import com.tqhy.client.service.HeartBeatService;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.PropertyUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author Yiheng
 * @create 8/26/2019
 * @since 1.0.0
 */
@Controller
public class MenuController extends BasePopWindowController {

    Logger logger = LoggerFactory.getLogger(MenuController.class);

    @Autowired
    HeartBeatService heartBeatService;
    @FXML
    public VBox menu_container;

    @FXML
    public void initialize() {
        menu_container.setOnMouseExited(event -> getOwnerStageFromEvent(event).hide());
    }

    @FXML
    public void openKit(MouseEvent mouseEvent) {
        logger.info("click icon {}", mouseEvent.getButton());
        Platform.runLater(() -> {
            FXMLUtils.loadChooseModel("/static/fxml/choose_model.fxml");
        });
        getOwnerStageFromEvent(mouseEvent).hide();
    }

    @FXML
    public void openSetting(MouseEvent mouseEvent) {
        PropertyUtils.setProperty(Constants.SERVER_IP, "");
        PropertyUtils.setProperty(Constants.USERNAME, "");
        PropertyUtils.setProperty(Constants.LANDING_IGNORE, "");
        Network.SERVER_IP = "";
        FXMLUtils.loadWindow(ClientApplication.stage, "/static/fxml/main.fxml", true);
    }

    @FXML
    public void openMainStage(MouseEvent mouseEvent) {
        ClientApplication.stage.setIconified(false);
        getOwnerStageFromEvent(mouseEvent).hide();
    }

    @FXML
    public void exit(MouseEvent mouseEvent) {
        getOwnerStageFromEvent(mouseEvent).close();
        ClientApplication.stage.close();
        System.exit(0);
    }
}
