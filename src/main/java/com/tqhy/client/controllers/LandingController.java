package com.tqhy.client.controllers;

import ch.qos.logback.core.util.FileUtil;
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
        initJumpToLanding();
        String serverIP = initServerIP();
        initWebEngine(serverIP);
    }

    private void initWebEngine(String serverIP) {
        //禁用右键菜单
        //webView.setContextMenuEnabled(false);
        logger.info("into init webEngine..");
        String initUrl = StringUtils.isEmpty(serverIP) ? initConnectionUrl : initLandingUrl;
        String localUrl = NetworkUtils.toExternalForm(initUrl);
        if (!StringUtils.isEmpty(localUrl)) {
            WebEngine webEngine = webView.getEngine();
            webEngine.setOnAlert(event -> {
                String data = event.getData();
                logger.info("alert data is: " + data);
                if (data.startsWith(Constants.CMD_MSG_UPLOAD)) {
                    String[] split = data.split(";");
                    String projectId = split[1];
                    String projectName = split[2];
                    uploadFileController.openUpload(UploadMsg.with(projectId, projectName));
                } else if (Constants.CMD_MSG_LOGOUT.equals(data)) {
                    heartBeatService.stopBeat();
                    logout();
                } else {
                    showAlert(data);
                }
            });

            logger.info("localUrl is: " + localUrl);
            webEngine.load(localUrl);
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
                Platform.runLater(() -> {
                    WebEngine webEngine = webView.getEngine();
                    webEngine.load(NetworkUtils.toExternalForm(initLandingUrl));
                });
                jumpToLandingFlag.set(false);
                Network.TOKEN = null;
            }
        });
    }

    /**
     * 初始化获取后台IP地址
     *
     * @return
     */
    private String initServerIP() {
        logger.info("into init webEngine..");
        File serverIPFile = FileUtils.getLocalFile(localDataPath, Constants.PATH_SERVER_IP);
        if (serverIPFile.exists()) {
            List<String> datas = FileUtils.readLine(serverIPFile, line -> line);
            String serverIP = datas.size() > 0 ? datas.get(0).trim() : "";
            VerifyMsg verifyMsg = new VerifyMsg();
            verifyMsg.setServerIP(serverIP);
            VerifyMsg response = activateClient(verifyMsg);
            if (BaseMsg.SUCCESS == response.getFlag()) {
                return serverIP;
            }
        }
        return "";
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

        String userName = landingMsg.getUserName();
        String userPwd = landingMsg.getUserPwd();

        Network.getAicApi()
               .landing(userName, userPwd)
               .map(body -> {
                   ClientMsg clientMsg = GsonUtils.parseResponseToObj(body);
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
            Network.setBaseUrl(serverIP);
            logger.info("base url is: " + Network.BASE_URL);
            Network.getAicApi()
                   .pingServer()
                   .map(body -> {
                       ClientMsg clientMsg = GsonUtils.parseResponseToObj(body);
                       Integer flag = clientMsg.getFlag();
                       response.setFlag(flag);
                       return response;
                   })
                   .observeOn(Schedulers.io())
                   .subscribeOn(Schedulers.trampoline())
                   .doOnError(error -> error.printStackTrace())
                   .subscribe(res -> {
                       if (BaseMsg.SUCCESS == res.getFlag()) {
                           logger.info("ping server: " + serverIP + " success");
                           Network.SERVER_IP = serverIP;


                           File serverIPFile = FileUtils.getLocalFile(localDataPath, Constants.PATH_SERVER_IP);
                           FileUtils.writeFile(serverIPFile, serverIP, null, true);
                       }
                   });
        } else {
            response.setFlag(0);
            response.setDesc("ip地址格式不正确");
        }

        return response;
    }

}
