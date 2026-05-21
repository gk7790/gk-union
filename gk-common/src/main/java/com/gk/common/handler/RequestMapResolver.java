package com.gk.common.handler;

import cn.hutool.core.util.StrUtil;
import com.gk.common.annotation.RequestMap;
import com.gk.common.tools.DataMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component
public class RequestMapResolver implements HandlerMethodArgumentResolver {

    private final List<String> _timeKey = List.of("startTime", "endTime");

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestMap.class) &&
                parameter.getParameterType().equals(DataMap.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        DataMap queryMap = new DataMap();
        if (request != null) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            parameterMap.forEach((key, values) -> {
                if (values != null && values.length == 1) {
                    try {
                        String value = StrUtil.trimToEmpty(values[0]);
                        if (_timeKey.contains(key) && StrUtil.isNumeric(value)) {
                            long ts = Long.parseLong(value);
                            // 10位：秒级时间戳 -> 毫秒
                            if (value.length() == 10) {
                                ts = ts * 1000;
                            }

                            LocalDateTime dateTime = Instant.ofEpochMilli(ts)
                                    .atZone(TimeZone.getDefault().toZoneId())
                                    .toLocalDateTime();
                            queryMap.put(key, dateTime);
                        } else {
                            queryMap.put(key, value);
                        }
                    } catch (Exception e) {
                        queryMap.put(key, values);
                    }
                } else {
                    queryMap.put(key, values);
                }
            });
        }
        return queryMap;
    }
}