package com.tqhy.client.utils;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yiheng
 * @create 3/19/2019
 * @since 1.0.0
 */
public class NetworkUtils {

    static Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * 将本地路径转为URL对象
     *
     * @param url
     * @return
     */
    public static String toExternalForm(String url) {
        System.out.println("url is: " + url);
        URL resource = NetworkUtils.class.getResource(url);

        return null == resource ? null : resource.toExternalForm();
    }

    /**
     * 获取本地mac地址
     *
     * @return
     */
    public static String getPhysicalAddress() {
        try {
            InetAddress ia = InetAddress.getLocalHost();
            byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
            //System.out.println("mac数组长度：" + mac.length);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    sb.append("-");
                }
                //字节转换为整数
                int temp = mac[i] & 0xff;
                String str = Integer.toHexString(temp);
                // logger.info("每8位:" + str);
                if (str.length() == 1) {
                    sb.append("0" + str);
                } else {
                    sb.append(str);
                }
            }
            logger.info("本机MAC地址:" + sb.toString().toUpperCase());
            return sb.toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取本地ip
     *
     * @return 本地ip字符串
     */
    public static String getLocalIp() {
        String ip = null;
        try {
            byte[] addr = InetAddress.getLocalHost().getAddress();
            ip = (addr[0] & 0xff) + "." + (addr[1] & 0xff) + "." + (addr[2] & 0xff) + "." + (addr[3] & 0xff);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
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
}
