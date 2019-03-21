package com.tqhy.client.controllers;

import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.VerifyMsg;
import com.tqhy.client.utils.NetworkUtils;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @author Yiheng
 * @create 3/18/2019
 * @since 1.0.0
 */
@RestController
public class ActivatingController extends BaseWebviewController {

    static Logger logger = LoggerFactory.getLogger(ActivatingController.class);
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

    @Deprecated
    @PostMapping("/verify/connection")
    @ResponseBody
    public VerifyMsg connectAic(@RequestBody VerifyMsg msg) {
        logger.info("get request with server ip.." + msg.getServerIP());
        VerifyMsg response = new VerifyMsg();

        String physicalAddress = NetworkUtils.getPhysicalAddress();
        if (StringUtils.isEmpty(physicalAddress)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("Mac地址获取失败!");
            return response;
        }
        //todo 访问后台获取客户端序列号
        String serverIP = msg.getServerIP();
        Integer integer = new Integer(serverIP);

        String serializableNum = integer % 2 == 0 ? physicalAddress + new Date() : "";

        if (StringUtils.isEmpty(serializableNum)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("客户端序列号获取失败!");
        } else {
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
