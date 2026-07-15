package com.gk.infra.config.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.gk.common.beans.CurrentUser;
import com.gk.common.constant.Constant;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.infra.config.dto.RuntimeParamDTO;
import com.gk.infra.config.dto.RuntimeParamUpdateDTO;
import com.gk.infra.config.service.RuntimeParamService;
import com.gk.infra.config.service.SysParamsService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuntimeParamServiceImpl implements RuntimeParamService {

    private final CurrentUser currentUser;
    private final SysParamsService sysParamsService;

    private static final List<RuntimeParamDefinition> DEFINITIONS = List.of(
            new RuntimeParamDefinition(
                    Constant.DOMAIN_CONFIG_KEY,
                    "sys:runtime-param:domain-config",
                    "域名配置",
                    "Platform domain config",
                    "{\"apiBaseUrl\":\"https://api.example.com\",\"adminBaseUrl\":\"https://admin.example.com\"}"
            ),
            new RuntimeParamDefinition(
                    Constant.TELEGRAM_BASE_CONFIG_KEY,
                    "sys:runtime-param:telegram-base",
                    "Telegram 基础配置",
                    "Telegram base config",
                    "{\"apiBaseUrl\":\"https://api.telegram.org\",\"bindCodeTtl\":10,\"webhookBaseUrl\":\"https://mqmq.vip.cpolar.cn\"}"
            )
    );

    private static final Map<String, RuntimeParamDefinition> DEFINITION_MAP = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(RuntimeParamDefinition::getParamCode, Function.identity()));

    @Override
    public List<RuntimeParamDTO> list() {
        return DEFINITIONS.stream()
                .filter(this::hasPermission)
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void update(String paramCode, RuntimeParamUpdateDTO dto) {
        RuntimeParamDefinition definition = getDefinition(paramCode);
        assertPermission(definition);

        if (dto == null || dto.getParamValue() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "paramValue cannot be empty");
        }

        String paramValue = normalizeJson(dto.getParamValue());
        sysParamsService.updateValueByCode(definition.getParamCode(), paramValue);
        sysParamsService.deleteCacheByCode(definition.getParamCode());
    }

    private RuntimeParamDTO toDTO(RuntimeParamDefinition definition) {
        String value = sysParamsService.getValue(definition.getParamCode());
        String json = StringUtils.isNotBlank(value) ? value : definition.getDefaultValue();

        RuntimeParamDTO dto = new RuntimeParamDTO();
        dto.setParamCode(definition.getParamCode());
        dto.setParamValue(parseJson(json));
        dto.setDefaultValue(parseJson(definition.getDefaultValue()));
        dto.setAuth(definition.getAuth());
        dto.setName(definition.getName());
        dto.setRemark(definition.getRemark());
        return dto;
    }

    private RuntimeParamDefinition getDefinition(String paramCode) {
        RuntimeParamDefinition definition = DEFINITION_MAP.get(StringUtils.trimToEmpty(paramCode));
        if (definition == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "runtime param is not allowed");
        }
        return definition;
    }

    private boolean hasPermission(RuntimeParamDefinition definition) {
        return currentUser.hasAnyAuth(definition.getAuth());
    }

    private void assertPermission(RuntimeParamDefinition definition) {
        if (!hasPermission(definition)) {
            throw new GkException(ErrorCode.FORBIDDEN, "permission denied");
        }
    }

    private Object parseJson(String value) {
        try {
            return JSON.parse(value);
        } catch (JSONException e) {
            throw new GkException(ErrorCode.JSON_FORMAT_ERROR, e);
        }
    }

    private String normalizeJson(Object value) {
        try {
            Object parsed = value instanceof String ? JSON.parse((String) value) : value;
            return JSON.toJSONString(parsed);
        } catch (JSONException e) {
            throw new GkException(ErrorCode.JSON_FORMAT_ERROR, e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class RuntimeParamDefinition {
        private final String paramCode;
        private final String auth;
        private final String name;
        private final String remark;
        private final String defaultValue;
    }
}
