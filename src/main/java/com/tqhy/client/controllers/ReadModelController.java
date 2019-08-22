package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.utils.FXMLUtils;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.List;

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
    List<String> models;

    @FXML
    void initialize() {
        logger.info("read model init...");
        super.initialize(webView);
        String readModelUrl = "";
        logger.info("init case id is {}, model size {}", caseId, models.size());
        //loadPage(webView, Network.LOCAL_BASE_URL + readModelUrl);
        webView.getEngine().load("https://www.baidu.com");
    }

    public void show(String caseId, List<String> models) {
        logger.info("read model show...case id is {}, model size {}", caseId, models.size());
        this.caseId = caseId;
        this.models = models;
        FXMLUtils.loadWindow(ClientApplication.stage, "/static/fxml/read_model.fxml");
        ClientApplication.stage.setIconified(false);
    }
}
