package com.tqhy.client.utils;
import sun.misc.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author poorguy
 * @version 0.0.1
 * @E-mail 494939649@qq.com
 * @created 2019/5/8 11:20
 * @description 加解密密码用
 */
public class EncryptUtils {
    public static String encrypt(String unencrypted) throws UnsupportedEncodingException {
        return new BASE64Encoder().encode(unencrypted.getBytes("utf-8"));
    }
    public static String decrypt(String encrypted) throws IOException {
        return new String(new BASE64Decoder().decodeBuffer(encrypted),"utf-8");
    }
}
