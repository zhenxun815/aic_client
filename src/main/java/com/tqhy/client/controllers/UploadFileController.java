package com.tqhy.client.controllers;

import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.utils.FXMLUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
@RestController
public class UploadFileController {

    Logger logger = LoggerFactory.getLogger(UploadFileController.class);
    Stage stage;

    /**
     * 上传目标课题
     */
    String subjectName;

    /**
     * 待上传文件夹
     */
    File directory;

    @FXML
    Text text_desc;

    @FXML
    public void startUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            logger.info("start upload: " + directory.getAbsolutePath());
            //todo 上传
        }
    }

    @FXML
    public void cancelUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            stage.hide();
        }
    }

    @FXML
    public void chooseDirectory(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directory = directoryChooser.showDialog(stage);
            if (null != directory) {
                logger.info("choose directory is: " + directory.getAbsolutePath());
            }
        }
    }

    /**
     * 开启上传窗口,舒适化页面
     * @param uploadMsg
     */
    @PostMapping("/upload/start")
    public void openUpload(@RequestBody UploadMsg uploadMsg) {

        subjectName = uploadMsg.getSubjectName();
        logger.info("subjectName to upload is: " + subjectName);
        Platform.runLater(() -> {
            stage = new Stage();
            FXMLUtils.loadWindow(stage, "/static/fxml/upload_choose.fxml");
            text_desc.setText("将数据导入至: " + subjectName);
        });
    }

}
