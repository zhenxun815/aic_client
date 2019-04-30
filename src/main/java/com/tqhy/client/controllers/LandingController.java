package com.tqhy.client.controllers;

import com.tqhy.client.config.Constants;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.LandingMsg;
import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.models.msg.local.VerifyMsg;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.service.HeartBeatService;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.GsonUtils;
import com.tqhy.client.utils.NetworkUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Yiheng
 * @create 3/18/2019
 * @since 1.0.0
 */
@RestController
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

    @Value("${network.url.init-landing:''}")
    private String initLandingUrl;

    @Value("${network.url.init-connection:''}")
    private String initConnectionUrl;

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

    @FXML
    private void initialize() {
        initWebEngine(Network.SERVER_IP);
        initJumpToLanding();
    }

    /**
     * 初始化webEngine,根据是否有serverIP判断是否显示测试连接页面
     *
     * @param serverIP
     */
    private void initWebEngine(String serverIP) {
        //禁用右键菜单
        //webView.setContextMenuEnabled(false);
        logger.info("into init webEngine..");
        String initUrl = StringUtils.isEmpty(serverIP) ? connectionUrl : landingUrl;
        if (!StringUtils.isEmpty(initUrl)) {
            webView.setCache(true);
            webView.setCacheHint(CacheHint.SPEED);

            WebEngine webEngine = webView.getEngine();
            initWebAlert(webEngine);
            logger.info("localUrl is: " + initUrl);
            webEngine.load(Network.LOCAL_BASE_URL + initUrl);
        }
    }

    private void initWebAlert(WebEngine webEngine) {

        webEngine.setOnAlert(event -> {
            String data = event.getData();
            logger.info("alert data is: " + data);
            //alert('upload;case;' + projectId + ';' + projectName)
            //alert('upload;test;' + taskId + ';' + projectName)
            if (data.startsWith(Constants.CMD_MSG_UPLOAD)) {
                String[] split = data.split(";");
                String uploadId = split[1];
                String uploadType = split[2];
                String uploadTargetName = split[3];
                uploadFileController.openUpload(UploadMsg.with(uploadId, uploadType, uploadTargetName));
            } else if (Constants.CMD_MSG_LOGOUT.equals(data)) {
                heartBeatService.stopBeat();
                logout();
            } else {
                showAlert(data);
            }
        });
    }


    /**
     * 初始化跳转登录页面逻辑
     */
    private void initJumpToLanding() {
        jumpToLandingFlag.bindBidirectional(heartBeatService.jumpToLandingFlagProperty());
        jumpToLandingFlag.addListener((observable, oldValue, newValue) -> {
            logger.info("jumpToLandingFlag changed,oldValue is: " + oldValue + ", newValue is: " + newValue);
            if (newValue) {
                Platform.runLater(() -> {
                    WebEngine webEngine = webView.getEngine();
                    webEngine.load(Network.LOCAL_BASE_URL + landingUrl);
                });
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
    private void showAlert(String message) {
        Dialog<ButtonType> alert = new Dialog<>();
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(NetworkUtils.toExternalForm("/static/img/logo_title_light.png")));
        alert.getDialogPane().setContentText(message);
        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.showAndWait();
    }

    @PostMapping("/landing")
    @ResponseBody
    public VerifyMsg landing(@RequestBody LandingMsg landingMsg) {
        logger.info("get LandingMsg.." + landingMsg);
        VerifyMsg response = new VerifyMsg();

       /* String physicalAddress = NetworkUtils.getPhysicalAddress();
        if (StringUtils.isEmpty(physicalAddress)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("Mac地址获取失败!");
            return response;
        }*/

        String localIp = NetworkUtils.getLocalIp();
        if (!NetworkUtils.isIP(localIp)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("IP地址获取失败!");
            return response;
        }

        String userName = landingMsg.getUserName().trim();
        String userPwd = landingMsg.getUserPwd().trim();

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
                    logger.info("ping server: " + serverIP + " success");

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
                                 .executeScript("callJsFunction(\"" + msg + "\")");
        String s = (String) response;
        logger.info("get response: " + s);
    }
}
