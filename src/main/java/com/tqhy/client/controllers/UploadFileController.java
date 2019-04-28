package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.task.UploadWorkerTask;
import com.tqhy.client.utils.FXMLUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /**
     * 是否跳转登录页面flag
     */
    BooleanProperty jumpToLandFlag = new SimpleBooleanProperty(false);

    /**
     * 主窗口是否最小化
     */
    BooleanProperty mainStageIconified = new SimpleBooleanProperty();

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

    /**
     * 显示将文件上传至哪个集合
     */
    @FXML
    Text text_choose_desc;

    /**
     * 选择上传文件提示
     * 未选择显示:未选择任何文件;已选择显示:选择文件夹的全路径
     */
    @FXML
    Text text_choose_info;

    /**
     * 上传进度百分比提示内容
     */
    @FXML
    Text text_progress_info;

    /**
     * 上传成功提示内容,显示本次上传批次号
     */
    @FXML
    Text text_success_info;

    /**
     * 上传进度条
     */
    @FXML
    ProgressBar progress_bar_upload;

    @Autowired
    LandingController landingController;

    @FXML
    public void initialize() {
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        showPanel(panel_choose.getId());

        jumpToLandFlag.addListener((observable, oldVal, newVal) -> {
            if (newVal) {
                logger.info("jump to land...");
                landingController.logout();
                Platform.runLater(() -> stage.hide());
                jumpToLandFlag.set(false);
            }
        });

        mainStageIconified.bind(ClientApplication.stage.iconifiedProperty());
        mainStageIconified.addListener((observable, oldVal, newVal) -> {
            logger.info("main stage iconified state change..." + newVal);
            stage.setIconified(newVal);
        });
    }

    /**
     * 数据重置
     */
    private void resetValues() {
        uploadMsg = null;
        dirToUpload = null;
    }

    /**
     * 开始上传
     *
     * @param mouseEvent
     */
    @FXML
    public void startUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            if (null == dirToUpload) {
                return;
            }
            logger.info("dir to upload: " + dirToUpload.getAbsolutePath());

            //显示上传中界面
            showPanel(panel_progress.getId());

            UploadWorkerTask workerTask = UploadWorkerTask.with(dirToUpload, uploadMsg);
            workerTask.messageProperty()
                      .addListener((observable, oldVal, newVal) -> {
                          if (UploadWorkerTask.PROGRESS_MSG_ERROR.equals(newVal)) {
                              logger.info("upload progress msg..." + newVal);
                              //显示上传失败页面
                              showPanel(panel_fail.getId());
                              text_progress_info.setText(0 + "%");
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


    /**
     * 取消上传,上传成功确认按钮与上传失败取消按钮亦调用此方法
     *
     * @param mouseEvent
     */
    @FXML
    public void cancelUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            resetValues();
            stage.hide();
            //通知页面刷新
            landingController.sendMsgToJs("uploadComplete");
        }
    }

    /**
     * 选择待上传文件夹
     *
     * @param mouseEvent
     */
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
     * 上传失败重试
     *
     * @param mouseEvent
     */
    @FXML
    public void retry(MouseEvent mouseEvent) {
        logger.info("into retry...");
        //显示上传中页面
        showPanel(panel_progress.getId());
        startUpload(mouseEvent);
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
        uploadMsg.setToken(Network.TOKEN);
        logger.info("uploadTargetName to upload is: " + uploadMsg.getUploadTargetName() + ", batchNumber is: " + batchNumber);
        Platform.runLater(() -> {
            stage = new Stage();
            FXMLUtils.loadWindow(stage, "/static/fxml/upload.fxml");
            text_choose_desc.setText("将数据导入至: " + uploadMsg.getUploadTargetName());
            text_success_info.setText("导入批次: " + uploadMsg.getBatchNumber());
        });
    }


}
