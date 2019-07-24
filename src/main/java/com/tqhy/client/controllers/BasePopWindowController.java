package com.tqhy.client.controllers;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Yiheng
 * @create 7/24/2019
 * @since 1.0.0
 */
@Getter
@Setter
public class BasePopWindowController {

    Stage getOwnerStageFromEvent(Event event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        return stage;
    }
}
