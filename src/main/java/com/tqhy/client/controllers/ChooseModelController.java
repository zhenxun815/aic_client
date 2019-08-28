package com.tqhy.client.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tqhy.client.models.entity.Case;
import com.tqhy.client.models.entity.Model;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.utils.DateUtils;
import com.tqhy.client.utils.FXMLUtils;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
    public ListView<Case> case_list;
    @FXML
    public ListView<Model> model_list;
    @FXML
    public TextField text_field_case_id;
    @FXML
    public Label label_tips;
    @Autowired
    ReadModelController readModelController;
    private List<Model> chosenModels = new ArrayList();
    private Case chosenCase = null;

    @FXML
    public void initialize() {
        logger.info("choose model initialize...");
        chosenModels.clear();

        Network.getAicApi()
               .getAllModels()
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .subscribe(responseBody -> {
                   String json = responseBody.string();
                   logger.info("get all models res is {}", json);
                   ClientMsg<Model> clientMsg = new Gson().fromJson(json, new TypeToken<ClientMsg<Model>>() {
                   }.getType());
                   logger.info("client msg flag is {}", clientMsg.getFlag());
                   if (BaseMsg.SUCCESS == clientMsg.getFlag()) {
                       Platform.runLater(() -> model_list.getItems().addAll(clientMsg.getData()));
                   }
               });


        FXMLUtils.center2Display(base_pane);

        model_list.setCellFactory(CheckBoxListCell.forListView(model -> {
            BooleanProperty observable = new SimpleBooleanProperty();
            observable.addListener((obs, wasSelected, isNowSelected) -> {
                logger.info("check status old: {}, new: {}", wasSelected, isNowSelected);
                if (isNowSelected) {
                    logger.info("add choose model {}", model.getName());
                    chosenModels.add(model);
                } else {
                    logger.info("remove choose model {}", model.getName());
                    chosenModels.remove(model.getId());
                }
            });
            return observable;
        }, new StringConverter<Model>() {
            Model model;

            @Override
            public String toString(Model model) {
                this.model = model;
                return model.getName();
            }

            @Override
            public Model fromString(String string) {
                return model;
            }
        }));
    }

    @FXML
    public void submit(MouseEvent mouseEvent) {
        logger.info("into submit");
        /*for (Model choosedModel : chosenModels) {
            logger.info("choosed model is {}", choosedModel.getName());
        }*/
        String patientId = text_field_case_id.getText();
        if (StringUtils.isEmpty(patientId)) {
            logger.info("case Id must not be empty!");
            label_tips.setText("请输入患者id!");
        } else {
            label_tips.setText("");
            if (chosenModels.size() > 0) {
                readModelController.show(chosenCase, chosenModels);
                cancel(mouseEvent);
            } else {
                logger.info("must check at least one model!");
                label_tips.setText("请至少选择一个模型!");
            }
        }
    }

    @FXML
    public void cancel(MouseEvent mouseEvent) {
        Stage stage = getOwnerStageFromEvent(mouseEvent);
        stage.close();
    }

    @FXML
    public void confirm(MouseEvent mouseEvent) {
        String patientId = text_field_case_id.getText();
        if (StringUtils.isEmpty(patientId)) {
            label_tips.setText("请输入患者id!");
            return;
        }

        Network.getAicApi()
               .searchCase(patientId)
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .subscribe(responseBody -> {
                   String json = responseBody.string();
                   logger.info("get all cases res is {}", json);
                   ClientMsg<Case> clientMsg = new Gson().fromJson(json, new TypeToken<ClientMsg<Case>>() {
                   }.getType());
                   logger.info("client msg flag is {}", clientMsg.getFlag());
                   if (BaseMsg.SUCCESS == clientMsg.getFlag()) {
                       ObservableList<Case> caseLists = FXCollections.observableArrayList();
                       caseLists.addAll(clientMsg.getData());
                       Platform.runLater(() -> case_list.setItems(caseLists));
                   }
               });
        case_list.setCellFactory(param -> new RadioListCell());

    }

    private ToggleGroup group = new ToggleGroup();

    private class RadioListCell extends ListCell<Case> {

        RadioButton radioButton;
        ChangeListener<Boolean> radioListener = (src, ov, nv) -> radioChanged(nv);
        WeakChangeListener<Boolean> weakRadioListener = new WeakChangeListener(radioListener);

        public RadioListCell() {
            radioButton = new RadioButton();
            radioButton.selectedProperty().addListener(weakRadioListener);
            radioButton.setFocusTraversable(false);
            // let it span the complete width of the list
            // needed in fx8 to update selection state
            radioButton.setMaxWidth(Double.MAX_VALUE);
        }

        protected void radioChanged(boolean selected) {
            if (selected && getListView() != null && !isEmpty() && getIndex() >= 0) {
                chosenCase = getItem();
                logger.info("radio changed {}", getIndex());
            }
        }

        @Override
        public void updateItem(Case caseEntity, boolean empty) {
            super.updateItem(caseEntity, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
                radioButton.setToggleGroup(null);
            } else {
                radioButton.setText(itemShowText(caseEntity));
                radioButton.setToggleGroup(group);
                radioButton.setSelected(Objects.equals(caseEntity, chosenCase));
                if (isSelected()) {
                    logger.info("selected case is: {}", caseEntity.getId());
                }
                setGraphic(radioButton);
            }
        }

        public String itemShowText(Case caseEntity) {
            long dateTimeMills = caseEntity.getSeriesDate() + caseEntity.getSeriesTime();
            String datetimeStr = DateUtils.getDatetimeFromMills(dateTimeMills);

            String part = caseEntity.getPart();
            StringBuilder textBuilder = new StringBuilder("拍摄时间: ").append(datetimeStr);
            if (StringUtils.isEmpty(part)) {
                return textBuilder.toString();
            }
            return textBuilder.append(", 拍摄部位: ").append(part).toString();
        }
    }
}
