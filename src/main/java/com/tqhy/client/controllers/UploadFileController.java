package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.config.Constants;
import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.task.UploadWorkerTask;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    /**
     * 是否可以上传
     */
    private boolean uploadReadyFlag;

    @Value("${path.data:'/data/'}")
    private String localDataPath;

    @FXML
    VBox panels_parent;
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
     * 失败提示信息标题
     */
    @FXML
    public Text text_fail_title;
    /**
     * 不合法信息展示滚动页面
     */
    @FXML
    public ScrollPane scrollPane;

    /**
     * 不合法信息
     */
    @FXML
    public Label label_fail_desc;
    /**
     * 上传备注信息
     */
    @FXML
    TextField text_field_remarks;
    /**
     * 上传进度百分比
     */
    @FXML
    Text text_progress_info;
    /**
     * 进度条界面描述信息
     */
    @FXML
    public Text text_progress_desc;
    /**
     * 上传完毕提示内容,显示本次上传批次号
     */
    @FXML
    Text text_success_info;

    /**
     * 长传完毕显示提示内容
     */
    @FXML
    Text text_success_desc;
    /**
     * 上传进度条
     */
    @FXML
    ProgressBar progress_bar_upload;

    @FXML
    Button btn_failed_check;

    @FXML
    HBox box_complete;

    @Autowired
    LandingController landingController;

    private UploadWorkerTask workerTask;

    private VBox[] panels;

    @FXML
    public void initialize() {
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();

        stage.setWidth(visualBounds.getWidth());
        stage.setHeight(visualBounds.getHeight());
        stage.centerOnScreen();
        panels = new VBox[]{panel_choose, panel_progress, panel_fail, panel_success};
        showPanel(panel_choose.getId());

        jumpToLandFlag.addListener((observable, oldVal, newVal) -> {
            if (newVal) {
                logger.info("jump to land...");
                landingController.logout();
                Platform.runLater(() -> stage.hide());
                jumpToLandFlag.set(false);
            }
        });

        uploadReadyFlag = false;

        mainStageIconified.bind(ClientApplication.stage.iconifiedProperty());
        mainStageIconified.addListener((observable, oldVal, newVal) -> {
            logger.info("main stage iconified state change..." + newVal);
            stage.setIconified(newVal);
        });

        text_field_remarks.setOnKeyPressed(event -> {
            int length = text_field_remarks.getLength();
            if (length >= 50) {
                String remarks = text_field_remarks.getText().substring(0, 50);
                text_field_remarks.setText(remarks);
            }
        });

        scrollPane.setFitToWidth(true);
        label_fail_desc.setWrapText(true);
    }

    /**
     * 数据重置
     */
    private void resetValues() {
        dirToUpload = null;
        text_choose_info.setText("未选择任何文件!");
        uploadReadyFlag = false;
    }

    /**
     * 开始上传
     *
     * @param mouseEvent
     */
    @FXML
    public void startUpload(MouseEvent mouseEvent) {
        if (!uploadReadyFlag) {
            return;
        }
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info(button.name() + "....");
            if (null == dirToUpload) {
                return;
            }


            logger.info("dir to upload: " + dirToUpload.getAbsolutePath());


            //显示上传中界面
            showPanel(panel_progress.getId());
            text_progress_info.setText(0.00 + "%");
            String remarksText = text_field_remarks.getText() == null ? "" : text_field_remarks.getText();
            logger.info("remark is: {}", remarksText);
            String remarks = UploadMsg.UPLOAD_TYPE_CASE.equals(uploadMsg.getUploadType()) ? remarksText : "";
            remarks = remarks.length() > 50 ? remarks.substring(0, 50) : remarks;
            uploadMsg.setRemarks(remarks);
            workerTask = UploadWorkerTask.with(dirToUpload, uploadMsg, localDataPath);

            workerTask.messageProperty()
                      .addListener((observable, oldVal, newVal) -> {
                          DecimalFormat decimalFormat = new DecimalFormat("#0.0");
                          logger.info("upload progress msg..." + newVal);
                          String[] msgSplit = newVal.split(";");
                          switch (msgSplit[0]) {
                              case UploadWorkerTask.PROGRESS_MSG_COMPLETE:
                                  //显示上传成功页面
                                  showPanel(panel_success.getId());

                                  String completeCount = msgSplit[1];
                                  String errorCount = msgSplit[2];
                                  String completeMsg = "上传完毕,成功: " + completeCount + " 条, 失败: " + errorCount + " 条!";
                                  FXMLUtils.displayChildNode(box_complete, btn_failed_check, Integer.parseInt(errorCount) > 0);
                                  text_success_desc.setText(completeMsg);
                                  break;
                              case UploadWorkerTask.PROGRESS_MSG_COLLECT:
                                  text_progress_desc.setText("文件信息采集中,请耐心等待..");
                                  text_progress_info.setText(decimalFormat.format(Double.parseDouble(msgSplit[1])) + "%");
                                  break;
                              case UploadWorkerTask.PROGRESS_MSG_UPLOAD:
                                  text_progress_desc.setText("文件上传中,请耐心等待..");
                                  logger.info("upload progress msg..." + newVal);
                                  text_progress_info.setText(decimalFormat.format(Double.parseDouble(msgSplit[1])) + "%");
                                  break;
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

        panels_parent.setLayoutX((stage.getWidth() - 600) / 2);
        panels_parent.setLayoutY((stage.getHeight() - 248) / 2);

        panels_parent.getChildren().removeAll(panels);
        VBox panelToShow = Arrays.stream(panels)
                                 .filter(panel -> panel.getId().equals(panelId))
                                 .findFirst()
                                 .get();
        if (panelToShow.getId().equals(panel_choose.getId())) {
            if (UploadMsg.UPLOAD_TYPE_CASE.equals(uploadMsg.getUploadType())) {
                FXMLUtils.displayChildNode(panel_choose, text_field_remarks, true);
                text_choose_desc.setText("将数据导入至: " + uploadMsg.getUploadTargetName());
                text_choose_desc.setVisible(true);
                //logger.info("upload case...target name is: [{}]",uploadMsg.getUploadTargetName());
            } else if (UploadMsg.UPLOAD_TYPE_TEST.equals(uploadMsg.getUploadType())) {
                FXMLUtils.displayChildNode(panel_choose, text_field_remarks, false);
                text_choose_desc.setVisible(false);
                logger.info("upload test...");
            }
        }

        panels_parent.getChildren()
                     .add(panelToShow);
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
                File[] files = dirToUpload.listFiles();

                if (null != files && files.length > 0) {
                    logger.info("choose dirToUpload is: [{}]", dirToUpload.getAbsolutePath());
                    File[] caseDirs = dirToUpload.listFiles(File::isDirectory);
                    if (null != caseDirs && caseDirs.length > 0) {
                        List<File> unvalidDirs = Arrays.stream(caseDirs)
                                                       .filter(caseDir -> {
                                                           File[] caseSubDirs = caseDir.listFiles(File::isDirectory);
                                                           return null != caseSubDirs && caseSubDirs.length > 0;
                                                       }).collect(Collectors.toList());
                        if (unvalidDirs.size() > 0) {
                            String paths = unvalidDirs.stream()
                                                      .collect(StringBuilder::new,
                                                               (builder, dir) ->
                                                                       builder.append(dir.getAbsolutePath())
                                                                              .append(Constants.NEW_LINE),
                                                               StringBuilder::append)
                                                      .toString();
                            text_fail_title.setText("以下文件夹路径结构不符合规则");
                            label_fail_desc.setText(paths);
                            showPanel(panel_fail.getId());
                        }
                    }
                    text_choose_info.setText(dirToUpload.getAbsolutePath());
                    uploadReadyFlag = true;
                } else {
                    logger.info("choose dirToUpload error");
                    text_choose_info.setText("文件夹路径不合法!");
                    uploadReadyFlag = false;
                }
            }
        }
    }

    @FXML
    public void checkFailed(MouseEvent mouseEvent) {
        logger.info("check upload failed files...");
        String batchNumber = uploadMsg.getBatchNumber();
        File uploadInfoFile = FileUtils.getLocalFile(localDataPath, batchNumber + ".txt");
        String failedInfos = FileUtils.readLine(uploadInfoFile, line -> line.concat(Constants.NEW_LINE))
                                      .stream()
                                      .collect(StringBuilder::new,
                                               StringBuilder::append,
                                               StringBuilder::append)
                                      .toString();
        text_fail_title.setText("以下文件上传失败");
        label_fail_desc.setText(failedInfos);
        showPanel(panel_fail.getId());
        /*try {
            Desktop.getDesktop().open(uploadInfoFile);
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @FXML
    public void stopUpload(MouseEvent mouseEvent) {
        MouseButton button = mouseEvent.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            logger.info("into stop upload....");
            workerTask.getStopUploadFlag().set(true);
            showPanel(panel_choose.getId());
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
        resetValues();
        //显示上传中页面
        showPanel(panel_choose.getId());

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
            text_success_info.setText("导入批次: " + uploadMsg.getBatchNumber());

            @NonNull String uploadType = uploadMsg.getUploadType();
            //text_field_remarks.setVisible(UploadMsg.UPLOAD_TYPE_CASE.equals(uploadType));
            if (UploadMsg.UPLOAD_TYPE_CASE.equals(uploadType)) {
                FXMLUtils.displayChildNode(panel_choose, text_field_remarks, true);
                text_choose_desc.setText("将数据导入至: " + uploadMsg.getUploadTargetName());
                text_choose_desc.setVisible(true);
                //logger.info("upload case...target name is: [{}]",uploadMsg.getUploadTargetName());
            } else if (UploadMsg.UPLOAD_TYPE_TEST.equals(uploadType)) {
                FXMLUtils.displayChildNode(panel_choose, text_field_remarks, false);
                text_choose_desc.setVisible(false);
                logger.info("upload test...");
            }
        });
    }


}
