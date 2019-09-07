package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.network.Network;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.NetworkUtils;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.HashMap;

/**
 * @author Yiheng
 * @create 8/22/2019
 * @since 1.0.0
 */
@Controller
public class ReadModelController extends BaseWebviewController {

    Logger logger = LoggerFactory.getLogger(ReadModelController.class);
    @FXML
    public BorderPane readModelPane;
    @FXML
    private WebView webView;

    String caseId;
    String modelIds;

    @FXML
    void initialize() {
        logger.info("read model init...");
        super.initialize(webView);
        showPage();

        //webView.getEngine().load("https://www.baidu.com");
    }

    private void showPage() {
        logger.info("init case id is {}, model ids {}", caseId, modelIds);

        HashMap<String, String> params = new HashMap<>();
        params.put("caseId", caseId);
        params.put("modelsId", modelIds);
        String readModelUrl = NetworkUtils.createUrl(Network.SERVER_BASE_URL, "/caseimg/aiCaseImgIndex", params);
        logger.info("load ai case ing index page...");
        loadPage(webView, readModelUrl);
    }

    public void showReadModel(String caseId, String modelIds) {
        this.caseId = caseId;
        this.modelIds = modelIds;
        logger.info("read model show...case id is {}, model ids {}", caseId, modelIds);
        logger.info("webview is null {}", null == webView);
        if (null == webView) {
            FXMLUtils.loadWindow(ClientApplication.stage, "/static/fxml/read_model.fxml", true);
        } else {
            showPage();
            ClientApplication.stage.setIconified(false);
        }

    }
}
