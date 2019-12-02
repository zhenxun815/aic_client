package com.tqhy.client.controllers;

import com.tqhy.client.network.Network;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.ImgUtils;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

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
    String imgStorePath = FileUtils.getAppPath() + "/capture.jpg";
    @FXML
    void initialize() {
        super.initialize(webView);

        ImgUtils.captureScreen(imgStorePath);
        loadPage(webView, Network.LOCAL_BASE_URL + "html/snapshot.html");

    }

    @GetMapping(value = "/viewImg")
    public void viewImg(HttpServletRequest req, HttpServletResponse res) {
        try {
            res.reset();
            OutputStream out = res.getOutputStream();
            res.setHeader("Content-Type", "image/jpeg");
            //logger.info("img rel path is: " + path);
            try {
                File file = new File(imgStorePath);
                if (file != null) {
                    FileInputStream fis = new FileInputStream(file);
                    @SuppressWarnings("resource")
                    BufferedInputStream buff = new BufferedInputStream(fis);
                    byte[] b = new byte[1024];
                    long k = 0;
                    // 开始循环下载
                    while (k < file.length()) {
                        int j = buff.read(b, 0, 1024);
                        k += j;
                        // 将b中的数据写到客户端的内存
                        out.write(b, 0, j);
                    }
                }
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            logger.error("加载图片失败！");
            e.printStackTrace();
        }
    }
}
