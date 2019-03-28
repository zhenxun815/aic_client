package com.tqhy.client.controllers;

import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.NetworkUtils;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    String caseName;

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
            HashMap<String, String> map = new HashMap<>();
            map.put("case", caseName);
            map.put("client", NetworkUtils.getPhysicalAddress());
            Map<String, okhttp3.RequestBody> requestParamMap = NetworkUtils.createRequestParamMap(map);

            File[] files = directory.listFiles(File::isFile);
            int fileCount = files.length;
            AtomicInteger completeCount = new AtomicInteger();
            Arrays.stream(files)
                  .forEach(file -> {
                      MultipartBody.Part filePart = NetworkUtils.createFilePart("file", file.getAbsolutePath());
                      Network.getAicApi()
                             .uploadFiles(requestParamMap, filePart)
                             .observeOn(Schedulers.io())
                             .subscribeOn(Schedulers.trampoline())
                             .subscribe(new Observer<ResponseBody>() {
                                 @Override
                                 public void onSubscribe(Disposable d) {
                                     logger.info("Disposable: " + d);
                                 }

                                 @Override
                                 public void onNext(ResponseBody responseBody) {
                                     try {
                                         String json = responseBody.string();
                                         logger.info("json is: " + json);
                                     } catch (IOException e) {
                                         e.printStackTrace();
                                     }
                                 }

                                 @Override
                                 public void onError(Throwable e) {
                                     e.printStackTrace();
                                 }

                                 @Override
                                 public void onComplete() {
                                     logger.info("complete");
                                     int complete = completeCount.addAndGet(1);
                                     double progress = (complete + 0D) / fileCount;
                                 }
                             });
                  });


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
     * 开启上传窗口,初始化页面
     *
     * @param uploadMsg
     */
    @PostMapping("/upload/start")
    public void openUpload(@RequestBody UploadMsg uploadMsg) {

        caseName = uploadMsg.getCaseName();
        logger.info("caseName to upload is: " + caseName);
        Platform.runLater(() -> {
            stage = new Stage();
            FXMLUtils.loadWindow(stage, "/static/fxml/upload_choose.fxml");
            text_desc.setText("将数据导入至: " + caseName);
        });
    }

}
