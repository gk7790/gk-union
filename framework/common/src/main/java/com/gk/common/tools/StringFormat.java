package com.gk.common.tools;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringFormat {
    /**
     * 无序占位符替换（%s）
     * @param template 包含 %s 占位符的模板字符串
     * @param values 按顺序填充的参数值
     * @return 替换后的字符串
     */
    public static String formatStr(String template, Object... values) {
        return String.format(template, values);
    }

    /**
     * 无序占位符替换（{}）
     * @param template 包含 {} 占位符的模板字符串
     * @param values 按顺序填充的参数值
     * @return 替换后的字符串
     */
    public static String format(String template, Object... values) {
        for (Object value : values) {
            String valueStr = value == null ? "" : value.toString();
            template = template.replaceFirst("\\{}", Matcher.quoteReplacement(valueStr));
        }
        return template;
    }

    /**
     * 使用 MessageFormat.format(template, values)
     */
    public static String formatMsg(String template, Object... values) {
        return MessageFormat.format(template, values);
    }

    /**
     * 带键值占位符替换（{key}）
     * @param template 包含 {key} 占位符的模板字符串
     * @param valuesMap 键值对 Map，键为占位符名称，值为对应替换内容
     * @return 替换后的字符串
     */
    public static String formatWithKeys(String template, Map<String, Object> valuesMap) {
        if (ObjUtil.isEmpty(valuesMap)) {
            return template;
        }
        Pattern pattern = Pattern.compile("\\{(\\w+)}");
        Matcher matcher = pattern.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = MapUtil.getStr(valuesMap, key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 带键值占位符替换（{key}）
     * @param template 包含 {key} 占位符的模板字符串
     * @param jsonParams 键值对 Map，键为占位符名称，值为对应替换内容
     * @return 替换后的字符串
     */
    public static String formatWithKeys(String template, JSONObject jsonParams) {
        Pattern pattern = Pattern.compile("\\{(\\w+)}");
        Matcher matcher = pattern.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = jsonParams.getString(key);
            replacement = StrUtil.isEmpty(replacement) ? "" : replacement;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String remove(String template, String... values) {
        for (String value : values) {
            template = StringUtils.remove(template, value);
        }
        return StrUtil.trimToEmpty(template);
    }

    /**
     * 拆分字符
     */
    public static List<String> splitToList(String delimited, String separator) {
        if (delimited != null && separator != null) {
            String[] split = StringUtils.split(delimited, separator);
            return List.of(split);
        } else {
            return List.of();
        }
    }

    /**
     * 拆分字符
     */
    public static Set<String> splitToSet(String delimited, String separator) {
        if (delimited != null && separator != null) {
            String[] split = StringUtils.split(delimited, separator);
            return new HashSet<>(Arrays.asList(split));
        } else {
            return Set.of();
        }
    }

    /**
     * 替换字符
     */
    public static String replace(String result, Map<String, String> map) {
        if (ObjUtil.isEmpty(map)) {
            return result;
        }
        if (StrUtil.isNotEmpty(result)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (StrUtil.isNotEmpty(entry.getKey()) && StrUtil.contains(result, entry.getKey())) {
                    result = result.replace(entry.getKey(), (StrUtil.isNotEmpty(entry.getValue()) ? entry.getValue() : ""));
                }
            }
        }
        return result;
    }

    public static String replaceUri(String url, Map<String, String> mapParams) {
        String result = url;
        for (Map.Entry<String, String> entry : mapParams.entrySet()) {
            String paramName = entry.getKey();
            String placeholder = entry.getValue();

            // 匹配 ?param=xxx 或 &param=xxx
            Pattern pattern = Pattern.compile("([?&]" + Pattern.quote(paramName) + "=)([^&#]*)");
            Matcher matcher = pattern.matcher(result);

            if (matcher.find()) {
                // 如果参数存在 → 替换值
                result = matcher.replaceAll(matcher.group(1) + placeholder);
            }
        }
        return result;
    }

    public static String normalizeUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }

        // 拆 scheme 前缀
        int index = url.indexOf("://");
        String prefix = index > 0 ? url.substring(0, index + 3) : "";
        String pathAndQuery = index > 0 ? url.substring(index + 3) : url;

        // 1. 处理路径部分多余的 "//"
        int queryIndex = pathAndQuery.indexOf("?");
        String path = queryIndex >= 0 ? pathAndQuery.substring(0, queryIndex) : pathAndQuery;
        String query = queryIndex >= 0 ? pathAndQuery.substring(queryIndex) : "";

        path = path.replaceAll("/{2,}", "/");

        // 2. 处理 query 部分多余的 & 和 ?&
        if (!query.isEmpty()) {
            query = query
                    .replaceAll("\\?&+", "?")   //  ?&xx → ?xx
                    .replaceAll("&{2,}", "&")   //  &&xx → &xx
                    .replaceAll("&+$", "");     //  末尾多余的 & 删除
        }

        return prefix + path + query;
    }

    /**
     * 合并参数
     * @param delimiter 分割符
     * @param elements 元素
     * @return 结果
     */
    public static String join(String delimiter, CharSequence... elements) {
        return Arrays.stream(elements).filter(StrUtil::isNotEmpty).collect(Collectors.joining(delimiter));
    }

    /**
     * 合并参数
     * @param delimiter 分割符
     * @param elements 元素
     * @return 结果
     */
    public static String join(String delimiter, Collection<String> elements) {
        return elements.stream().filter(StrUtil::isNotEmpty).collect(Collectors.joining(delimiter));
    }


    /**
     * 忽略大小写,全部都要包含
     * @param str 字符串
     * @param testChars 字符集合
     * @return
     */
    public static boolean containsAllIgnoreCase(CharSequence str, CharSequence... testChars) {
        if (StrUtil.isNotBlank(str) && !ArrayUtil.isEmpty(testChars)) {
            CharSequence[] var2 = testChars;
            int var3 = testChars.length;
            for(int var4 = 0; var4 < var3; ++var4) {
                CharSequence testChar = var2[var4];
                if (!StrUtil.containsAnyIgnoreCase(str, testChar)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将数据转成 List<String>
     * @param value 数据
     * @return 返回 String 集合
     */
    public static List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();

        if (value == null) {
            return result;
        }

        if (value instanceof String str) {
            result.add(str);
            return result;
        }

        if (value instanceof JSONArray array) {
            for (Object item : array) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }

        throw new IllegalArgumentException("无法转换为字符串数组: " + value);
    }

}
