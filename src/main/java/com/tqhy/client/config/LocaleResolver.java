package com.tqhy.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * @author Yiheng
 * @create 3/20/2020
 * @since 1.0.0
 */
public class LocaleResolver extends AcceptHeaderLocaleResolver {

    Logger logger = LoggerFactory.getLogger(LocaleResolver.class);
    private Locale myLocal;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        //获取cookie数组
        Cookie[] cookies = request.getCookies();
        String lang = "";
        if (cookies != null) {
            //遍历cookie数组
            for (Cookie cookie : cookies) {
                if ("ClientLanguage".equals(cookie.getName())) {
                    lang = cookie.getValue();
                    logger.info("get client lang {}", lang);
                    break;
                }
            }
        }
        LocaleResolver localeResolver = (LocaleResolver) RequestContextUtils.getLocaleResolver(request);
        if (lang == null || "".equals(lang)) {
            myLocal = Locale.CHINA;
        } else {
            if (lang.equals("zh")) {
                myLocal = Locale.CHINA;
            } else if (lang.equals("en")) {
                myLocal = Locale.ENGLISH;
            }
        }
        return myLocal;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        myLocal = locale;
    }
}
