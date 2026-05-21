package com.gk.common.annotation;

import java.lang.annotation.*;

/**
 * 数据过滤注解
 *
 * @author Lowen
 */
@Inherited
@Documented
@Target(ElementType.METHOD)  // 该注解应用在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时有效
public @interface DataFilter {
    /**
     * 表的别名
     */
    String tableAlias() default "";

    /**
     * 用户ID
     */
    String userId() default "created_by";

    /**
     * 部门ID
     */
    String deptId() default "dept_id";

    boolean verifyUser() default true;}