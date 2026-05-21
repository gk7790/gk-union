package com.gk.common.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.*;

/**
 * 转换工具类
 *
 * @author Lowen
 */
@Slf4j
public class ConvertUtils {

    public static <T> T sourceToTarget(Object source, Class<T> target){
        if(source == null){
            return null;
        }
        if (ClassUtil.isBasicType(target) || ClassUtil.isSimpleValueType(target)) {
            return Convert.convert(target, source);
        }
        if (target.isAssignableFrom(source.getClass())) {
            return (T) source;
        }
        try {
            T targetObject = target.getDeclaredConstructor().newInstance();
            if (source instanceof Map) {
                // 如果是 SurveyMap 或者普通 Map，直接用 BeanUtil 填充
                BeanUtil.fillBeanWithMap((Map<?, ?>) source, targetObject, false);
            } else {
                // 普通 Bean -> Bean
                BeanUtils.copyProperties(source, targetObject);
            }
            return targetObject;
        } catch (Exception e) {
            log.error("convert error ", e);
        }
        return null;
    }

    public static <T> List<T> sourceToTarget(Collection<?> sourceList, Class<T> target){
        if(sourceList == null){
            return null;
        }

        List<T> targetList = new ArrayList<>(sourceList.size());
        try {
            for(Object source : sourceList){
                T targetObject = target.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(source, targetObject);
                targetList.add(targetObject);
            }
        }catch (Exception e){
            log.error("convert error ", e);
        }
        return targetList;
    }

    public static <T> Set<T> sourceToSet(Collection<?> sourceList, Class<T> target){
        if (sourceList == null) {
            return Collections.emptySet(); // 返回空 Set，避免 null
        }
        Set<T> targetList = new HashSet<>(sourceList.size());
        try {
            for(Object source : sourceList){
                if (target == String.class || target == Integer.class || target == Long.class) {
                    // 处理 String 类型，直接转换
                    targetList.add((T) source);
                } else {
                    T targetObject = target.getDeclaredConstructor().newInstance();
                    BeanUtils.copyProperties(source, targetObject);
                    targetList.add(targetObject);
                }
            }
        }catch (Exception e){
            log.error("convert error ", e);
        }
        return targetList;
    }
}