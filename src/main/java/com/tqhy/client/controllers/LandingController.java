package com.tqhy.client.controllers;

import com.google.gson.Gson;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.LandingMsg;
import com.tqhy.client.models.msg.local.VerifyMsg;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.service.HeartBeatService;
import com.tqhy.client.utils.GsonUtils;
import com.tqhy.client.utils.NetworkUtils;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @Value("${network.url.init:''}")
    private String initUrl;

    @Autowired
    HeartBeatService heartBeatService;

    /**
     * 判断页面是否需要跳转到登录页,与{@link com.tqhy.client.service.HeartBeatService HeartBeatService} 中
     * {@code jumpToLandingFlag} 进行双向绑定
     */
    private BooleanProperty jumpToLandingFlag = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {

        webView.setContextMenuEnabled(false);
        String localUrl = NetworkUtils.toExternalForm(initUrl);
        if (!StringUtils.isEmpty(localUrl)) {
            WebEngine webEngine = webView.getEngine();
            webEngine.load(localUrl);
        }

        jumpToLandingFlag.bindBidirectional(heartBeatService.jumpToLandingFlagProperty());
        jumpToLandingFlag.addListener((observable, oldValue, newValue) -> {
            logger.info("jumpToLandingFlag changed,oldValue is: " + oldValue + ", newValue is: " + newValue);
            if (newValue) {
                Platform.runLater(() -> {
                    WebEngine webEngine = webView.getEngine();
                    webEngine.load(NetworkUtils.toExternalForm(landingUrl));
                });
            }
        });
    }

    @PostMapping("/landing")
    @ResponseBody
    public VerifyMsg landing(@RequestBody LandingMsg landingMsg) {
        logger.info("get LandingMsg.." + landingMsg);
        VerifyMsg response = new VerifyMsg();

        String physicalAddress = NetworkUtils.getPhysicalAddress();
        if (StringUtils.isEmpty(physicalAddress)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("Mac地址获取失败!");
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
                   response.setToken(msg.get(0));
                   return response;
               })
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .subscribe(res -> {
                   if (BaseMsg.SUCCESS == res.getFlag()) {
                       heartBeatService.startBeat(response.getToken());
                   }
               });

        return response;

    }

    @GetMapping("/logout")
    public void logout() {
        jumpToLandingFlag.set(true);
    }


    @Deprecated
    @PostMapping("/verify/activation")
    public VerifyMsg activateClient(@RequestBody VerifyMsg msg) {
        logger.info("get request.." + msg);
        String serializableNum = msg.getToken();
        logger.info("get token: " + serializableNum);

        //todo 验证序列号
        boolean valid = true;
        VerifyMsg response = new VerifyMsg();
        response.setFlag(valid ? BaseMsg.SUCCESS : BaseMsg.FAIL);
        response.setDesc(valid ? "激活成功" : "激活失败");
        return response;
    }

}
