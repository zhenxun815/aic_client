package com.tqhy.client.utils;

import com.google.gson.Gson;
import com.tqhy.client.models.msg.server.ClientMsg;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Yiheng
 * @create 4/4/2019
 * @since 1.0.0
 */
public class GsonUtils {


    public static <T> Optional<T> parseJsonToObj(String json, Class<T> type) {
        T t = new Gson().fromJson(json, type);
        return Optional.ofNullable(t);
    }

    public static ClientMsg parseResponseToObj(ResponseBody responseBody) {
        ClientMsg<Object> defaultMsg = new ClientMsg<>();
        defaultMsg.setFlag(0);
        defaultMsg.setMsg(new ArrayList<String>());
        defaultMsg.setDesc("response parse error!");
        try {
            String jsonStr = responseBody.string();
            return parseJsonToObj(jsonStr, ClientMsg.class).orElse(defaultMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultMsg;
    }
}
