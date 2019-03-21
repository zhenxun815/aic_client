package com.tqhy.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;

/**
 * @author Yiheng
 * @create 3/19/2019
 * @since 1.0.0
 */
public class NetworkUtils {

    static Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * 将本地路径转为URL对象
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
}
