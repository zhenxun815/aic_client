package com.tqhy.client.controllers;

import com.google.gson.Gson;
import com.tqhy.client.ClientApplication;
import com.tqhy.client.models.User;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.LandingMsg;
import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.models.msg.local.VerifyMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.service.HeartBeatService;
import com.tqhy.client.utils.NetworkUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yiheng
 * @create 3/18/2019
 * @since 1.0.0
 */
@RestController
public class LandingController extends BaseWebviewController {

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

    @FXML
    private void initialize() {
        // logger.info("connectionUrl is: " + connectionUrl);
        // logger.info("activatingUrl is: " + activatingUrl);
        String localUrl = NetworkUtils.toExternalForm(initUrl);
        if (!StringUtils.isEmpty(localUrl)) {
            WebEngine webEngine = webView.getEngine();
            webEngine.load(localUrl);
        }
    }

    @PostMapping("/landing")
    @ResponseBody
    public VerifyMsg landing(@RequestBody LandingMsg msg) {
        logger.info("get LandingMsg.." + msg);
        VerifyMsg response = new VerifyMsg();

        String physicalAddress = NetworkUtils.getPhysicalAddress();
        if (StringUtils.isEmpty(physicalAddress)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("Mac地址获取失败!");
            return response;
        }
        String userName = msg.getUserName();
        String userPwd = msg.getUserPwd();
        HashMap<String, String> userMap = new HashMap<>();

        userMap.put("userName", userName);
        userMap.put("passWord", userPwd);

        Network.getAiHelperApi().landing(Network.createRequestParamMap(userMap))
               .map(body -> {
                   String json = body.string();
                   logger.info("AiWarningDialogController postDocConfirm recieve json:" + json);
                   return json;
               })
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .subscribe(json -> {
                   logger.info("landing response: " + json);


               });


        String serializableNum = "";

        if (StringUtils.isEmpty(serializableNum)) {

            response.setFlag(BaseMsg.FAIL);
            response.setDesc("客户端序列号获取失败!");
        } else {
            heartBeatService.startBeat();
            response.setFlag(BaseMsg.SUCCESS);
            response.setDesc("测试连通AIC成功!");
            response.setSerializableNum(serializableNum);
        }

        return response;
    }


    @Deprecated
    @PostMapping("/verify/activation")
    public VerifyMsg activateClient(@RequestBody VerifyMsg msg) {
        logger.info("get request.." + msg);
        String serializableNum = msg.getSerializableNum();
        logger.info("get serializableNum: " + serializableNum);

        //todo 验证序列号
        boolean valid = true;
        VerifyMsg response = new VerifyMsg();
        response.setFlag(valid ? BaseMsg.SUCCESS : BaseMsg.FAIL);
        response.setDesc(valid ? "激活成功" : "激活失败");
        return response;
    }
}
