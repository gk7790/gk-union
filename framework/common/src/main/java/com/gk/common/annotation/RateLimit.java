package com.gk.common.annotation;

import java.lang.annotation.*;

@Inherited
@Documented
@Target(ElementType.METHOD)  // 该注解应用在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时有效
public @interface RateLimit {
    /**
     * 限流的 key，可以根据方法参数动态生成 key
     */
    String key();
    /**
     * 前置key
     */
    String preKey() default "";

    /**
     * key 是否追加token
     * 默认key中增加token
     */
    boolean isToken() default true;
    /**
     * 限制的访问次数
     */
    int count() default 100;
    /**
     * 限制的时间窗口，单位：秒
     */
    int time() default 60;
    /**
     * 提示信息
     */
    String msg() default "";

    String keySpEL() default ""; // 动态 key 片段表达式（可选）
}
