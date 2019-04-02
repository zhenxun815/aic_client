package com.tqhy.client.controllers;

import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.task.UploadWorkerTask;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
@RestController
public class UploadFileController {

    Logger logger = LoggerFactory.getLogger(UploadFileController.class);
    Stage stage;

    BooleanProperty jumpToLandFlag = new SimpleBooleanProperty(false);

    /**
     * 本次上传信息
     */
    private UploadMsg uploadMsg;

    /**
     * 待上传文件夹
     */
    private File dirToUpload;

    @FXML
    VBox panel_choose;
    @FXML
    VBox panel_progress;
    @FXML
    VBox panel_success;
    @FXML
    VBox panel_fail;

    @FXML
    Text text_choose_desc;
    @FXML
    Text text_choose_info;
    @FXML
    Text text_progress_info;
    @FXML
    ProgressBar progress_bar_upload;


    @FXML
    public void initialize() {
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        showPanel(panel_choose.getId());
        jumpToLandFlag.addListener((observable, oldVal, newVal) -> {
            if (newVal) {
                //todo 跳转到登录页面
                logger.info("jump to land...");
                //stage.hide();
            }
        });
    }

    /**
     * 数据清空
     */
    private void resetValues() {
        uploadMsg = null;
        dirToUpload = null;
    }

    @FXML
    public void startUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            if (null == dirToUpload) {
                return;
            }
            logger.info("start upload: " + dirToUpload.getAbsolutePath());

            //显示上传中界面
            showPanel(panel_progress.getId());

            List<File> filesInDir = FileUtils.getFilesInDir(dirToUpload);
            List<File> filesToUpload = FileUtils.transAllToJpg(filesInDir);

            UploadWorkerTask workerTask = UploadWorkerTask.with(filesToUpload, uploadMsg);
            workerTask.messageProperty()
                      .addListener((observable, oldVal, newVal) -> {
                          if (UploadWorkerTask.PROGRESS_MSG_ERROR.equals(newVal)) {
                              logger.info("upload progress msg..." + newVal);
                              //显示上传失败页面
                              showPanel(panel_fail.getId());
                          } else if (UploadWorkerTask.PROGRESS_MSG_COMPLETE.equals(newVal)) {
                              logger.info("upload progress msg..." + newVal);
                              //显示上传成功页面
                              showPanel(panel_success.getId());
                          } else {
                              logger.info("upload progress msg..." + newVal);
                              text_progress_info.setText(newVal + "%");
                          }
                      });

            jumpToLandFlag.bindBidirectional(workerTask.getJumpToLandFlag());
            progress_bar_upload.progressProperty()
                               .bind(workerTask.progressProperty());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(workerTask);
        }
    }

    /**
     * 显示对应界面
     *
     * @param panelId 待显示界面的id
     */
    private void showPanel(String panelId) {
        panel_choose.setVisible(panel_choose.getId().equals(panelId));
        panel_progress.setVisible(panel_progress.getId().equals(panelId));
        panel_fail.setVisible(panel_fail.getId().equals(panelId));
        panel_success.setVisible(panel_success.getId().equals(panelId));
    }


    @FXML
    public void cancelUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            resetValues();
            stage.hide();
        }
    }

    @FXML
    public void chooseDirectory(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            DirectoryChooser directoryChooser = new DirectoryChooser();
            dirToUpload = directoryChooser.showDialog(stage);
            if (null != dirToUpload) {
                logger.info("choose dirToUpload is: " + dirToUpload.getAbsolutePath());
                text_choose_info.setText(dirToUpload.getAbsolutePath());
            }
        }
    }

    /**
     * 开启上传窗口,初始化页面
     *
     * @param msg 本次上传信息
     */
    @PostMapping("/upload/start")
    public void openUpload(@RequestBody UploadMsg msg) {

        uploadMsg = msg;
        String batchNumber = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        uploadMsg.setBatchNumber(batchNumber);
        logger.info("caseName to upload is: " + uploadMsg.getCaseName()+", batchNumber is: "+batchNumber);
        Platform.runLater(() -> {
            stage = new Stage();
            FXMLUtils.loadWindow(stage, "/static/fxml/upload.fxml");
            text_choose_desc.setText("将数据导入至: " + uploadMsg.getCaseName());
        });
    }


    @FXML
    public void retry(MouseEvent mouseEvent) {
        logger.info("into retry...");
        //显示上传中页面
        showPanel(panel_progress.getId());
        startUpload(mouseEvent);
    }
}
