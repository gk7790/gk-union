package com.gk.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class MsgUtils {
    private static MessageSource messageSource;
    static {
        if (SpringContextUtils.applicationContext != null) {
            messageSource = (MessageSource)SpringContextUtils.getBean("messageSource");
        }
    }

    public static String getMessage(int code){
        return getMessage(code, new String[0]);
    }

    public static String getMessage(int code, String... params){
        if (messageSource == null) {
            return "Failed to initialize the system message source";
        }
        if (params == null || params.length == 0) {
            params = new String[]{""};
        }
        return messageSource.getMessage(code+"", params, LocaleContextHolder.getLocale());
    }
}