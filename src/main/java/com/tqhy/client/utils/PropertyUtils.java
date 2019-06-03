package com.tqhy.client.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author poorguy
 * @version 0.0.1
 * @E-mail 494939649@qq.com
 * @created 2019/5/8 11:33
 * @description 需要获取配置文件的绝对路径，对配置文件的修改才会持久化
 */
public class PropertyUtils {
    public static void setUserName(String name){
        File file = new File("config.properties");
        String path=file.getAbsolutePath();
        //这里自动获取的绝对路径不对，要用下面一行做一下修改
        path=path.replace("config.properties","src\\main\\resources\\config.properties");
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(path));
            FileOutputStream fos = new FileOutputStream(path);
            properties.setProperty("username", name);
            properties.store(fos,"update the username");//配置文件中可以生成修改日志，可以没有
            fos.close();
        } catch (IOException e) {
            System.out.println("can't load properties file");
            e.printStackTrace();
        }
    }
    public static String getUserName() throws IOException {
        File file = new File("config.properties");
        String path=file.getAbsolutePath();
        //这里自动获取的绝对路径不对，要用下面一行做一下修改
        path=path.replace("config.properties","src\\main\\resources\\config.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(path));

        String username=properties.getProperty("username");
        return username;
    }
    public static void main(String[] args){
        try {
            System.out.println(getUserName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
