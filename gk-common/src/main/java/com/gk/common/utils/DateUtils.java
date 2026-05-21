package com.gk.common.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 日期处理
 *
 * @author Lowen
 */
public class DateUtils {
    /** 时间格式(yyyy-MM-dd) */
    public final static String DATE_PATTERN = "yyyy-MM-dd";
    /** 时间格式(yyyy-MM-dd HH:mm:ss) */
    public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";



    /**
     * 日期解析
     * @return  返回Date
     */
    public static Date now(String timeZone) {
        TimeZone timeZone1 = TimeZone.getTimeZone(timeZone);
        return DateUtil.date(Calendar.getInstance(timeZone1));
    }

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     * @param date  日期
     * @return 返回yyyy-MM-dd格式日期
     */
    public static String format(Date date) {
        return format(date, DATE_PATTERN);
    }

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     * @param date  日期
     * @return 返回yyyy-MM-dd格式日期
     */
    public static String formatDateTime(Date date) {
        return format(date, DATE_TIME_PATTERN);
    }

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     * @param date  日期
     * @param pattern  格式，如：DateUtils.DATE_TIME_PATTERN
     * @return 返回yyyy-MM-dd格式日期
     */
    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }

    /**
     * 日期格式化
     * @param date 日期
     * @param pattern 日期字符格式
     * @param timeZone 时区 GMT+
     * @return
     */
    public static String format(Date date, String pattern, String timeZone) {
        if(date != null){
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            df.setTimeZone(TimeZone.getTimeZone(timeZone));
            return df.format(date);
        }
        return null;
    }

    public static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return DateUtil.toLocalDateTime(date).toLocalDate();
    }


    /**
     * 日期解析
     * @param date  日期
     * @param pattern  格式，如：DateUtils.DATE_TIME_PATTERN
     * @return 返回Date
     */
    public static Date parse(String date, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 日期解析
     * @param date  日期
     * @param pattern  格式，如：DateUtils.DATE_TIME_PATTERN
     * @return  返回Date
     */
    public static Date parse(String date, String pattern, String timeZone) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            df.setTimeZone(TimeZone.getTimeZone(timeZone));
            return df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> genDateRangeStr(Date startDate, Date endDate) {
        List<String> result = new ArrayList<>();
        DateTime current = DateUtil.beginOfDay(startDate);
        DateTime end = DateUtil.beginOfDay(endDate);
        while (!current.isAfter(end)) {
            result.add(DateUtil.format(current, DATE_PATTERN));
            current = DateUtil.offsetDay(current, 1);
        }
        return result;
    }

    public static long getTodayRemainingSeconds() {
        long now = System.currentTimeMillis();
        long end = DateUtil.endOfDay(new Date()).getTime();
        return (end - now) / 1000;
    }

    /**
     * 校验时间范围
     * @param start 开始时间
     * @param end 结束时间
     * @param time 当前时间
     * @return 如果在范围返回 true, 否则 fasle
     */
    public static boolean isInRange(LocalTime start, LocalTime end, LocalTime time) {
        if (start.equals(end)) {
            // 起止时间相同，认为全天都在范围内
            return true;
        }
        if (start.isBefore(end)) {
            // 正常时间段（不跨天）
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            // 跨天时间段，例如 22:00 到 06:00
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }

    public static long getSecondTimestamp(Date date) {
        if (ObjectUtils.isEmpty(date)) {
            return 0L;
        }
        return DateUtil.toInstant(date).getEpochSecond();
    }


    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " 毫秒";
        }
        long seconds = millis / 1000;
        long remainingMillis = millis % 1000;
        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" 天 ");
        if (hours > 0) sb.append(hours).append(" 小时 ");
        if (minutes > 0) sb.append(minutes).append(" 分钟 ");
        if (remainingSeconds > 0 || sb.length() == 0) sb.append(remainingSeconds).append(" 秒 ");
        if (remainingMillis > 0 || sb.length() == 0) sb.append(remainingMillis).append(" 毫秒");
        return sb.toString().trim();
    }

    public static LocalDateTime beginOfDay(LocalDateTime now) {
        return LocalDateTimeUtil.beginOfDay(now);
    }

    public static LocalDateTime endOfDay(LocalDateTime now) {
        return LocalDateTimeUtil.endOfDay(now, true);
    }

}
