package com.tqhy.client.service;

import com.google.gson.Gson;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
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

    private static final String CMD_CONTINUE_BEAT = "continue";

    public void startBeat(String token) {
        status = CMD_CONTINUE_BEAT;
        Observable.interval(5000, TimeUnit.MILLISECONDS)
                  .takeWhile(beatTimes -> CMD_CONTINUE_BEAT.equals(status))
                  .observeOn(Schedulers.trampoline())
                  .subscribeOn(Schedulers.io())
                  .subscribe(aLong -> {
                      logger.info("heartBeating... ");

                      Network.getAicApi()
                             .heartbeat(token)
                             .observeOn(Schedulers.io())
                             .subscribeOn(Schedulers.trampoline())
                             .subscribe(responseBody -> {
                                 String json = responseBody.string();
                                 logger.info("heart beat json is: " + json);
                                 ClientMsg clientMsg = new Gson().fromJson(json, ClientMsg.class);
                                 Integer flag = clientMsg.getFlag();
                                 if (1 == flag) {
                                     logger.info("heart beat continue");
                                     status = CMD_CONTINUE_BEAT;
                                 } else if (203 == flag) {
                                     logger.info("heart beat stop");
                                     status = CMD_STOP_BEAT;
                                     //todo webview跳转到登录页面
                                 }
                             });
                  });
    }
}
