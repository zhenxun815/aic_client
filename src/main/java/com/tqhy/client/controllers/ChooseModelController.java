package com.tqhy.client.controllers;

import com.tqhy.client.utils.FXMLUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Yiheng
 * @create 8/22/2019
 * @since 1.0.0
 */
@Controller
public class ChooseModelController extends BasePopWindowController {


    Logger logger = LoggerFactory.getLogger(ChooseModelController.class);

    @FXML
    public VBox base_pane;
    @FXML
    public ListView model_list;
    @FXML
    public TextField text_field_case_id;
    @FXML
    public Label label_tips;
    @Autowired
    ReadModelController readModelController;
    private List<String> allModels = new ArrayList<>();
    private List<String> chosenModels = new ArrayList();

    @FXML
    public void initialize() {
        logger.info("choose model initialize...");
        allModels.clear();
        chosenModels.clear();
        for (int i = 0; i < 100; i++) {
            allModels.add("model name ".concat(Integer.toString(i)));
        }
        FXMLUtils.center2Display(base_pane);
        model_list.getItems().addAll(allModels);
        model_list.setCellFactory(CheckBoxListCell.forListView((Callback<String, ObservableValue<Boolean>>) item -> {
            BooleanProperty observable = new SimpleBooleanProperty();
            observable.addListener((obs, wasSelected, isNowSelected) -> {
                logger.info("check status old: {}, new: {}", wasSelected, isNowSelected);
                if (isNowSelected) {
                    logger.info("add choose model {}", item);
                    chosenModels.add(item);
                } else {
                    logger.info("remove choose model {}", item);
                    chosenModels.remove(item);
                }
            });
            return observable;
        }));
    }

    public void confirm(MouseEvent mouseEvent) {
        logger.info("into confirm");
        for (String choosedModel : chosenModels) {
            logger.info("choosed model is {}", choosedModel);
        }
        //
        String caseId = text_field_case_id.getText();
        if (StringUtils.isEmpty(caseId)) {
            logger.info("case Id must not be empty!");
            label_tips.setText("case id must not be empty!");
        } else {
            label_tips.setText("");
            if (chosenModels.size() > 0) {
                readModelController.show(caseId, chosenModels);
                cancel(mouseEvent);
            } else {
                logger.info("must check at least one model!");
                label_tips.setText("must check at least one model!");
            }
        }
    }

    public void cancel(MouseEvent mouseEvent) {
        Stage stage = getOwnerStageFromEvent(mouseEvent);
        stage.close();
    }
}
