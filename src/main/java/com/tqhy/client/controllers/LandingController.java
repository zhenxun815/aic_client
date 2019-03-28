package com.tqhy.client.controllers;

import com.google.gson.Gson;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.LandingMsg;
import com.tqhy.client.models.msg.local.VerifyMsg;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.service.HeartBeatService;
import com.tqhy.client.utils.NetworkUtils;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

import static com.tqhy.client.utils.NetworkUtils.createRequestParamMap;
import static io.reactivex.internal.operators.flowable.FlowableBlockingSubscribe.subscribe;

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
        HashMap<String, String> userMap = new HashMap<>();

        userMap.put("userName", userName);
        userMap.put("passWord", userPwd);

        Network.getAicApi()
               //.landing(createRequestParamMap(userMap))
               .landing(userName, userPwd)
               .map(body -> {
                   String json = body.string();
                   logger.info("landing response: " + json);
                   ClientMsg clientMsg = new Gson().fromJson(json, ClientMsg.class);
                   Integer flag = clientMsg.getFlag();
                   List<String> msgs = clientMsg.getMsg();

                   if (1 == flag && null != msgs && msgs.size() > 0) {
                       response.setFlag(BaseMsg.SUCCESS);
                       response.setDesc("测试连通AIC成功!");
                       response.setToken(msgs.get(0));
                   } else {
                       response.setFlag(BaseMsg.FAIL);
                       response.setDesc("客户端序列号获取失败!");
                   }
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
