package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

/**
 * @author Yiheng
 * @create 7/24/2019
 * @since 1.0.0
 */
@Controller
public class WarningOnCloseController extends BasePopWindowController {

    Logger logger = LoggerFactory.getLogger(WarningOnCloseController.class);

    @FXML
    public VBox base_pane;

    @FXML
    public void initialize() {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double visualWidth = visualBounds.getWidth();
        double visualHeight = visualBounds.getHeight();

        base_pane.setLayoutX((visualWidth - base_pane.getMinWidth()) / 2);
        base_pane.setLayoutY((visualHeight - base_pane.getMinHeight()) / 2);
        logger.info("pane width {}, pane height {}", base_pane.getMinWidth(), base_pane.getMinHeight());
        logger.info("x {}, y {}", base_pane.getLayoutX(), base_pane.getLayoutY());
    }

    @FXML
    public void closeConfirm(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info("close confirm ...");
            Stage stage = getOwnerStageFromEvent(mouseEvent);
            stage.close();
            ClientApplication.stage.close();
            System.exit(0);
        }
    }

    @FXML
    public void closeCancel(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info("close cancel ...");
            Stage stage = getOwnerStageFromEvent(mouseEvent);
            stage.close();
        }
    }

    @FXML
    public void stageMinimum(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info("stage minimum ...");
            Stage stage = getOwnerStageFromEvent(mouseEvent);
            stage.close();
            ClientApplication.stage.setIconified(true);
        }
    }
}
