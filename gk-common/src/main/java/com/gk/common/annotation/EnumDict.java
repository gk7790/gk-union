package com.gk.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要通过统一枚举接口暴露给前端的枚举。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumDict {
    /**
     * 枚举字典编码，作为接口返回的 key。
     */
    String value();

    /**
     * 枚举字典名称，仅用于说明。
     */
    String name() default "";

    /**
     * 是否暴露到统一枚举接口。
     */
    boolean visible() default true;

    /**
     * 返回顺序，数值越小越靠前。
     */
    int order() default 0;
}
