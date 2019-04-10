package com.tqhy.client.network;


import com.tqhy.client.network.api.AicApi;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yiheng
 * @create 2019/3/21
 * @since 1.0.0
 */
public class Network {
    private static AicApi aicApi;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private static Converter.Factory gsonConverterFactory = GsonConverterFactory.create();
    private static CallAdapter.Factory rxJavaCallAdapterFactory = RxJava2CallAdapterFactory.create();

    public static final String TEST_URL = "http://baidu.com/";
    public static String SERVER_IP;
    public static String TOKEN;
    public static String BASE_URL = "http://192.168.1.129:8080/";
    //public static String BASE_URL = "http://localhost:8764/";
    private static Logger logger = LoggerFactory.getLogger(Network.class);

    /**
     * 获取AIHelperApi对象
     *
     * @return
     */
    public static AicApi getAicApi() {

        logger.info("into getAicApi..base url: " + BASE_URL);
        if (null == aicApi) {
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(BASE_URL)
                    .addConverterFactory(gsonConverterFactory)
                    .addCallAdapterFactory(rxJavaCallAdapterFactory)
                    .build();
            aicApi = retrofit.create(AicApi.class);
        }
        return aicApi;
    }

    public static void setBaseUrl(String ip) {
        BASE_URL = "http://" + ip + ":8080/";
        aicApi = null;
    }

}
