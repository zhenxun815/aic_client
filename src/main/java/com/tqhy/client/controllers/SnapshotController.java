package com.tqhy.client.controllers;

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
}
