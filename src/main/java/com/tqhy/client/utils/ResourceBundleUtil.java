package com.tqhy.client.utils;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Yiheng
 * @create 3/23/2020
 * @since 1.0.0
 */
public class ResourceBundleUtil {

    static Locale zhLocale = new Locale("zh");
    static Locale enLocale = new Locale("en");

    public static ResourceBundle getBundle() {
        String language = PropertyUtils.getProperty("language");
        if (StringUtils.isEmpty(language)) {
            return ResourceBundle.getBundle("static/i18n/strings", zhLocale);
        }

        Locale locale = "zh".equals(language) ? zhLocale : enLocale;
        return ResourceBundle.getBundle("static/i18n/strings", locale);
    }
}
