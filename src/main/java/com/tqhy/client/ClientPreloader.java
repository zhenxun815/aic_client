package com.tqhy.client;

import com.sun.javafx.application.LauncherImpl;
import com.tqhy.client.controllers.PreloaderController;
import com.tqhy.client.unique.AlreadyLockedException;
import com.tqhy.client.unique.JUnique;
import com.tqhy.client.utils.FXMLUtils;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.SystemUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Preloader;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yiheng
 * @create 4/15/2019
 * @since 1.0.0
 */
public class ClientPreloader extends Preloader {


    Logger logger = LoggerFactory.getLogger(ClientPreloader.class);

    private Stage preloaderStage;
    private PreloaderController preloaderController;
    private boolean preloaderFlag = false;


    public static void main(String[] args) {
        String appId = "TQHY-AIC-CLIENT";
        boolean alreadyRunning;
        try {
            JUnique.acquireLock(appId, message -> {
                System.out.println("get message: " + message);
                return null;
            });
            alreadyRunning = false;
        } catch (AlreadyLockedException e) {
            alreadyRunning = true;
        }

        if (alreadyRunning) {
            for (int i = 0; i < args.length; i++) {
                JUnique.sendMessage(appId, "call_window");
            }
        } else {
            LauncherImpl.launchApplication(ClientApplication.class, ClientPreloader.class, args);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
        initPreloaderStage(preloaderStage);
        this.preloaderController = FXMLUtils.loadPreloader(preloaderStage, "/static/fxml/preloader.fxml");

    }

    /**
     * 初始化动态库
     */
    private void initLibPath() {
        String arc = SystemUtils.getArc();
        logger.info("system arc is: " + arc);

        String dllToCopy = SystemUtils.SYS_ARC_64.equals(arc) ? "/bin/opencv_java_64bit.dll" : "/bin/opencv_java_32bit.dll";
        File destDll = FileUtils.getLocalFile("/", "opencv_java.dll");

        boolean copyResource = FileUtils.copyResource(dllToCopy, destDll.getAbsolutePath());
        if (!copyResource) {
            preloaderController.setPreloadMessage("初始化动态库失败...");
            preloaderStage.hide();
        }
    }

    /**
     * 初始化preloader窗口
     *
     * @param preloaderStage
     */
    private void initPreloaderStage(Stage preloaderStage) {
        preloaderStage.initStyle(StageStyle.TRANSPARENT);
        preloaderStage.setHeight(340);
        preloaderStage.setWidth(680);
        preloaderStage.setResizable(false);
        preloaderStage.setAlwaysOnTop(true);
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification notification) {
        switch (notification.getType()) {
            case BEFORE_LOAD:
                preloaderController.setPreloadProgress(0);
                logger.info("before load...");
                preloaderFlag = false;
                initLibPath();
                break;
            case BEFORE_INIT:
                AtomicInteger integer = new AtomicInteger();
                preloaderController.setPreloadMessage("资源加载中...");
                Observable.interval(500, TimeUnit.MILLISECONDS)
                          .map(aLong -> aLong)
                          .observeOn(Schedulers.trampoline())
                          .subscribeOn(Schedulers.trampoline())
                          .takeUntil(flag -> preloaderFlag)
                          .subscribe(type -> {
                              logger.info("interval...");
                              integer.addAndGet(10);
                              preloaderController.setPreloadProgress(integer.doubleValue() / 100);
                          });
                break;
            case BEFORE_START:
                preloaderFlag = true;
                preloaderController.setPreloadProgress(100D);
                preloaderController.setPreloadMessage("资源加载完毕...");
                preloaderStage.hide();
                logger.info("before start...");
                break;
        }

    }


    @Override
    public void handleProgressNotification(ProgressNotification info) {
        logger.info("handleProgressNotification: " + info.getProgress());
        preloaderController.setPreloadProgress(info.getProgress());
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        preloaderController.setPreloadMessage(info.getDetails());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
