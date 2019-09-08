package com.tqhy.client.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tqhy.client.ClientApplication;
import com.tqhy.client.models.entity.Case;
import com.tqhy.client.models.entity.Model;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.server.ModelMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.utils.NetworkUtils;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

/**
 * @author Yiheng
 * @create 9/4/2019
 * @since 1.0.0
 */
@Controller
public class ChooseModelController {

    Logger logger = LoggerFactory.getLogger(ChooseModelController.class);
    @FXML
    public VBox base_pane;

    @FXML
    private WebView webView;
    @Autowired
    ReadModelController readModelController;
    @Autowired
    LandingController landingController;

    private ModelMsg<Model> modelMsg;
    private ModelMsg<Case> caseMsg;

    @FXML
    void initialize() {
        base_pane.setMinWidth(400D);
        base_pane.setMinHeight(500D);
        //FXMLUtils.center2Display(base_pane);
        loadPage("html/model_choose.html");
    }

    void loadPage(String url) {
        //url为空则加载默认页面:测试连接页面
        String defaultUrl = Network.LOCAL_BASE_URL + url;
        webView.getEngine()
               .load(defaultUrl);
    }

    @GetMapping("cancel")
    public void cancel() {
        logger.info("cancel...");
        this.modelMsg = null;
        this.caseMsg = null;
        Platform.runLater(() -> {
            ClientApplication.chooseModelStage.close();
        });
    }


    @GetMapping("show/{case}/{models}")
    @ResponseBody
    public String getShow(@PathVariable("case") String caseId, @PathVariable("models") String modelIds) {
        logger.info("request case {}, models {}", caseId, modelIds);
        cancel();
        HashMap<String, String> params = new HashMap<>();
        params.put("caseId", caseId);
        params.put("modelsId", modelIds);
        String readModelUrl = NetworkUtils.createUrl(Network.SERVER_BASE_URL, "/caseimg/aiCaseImgIndex", params);
        logger.info("read model url is {}", readModelUrl);
        Platform.runLater(() -> {
            //readModelController.showReadModel(caseId, modelIds);
            landingController.showPage(readModelUrl);
            ClientApplication.stage.setIconified(false);
        });
        return readModelUrl;
    }

    @GetMapping("cases/{patientId}")
    @ResponseBody
    public ModelMsg<Case> getCaseList(@PathVariable String patientId) {
        Network.getAicApi()
               .searchCase(patientId)
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .doOnError(err -> {
                   this.caseMsg = new ModelMsg<>();
                   this.caseMsg.setFlag(BaseMsg.FAIL);
                   logger.error("get models failed!", err);
               })
               .blockingSubscribe(responseBody -> {
                   String json = responseBody.string();
                   logger.info("get all cases res is {}", json);
                   ModelMsg<Case> msg = new Gson().fromJson(json, new TypeToken<ModelMsg<Case>>() {
                   }.getType());
                   logger.info("client msg flag is {}", msg.getFlag());
                   if (BaseMsg.SUCCESS == msg.getFlag()) {
                       this.caseMsg = msg;
                   }
               });
        return caseMsg;
    }

    @GetMapping("/models")
    @ResponseBody
    public ModelMsg<Model> getModelList() {
        Network.getAicApi()
               .getAllModels()
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .doOnError(err -> {
                   this.modelMsg = new ModelMsg<>();
                   this.modelMsg.setFlag(BaseMsg.FAIL);
                   logger.error("get models failed!", err);
               })
               .blockingSubscribe(responseBody -> {
                   String json = responseBody.string();
                   logger.info("get all models res is {}", json);
                   ModelMsg<Model> modelMsg = new Gson().fromJson(json, new TypeToken<ModelMsg<Model>>() {
                   }.getType());
                   logger.info("client msg flag is {}", modelMsg.getFlag());
                   if (BaseMsg.SUCCESS == modelMsg.getFlag()) {
                       this.modelMsg = modelMsg;
                   }
               });

        return this.modelMsg;
    }
}
