package com.tqhy.client.service;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author Yiheng
 * @create 3/23/2019
 * @since 1.0.0
 */
@Service
public class HeartBeatService {

    Logger logger = LoggerFactory.getLogger(HeartBeatService.class);

    private static String status;

    private static final String CMD_STOP_BEAT = "stop";

    private static final String CMD_STOP_CONTINUE = "continue";

    public void startBeat() {
        status = CMD_STOP_CONTINUE;
        Observable.interval(1000, TimeUnit.MILLISECONDS)
                  .takeWhile(beatTimes -> CMD_STOP_CONTINUE.equals(status))
                  .map(beatTimes -> {
                           logger.info("current status is: " + status);
                           //todo 请求后台,获取心跳返回
                           String cmd = "";
                           return cmd;
                       }
                  )
                  .observeOn(Schedulers.trampoline())
                  .subscribeOn(Schedulers.trampoline())
                  .subscribe(cmd -> {
                      logger.info("subscribe: " + cmd);
                      if ("youxiao".equals(cmd)) {

                          //logger.info(".dll caller get: " + str);
                          status = CMD_STOP_CONTINUE;
                      } else {
                          status = CMD_STOP_BEAT;
                          //todo webview跳转到登录页面
                      }


                  });
    }
}
