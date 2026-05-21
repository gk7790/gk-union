package com.gk.common.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataMap extends HashMap<String, Object> {

    public String getStr(Object key) {
        return MapUtil.getStr(this, key);
    }

    public String getStr(Object key, String defaultValue) {
        return MapUtil.getStr(this, key, defaultValue);
    }

    public Long getLong(Object key) {
        return getLong(key, 0L);
    }

    public Long getLong(Object key, Long defaultValue) {
        return MapUtil.getLong(this, key, defaultValue);
    }

    public int getInt(Object key) {
        return getInt(key, 0);
    }

    public int getInt(Object key, Integer defaultValue) {
        return MapUtil.getInt(this, key, defaultValue);
    }

    public Boolean getBool(Object key) {
        return MapUtil.getBool(this, key);
    }

    public Boolean getBool(Object key, Boolean defaultValue) {
        return MapUtil.getBool(this, key, defaultValue);
    }

    public Double getDouble(Object key) {
        return MapUtil.getDouble(this, key, 0.0);
    }

    public Date getDate(String key, String format) {
        String val = getStr(key);
        if (StrUtil.isEmpty(val)) return null;
        try {
            return DateUtil.parse(val, format);
        } catch (Exception e) {
            return null;
        }
    }

    public Date getDate(String key, Date defaultValue) {
        return MapUtil.getDate(this, key, defaultValue);
    }

    public DataMap getMap(String key) {
        Object value = this.get(key);
        if (value == null) {
            return new DataMap();
        }
        if (value instanceof DataMap im) {
            return im;
        }

        // 普通 Map，直接复制到新的 ItemMap
        if (value instanceof Map<?, ?> map) {
            DataMap itemMap = new DataMap();
            map.forEach((k, v) -> itemMap.put(String.valueOf(k), v));
            return itemMap;
        }

        // JSON 字符串，尝试解析
        if (value instanceof String str) {
            if (StrUtil.isBlank(str)) {
                return new DataMap();
            }
            try {
                // 使用 TypeReference 避免泛型警告
                Map<String, Object> parsed = JSONUtil.toBean(str, new TypeReference<>() {}, false);
                if (parsed == null) {
                    return new DataMap();
                }
                DataMap itemMap = new DataMap();
                itemMap.putAll(parsed);
                return itemMap;
            } catch (Exception e) {
                // 解析失败，返回空 Map，保证容错
                return new DataMap();
            }
        }
        // 其他类型一律兜底
        return new DataMap();
    }

    public <T> List<T> getList(Object key, Class<T> clazz) {
        return getList(key, clazz, Collections.emptyList());
    }

    public <T> List<T> getList(Object key, Class<T> clazz, List<T> defaultList) {
        Object value = this.get(key);
        if (value == null) {
            return defaultList;
        }

        // 1. 如果是字符串（可能是逗号分隔）
        if (value instanceof String) {
            String str = (String) value;
            if (StrUtil.isBlank(str)) {
                return Collections.emptyList();
            }
            return Convert.toList(clazz, StrUtil.split(str, ","));
        }

        // 2. 其它情况直接交给 Hutool Convert
        List<T> result = Convert.toList(clazz, value);
        return CollUtil.isEmpty(result) ? defaultList : result;
    }

    /**
     * 获取合并
     * @param keys key 集合
     * @return 返回合并值
     */
    public String getMerge(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String val = getStr(key);
            sb.append(val).append("|");
        }
        return sb.toString();
    }

    public LocalDateTime getLocalDateTime(String key) {
        return getLocalDateTime(key, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 转成 LocalDateTime
     * @param key 键
     * @return 返回合并值
     */
    public LocalDateTime getLocalDateTime(String key, String format) {
        Object val = this.get(key);
        if (val == null) {
            return null;
        }


        // 已经是 LocalDateTime 类型
        if (val instanceof LocalDateTime) {
            return (LocalDateTime) val;
        }

        // 是 Date 类型，转换为 LocalDateTime
        if (val instanceof Date) {
            return ((Date) val).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        // 是数字类型（可能是时间戳）
        if (val instanceof Number) {
            long timestamp = ((Number) val).longValue();
            // 判断是秒还是毫秒
            if (String.valueOf(timestamp).length() == 10) {
                timestamp *= 1000L;
            }
            return Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        // 是字符串类型
        String strVal = val.toString().trim();
        if (strVal.isEmpty()) return null;
        if (StrUtil.isNumeric(strVal)) {
            long timestamp = Long.parseLong(strVal);
            if (strVal.length() == 10) timestamp *= 1000L;
            return Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        if (StrUtil.isNotBlank(format)) {
            return LocalDateTime.parse(strVal, DateTimeFormatter.ofPattern(format));
        }

        return null;
    }

    /**
     * 校验 key的值 是否存在
     * @param key key
     * @return 布尔值
     */
    public boolean isValueNull(String key) {
        Object o = get(key);
        if (o instanceof String) {
            return StrUtil.isNotBlank((String) o);
        }
        return ObjectUtil.isNotEmpty(o);
    }

}
