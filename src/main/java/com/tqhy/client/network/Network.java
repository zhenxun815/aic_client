package com.tqhy.client.network;


import com.tqhy.client.network.api.AiHelperApi;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private static AiHelperApi aiHelperApi;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private static Converter.Factory gsonConverterFactory = GsonConverterFactory.create();
    private static CallAdapter.Factory rxJavaCallAdapterFactory = RxJava2CallAdapterFactory.create();
    public static String currentId = "";


    public static final String TEST_URL = "http://baidu.com/";
    public static String IP;
    public static String BASE_URL = "http://192.168.1.129:8080/";
    private static Logger logger = LoggerFactory.getLogger(Network.class);

    /**
     * 获取AIHelperApi对象
     *
     * @return
     */
    public static AiHelperApi getAiHelperApi() {

        //logger.info("into getAiHelperApi..");
        if (null == aiHelperApi) {
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(BASE_URL)
                    .addConverterFactory(gsonConverterFactory)
                    .addCallAdapterFactory(rxJavaCallAdapterFactory)
                    .build();
            aiHelperApi = retrofit.create(AiHelperApi.class);
        }
        return aiHelperApi;
    }

    /**
     * 创建单参数请求,将字符串转换为{@link RequestBody}对象
     *
     * @param content
     * @return
     */
    public static RequestBody createRequestParam(String content) {
        if (content == null) {
            content = "";
        }
        RequestBody body = RequestBody.create(MediaType.parse("multipart/form-data"), content);
        return body;
    }

    /**
     * 创建多参数请求
     *
     * @param params
     * @return
     */
    public static Map<String, RequestBody> createRequestParamMap(Map<String, String> params) {
        HashMap<String, RequestBody> paramMap = new HashMap<>();
        params.forEach((k, v) -> {
            RequestBody requestParam = createRequestParam(v);
            paramMap.put(k, requestParam);
        });
        return paramMap;
    }


    /**
     * 根据待上传文件路径生成上传文件{@link MultipartBody.Part}对象
     *
     * @param filePath
     * @return
     */
    public static MultipartBody.Part createFilePart(String partName, String filePath) {
        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        return part;
    }

    /**
     * 根据待上传文件路径生成上传文件{@link MultipartBody.Part}对象
     *
     * @param uploadFileMap
     * @return
     */
    public static List<MultipartBody.Part> createMultiFilePart(Map<String, String> uploadFileMap) {
        List<MultipartBody.Part> multiParts = new ArrayList<>();
        uploadFileMap.forEach((partName, filePath) -> {
            MultipartBody.Part filePart = createFilePart(partName, filePath);
            multiParts.add(filePart);
        });

        return multiParts;
    }


    public static void setBaseUrl(String ip) {
        BASE_URL = "http://" + ip + ":8080/";
    }

}
