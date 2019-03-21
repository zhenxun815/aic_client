package com.tqhy.client.controllers;

import com.tqhy.client.network.app.JavaAppBase;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yiheng
 * @create 3/19/2019
 * @since 1.0.0
 */
public class BaseWebviewController {

    Logger logger = LoggerFactory.getLogger(BaseWebviewController.class);

    void engineBindApp(WebEngine engine, JavaAppBase javaApp) {
        engine.getLoadWorker()
              .stateProperty()
              .addListener((ov, oldState, newState) -> {
                 // logger.info("old state: " + oldState + " ,new state: " + newState);
                  if (Worker.State.FAILED == newState) {
                      JSObject window = (JSObject) engine.executeScript("window");
                      window.setMember("tqClient", javaApp);
                      engine.reload();
                  } else if (Worker.State.SUCCEEDED == newState) {
                      JSObject window = (JSObject) engine.executeScript("window");
                      window.setMember("tqClient", javaApp);
                  }
              });
    }
}
