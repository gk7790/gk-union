package com.gk.common.annotation;

import com.gk.common.enums.DataScopeType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据范围注解，用于通用查询方法的数据隔离。
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /**
     * 数据范围类型。
     */
    DataScopeType value() default DataScopeType.AUTO;

    /**
     * 主表别名，例如 t、u、o。
     */
    String tableAlias() default "";

    /**
     * 租户字段名。
     */
    String tenantColumn() default "tenant_id";

    /**
     * 商户字段名。
     */
    String merchantColumn() default "merchant_id";

    /**
     * 部门字段名。
     */
    String deptColumn() default "dept_id";

    /**
     * 用户字段名，通常是 created_by。
     */
    String userColumn() default "created_by";

    /**
     * 平台主体是否跳过租户/商户过滤。
     */
    boolean platformBypass() default true;

    /**
     * 缺少必要上下文时是否抛异常。
     */
    boolean strict() default true;
}