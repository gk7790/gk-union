package com.gk.common.constant;

import lombok.Getter;

import java.util.List;

/**
 * 常量
 *
 * @author Lowen
 */
public interface Constant {
    /**
     * 成功
     */
    int SUCCESS = 1;
    /**
     * 失败
     */
    int FAIL = 0;
    /**
     * OK
     */
    String OK = "OK";
    /**
     * 用户标识
     */
    String USER_KEY = "userId";
    /**
     * admin
     */
    String ADMIN = "ADMIN";
    /**
     * organization
     */
    String ORG = "ORG";
    /**
     * client
     */
    String CLIENT = "CLIENT";
    /**
     * client
     */
    String CLIENT_APP = "client_app";
    /**
     * 菜单根节点标识
     */
    Long MENU_ROOT = 0L;
    /**
     * 部门根节点标识
     */
    Long DEPT_ROOT = 0L;
    /**
     *  升序
     */
    String ASC = "asc";
    /**
     * 降序
     */
    String DESC = "desc";
    /**
     * 创建时间字段名
     */
    String CREATED_AT = "created_at";

    /**
     * 数据权限过滤
     */
    String SQL_FILTER = "sqlFilter";
    /**
     * 当前页码
     */
    String PAGE = "page";
    /**
     * 每页显示记录数
     */
    String LIMIT = "pageSize";
    /**
     * 排序字段
     */
    String ORDER_FIELD = "orderField";
    /**
     * 排序方式
     */
    String ORDER = "order";
    /**
     * token header
     */
    String TOKEN_HEADER = "token";
    /**
     * authorization header
     */
    String AUTHORIZATION_HEADER = "authorization";
    /**
     * 云存储配置KEY
     */
    String CLOUD_STORAGE_CONFIG_KEY = "CLOUD_STORAGE_CONFIG_KEY";

    /**
     * 代码生成参数KEY
     */
    String DEV_TOOLS_PARAM_KEY = "DEV_TOOLS_PARAM_KEY";

    /**
     * 国际化语言key
     */
    String SYS_I18N_PARAMS_KEY = "SYS_I18N_PARAMS_KEY";

    /**
     * 国际化语言类型key
     */
    String SYS_I18N_TYPE_KEY = "SYS_I18N_TYPE_KEY";
    /**
     * 用户范围
     */
    String SYS_USER_SCOPE = "SYS_USER_SCOPE";
    /**
     * 用户范围
     */
    String SYS_DOMAIN_KEY = "SYS_DOMAIN_KEY";
    /**
     * Redis 缓存时间
     */
    String REDIS_EXPIRE_KEY = "REDIS_EXPIRE_KEY";

    /**
     * 邮件配置KEY
     */
    String MAIL_CONFIG_KEY = "MAIL_CONFIG_KEY";

    /**
     * 调查配置
     */
    String SURVEY_CALLBACK_CONFIG_KEY = "SURVEY_CALLBACK_CONFIG_KEY";

    /**
     * 会员任务奖励KEY
     */
    String MEMBER_TASK_REWARD_KEY = "MEMBER_TASK_REWARD_KEY";

    /**
     * 调查操作配置
     */
    String SURVEY_OPERATE_CONFIG_KEY = "SURVEY_OPERATE_CONFIG_KEY";

    /**
     * 调查操作配置
     */
    String MEMEBER_CASH_POINT_KEY = "MEMEBER_CASH_POINT_KEY";

    /**
     * 调查订单通知配置
     */
    String SURVEY_ORDER_NOTICE_CONFIG = "SURVEY_ORDER_NOTICE_CONFIG";
}