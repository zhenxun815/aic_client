package com.tqhy.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * @author Yiheng
 * @create 3/20/2020
 * @since 1.0.0
 */
@Configuration
public class I18nConfig {
    private static Logger logger = LoggerFactory.getLogger(I18nConfig.class);

    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        logger.info("创建cookieLocaleResolver");

        LocaleResolver localeResolver = new LocaleResolver();
        localeResolver.setDefaultLocale(Locale.CHINA);
        logger.info("cookieLocaleResolver:");
        return localeResolver;
    }
}
