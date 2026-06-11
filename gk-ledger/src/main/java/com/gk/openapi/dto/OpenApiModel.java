package com.gk.openapi.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 商户 Open API 模型标记: JSON 字段统一 snake_case, 不接受别名。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public @interface OpenApiModel {
}
