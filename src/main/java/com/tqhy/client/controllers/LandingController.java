package com.tqhy.client.controllers;

import com.tqhy.client.config.Constants;
import com.tqhy.client.models.entity.DownloadInfo;
import com.tqhy.client.models.entity.SaveDatas;
import com.tqhy.client.models.enums.DownloadTaskApi;
import com.tqhy.client.models.enums.SaveTaskType;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.*;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.service.HeartBeatService;
import com.tqhy.client.task.DownloadTask;
import com.tqhy.client.task.SaveFileTask;
import com.tqhy.client.utils.*;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @author Yiheng
 * @create 3/18/2019
 * @since 1.0.0
 */
@RestController
@Getter
@Setter
public class LandingController {

    static Logger logger = LoggerFactory.getLogger(LandingController.class);
    @FXML
    private WebView webView;

    @Value("${network.url.connection:''}")
    private String connectionUrl;

    @Value("${network.url.activating:''}")
    private String activatingUrl;

    @Value("${network.url.landing:''}")
    private String landingUrl;

    @Value("${network.url.test:''}")
    private String testUrl;

    @Value("${path.data:'/data/'}")
    private String localDataPath;

    @Autowired
    HeartBeatService heartBeatService;

    @Autowired
    UploadFileController uploadFileController;

    /**
     * 判断页面是否需要跳转到登录页,与{@link com.tqhy.client.service.HeartBeatService HeartBeatService} 中
     * {@code jumpToLandingFlag} 进行双向绑定
     */
    private BooleanProperty jumpToLandingFlag = new SimpleBooleanProperty(false);

    private WebEngine webEngine;

    @FXML
    private void initialize() {
        initWebView();
        initWebAlert();
        initJumpToLanding();
        loadPage(Network.SERVER_IP);
        //webEngine.load("https://www.baidu.com");
    }

    private void loadPage(String serverIP) {
        String initUrl = StringUtils.isEmpty(serverIP) ? connectionUrl : landingUrl;
        if (!StringUtils.isEmpty(initUrl)) {
            logger.info("localUrl is: " + initUrl);
            webEngine.load(Network.LOCAL_BASE_URL + initUrl);
        }
    }


    /**
     * 初始化webView设置与webEngine对象
     */
    public void initWebView() {
        logger.info("into init webEngine..");
        webView.setCache(true);
        webView.setCacheHint(CacheHint.SPEED);
        logger.info("init webView complete..");
        this.webEngine = webView.getEngine();
    }

    /**
     * 初始化web页面alert事件监听
     */
    private void initWebAlert() {
        webEngine.setOnAlert(event -> {
            String data = event.getData() + Constants.MSG_SPLITTER;
            logger.info("alert data is: " + data);

            String[] split = data.split(Constants.MSG_SPLITTER);
            switch (split[0]) {
                case Constants.CMD_MSG_UPLOAD:
                    //alert('upload;case;' + projectId + ';' + projectName)
                    //alert('upload;test;' + taskId + ';' + projectName)

                    String uploadType = split[1];
                    String uploadId = split[2];
                    String uploadTargetName = split[3];
                    uploadFileController.openUpload(UploadMsg.with(uploadType, uploadId, uploadTargetName));
                    break;
                case Constants.CMD_MSG_DOWNLOAD:
                    //download;{"fileName":"taskName","imgUrlString":"imgUrl1;imgUrl2"}
                    String downloadInfoStr = split[1];
                    Optional<DownloadInfo> downloadInfoOptional = GsonUtils.parseJsonToObj(downloadInfoStr, DownloadInfo.class);
                    onDownloadOption(downloadInfoOptional);
                    break;
                case Constants.CMD_MSG_SAVE:
                    /*save;{"fileName":"projectName",
                        "head":[{"title":"分类名称","key":"name","__id":"gCYIMF"},{"title":"已标注","key":"value","__id":"gcSMlC"},{"title":"占比","key":"per","__id":"37ZTmj"}],
                        "body":[{"name":"temp","value":2,"per":"8%"},{"name":"牙","value":11,"per":"44%"}]
                        }*/

                    String dataToSave = split[1];

                    Optional<SaveDatas> saveDataOptional = GsonUtils.parseJsonToObj(dataToSave, SaveDatas.class);
                    onSaveDataOption(saveDataOptional);
                    break;
                case Constants.CMD_MSG_LOGOUT:
                    heartBeatService.stopBeat();
                    logout();
                    break;
                default:
                    showAlert(data);
            }
        });
    }

    /**
     * 执行保存指令
     */
    private void onSaveDataOption(Optional<SaveDatas> saveDataOptional) {

        File saveDir = FXMLUtils.chooseDir(null);
        if (null == saveDir) {
            return;
        }

        if (saveDataOptional.isPresent()) {
            SaveDatas saveDatas = saveDataOptional.get();
            Observable.fromCallable(SaveFileTask.of(SaveDataMsg.of(SaveTaskType.SAVE_REPORT_TO_CSV, saveDir, saveDatas)))
                      .subscribeOn(Schedulers.io())
                      .observeOn(Schedulers.io())
                      .subscribe(saveDataMsgObservable -> {
                          saveDataMsgObservable.subscribe(saveDataMsg -> {
                              Integer saveFlag = saveDataMsg.getFlag();
                              if (BaseMsg.SUCCESS == saveFlag) {
                                  logger.info("save success");
                                  showAlert("保存完毕");
                              } else {
                                  logger.info("save fail");
                                  showAlert("保存失败");
                              }
                          });
                      });
        } else {
            logger.info("save optional is null");
            showAlert("保存失败");
        }
    }

    /**
     * 执行下载指令
     *
     * @param downloadInfoOptional
     */
    private void onDownloadOption(Optional<DownloadInfo> downloadInfoOptional) {
        File downloadDir = FXMLUtils.chooseDir(null);
        if (null == downloadDir) {
            return;
        }

        if (downloadInfoOptional.isPresent()) {
            DownloadInfo downloadInfo = downloadInfoOptional.get();
            Observable.fromCallable(DownloadTask.of(DownloadMsg.of(DownloadTaskApi.DOWNLOAD_PDF, downloadDir, downloadInfo)))
                      .subscribeOn(Schedulers.io())
                      .observeOn(Schedulers.io())
                      .subscribe(downloadMsgObservable ->
                                         downloadMsgObservable.subscribe(downloadMsg -> {
                                             Integer downloadFlag = downloadMsg.getFlag();
                                             if (BaseMsg.SUCCESS == downloadFlag) {
                                                 logger.info("download success");
                                                 showAlert("下载完毕");
                                             } else {
                                                 logger.info("download fail");
                                                 showAlert("下载失败");
                                             }
                                         }));
        } else {
            showAlert("下载失败");
        }
    }

    /**
     * 初始化跳转登录页面逻辑
     */
    private void initJumpToLanding() {
        jumpToLandingFlag.bindBidirectional(heartBeatService.jumpToLandingFlagProperty());
        jumpToLandingFlag.addListener((observable, oldValue, newValue) -> {
            logger.info("jumpToLandingFlag changed,oldValue is: " + oldValue + ", newValue is: " + newValue);
            if (newValue) {
                Platform.runLater(() -> webEngine.load(Network.LOCAL_BASE_URL + landingUrl));
                jumpToLandingFlag.set(false);
                Network.TOKEN = null;
            }
        });
    }

    /**
     * alert
     *
     * @param message
     */
    public void showAlert(String message) {
        Platform.runLater(() -> {
            Dialog<ButtonType> alert = new Dialog<>();
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(NetworkUtils.toExternalForm("/static/img/logo_title_light.png")));
            alert.getDialogPane().setContentText(message);
            alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
            alert.showAndWait();
        });
    }

    @PostMapping("/landing")
    @ResponseBody
    public VerifyMsg landing(@RequestBody LandingMsg landingMsg) {
        logger.info("get LandingMsg.." + landingMsg);
        VerifyMsg response = new VerifyMsg();

        String localIp = NetworkUtils.getLocalIp();
        if (!NetworkUtils.isIP(localIp)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("IP地址获取失败!");
            return response;
        }

        String userName = landingMsg.getUserName().trim();
        String userPwd = landingMsg.getUserPwd().trim();
        PropertyUtils.setUserName(userName);

        Network.getAicApi()
               .landing(userName, userPwd)
               .map(body -> {
                   ClientMsg clientMsg = GsonUtils.parseResponseToObj(body);
                   logger.info("flag is: " + clientMsg.getFlag());
                   response.setFlag(clientMsg.getFlag());
                   response.setDesc(clientMsg.getDesc());
                   List<String> msg = clientMsg.getMsg();
                   String token = msg.get(0);
                   logger.info("token is: " + token);
                   response.setToken(token);
                   response.setLocalIP(localIp);
                   response.setServerIP(Network.SERVER_IP);
                   return response;
               })
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .subscribe(res -> {
                   if (BaseMsg.SUCCESS == res.getFlag()) {
                       heartBeatService.startBeat(response.getToken());
                       Network.TOKEN = response.getToken();
                   }
               });

        return response;
    }

    @GetMapping("/logout")
    public void logout() {
        jumpToLandingFlag.set(true);
    }

    @GetMapping("/user/name")
    @ResponseBody
    public String getUserName() throws IOException {
        return PropertyUtils.getUserName();
    }


    @PostMapping("/verify/connection")
    public VerifyMsg activateClient(@RequestBody VerifyMsg msg) {
        logger.info("get request.." + msg);
        String serverIP = msg.getServerIP();
        VerifyMsg response = new VerifyMsg();
        if (NetworkUtils.isIP(serverIP)) {
            Network.SERVER_IP = serverIP;
            Network.setServerBaseUrl(serverIP);
            logger.info("base url is: " + Network.SERVER_BASE_URL);
            try {
                okhttp3.ResponseBody responseBody = Network.getAicApi()
                                                           .pingServer()
                                                           .execute()
                                                           .body();
                ClientMsg clientMsg = GsonUtils.parseResponseToObj(responseBody);

                if (BaseMsg.SUCCESS == clientMsg.getFlag()) {
                    logger.info("ping server: " + serverIP + " successCount");

                    File serverIPFile = FileUtils.getLocalFile(localDataPath, Constants.PATH_SERVER_IP);
                    FileUtils.writeFile(serverIPFile, serverIP, null, true);
                    response.setFlag(1);
                    response.setServerIP(Network.SERVER_IP);
                    return response;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            response.setFlag(0);
            response.setDesc("ip地址格式不正确");
        }

        return response;
    }

    /**
     * 向js传值
     *
     * @param msg
     */
    public void sendMsgToJs(String msg) {
        Object response = webView.getEngine()
                                 .executeScript("callJsFunction('" + msg + "')");
        String s = (String) response;
        logger.info("get response: " + s);
    }
}
