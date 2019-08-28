package com.tqhy.client.controllers;

import com.tqhy.client.ClientApplication;
import com.tqhy.client.config.Constants;
import com.tqhy.client.models.entity.Case;
import com.tqhy.client.models.entity.Model;
import com.tqhy.client.network.Network;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.NetworkUtils;
import com.tqhy.client.utils.StringUtils;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
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

    Case caseEntity;
    List<Model> models;

    @FXML
    void initialize() {
        logger.info("read model init...");
        super.initialize(webView);
        String caseId = caseEntity.getId();
        String modelIds = StringUtils.join(models, Constants.VALUE_SPLITTER, model -> model.getId());
        logger.info("init case id is {}, model size {}", caseId, models.size());

        HashMap<String, String> params = new HashMap<>();
        params.put("caseId", caseId);
        params.put("modelsId", modelIds);
        String readModelUrl = NetworkUtils.createUrl(Network.SERVER_BASE_URL, "/ai/caseimg/aiCaseImgIndex", params);

        loadPage(webView, readModelUrl);
        //webView.getEngine().load("https://www.baidu.com");
    }

    public void show(Case caseEntity, List<Model> models) {
        logger.info("read model show...case id is {}, model size {}", caseEntity.getId(), models.size());
        this.caseEntity = caseEntity;
        this.models = models;
        FXMLUtils.loadWindow(ClientApplication.stage, "/static/fxml/read_model.fxml", true);
        ClientApplication.stage.setIconified(false);
    }
}
