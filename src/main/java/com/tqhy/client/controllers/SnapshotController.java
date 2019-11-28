package com.tqhy.client.controllers;

import com.tqhy.client.network.Network;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

/**
 * @author Yiheng
 * @create 11/26/2019
 * @since 1.0.0
 */
@Controller
public class SnapshotController extends BaseWebviewController {

    static Logger logger = LoggerFactory.getLogger(SnapshotController.class);

    @FXML
    private WebView webView;

    @FXML
    void initialize() {
        super.initialize(webView);
        loadPage(webView, Network.LOCAL_BASE_URL + "html/snapshot.html");

    }
}
