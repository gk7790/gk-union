package com.gk.common.constant;

/**
 * 常量
 *
 * @author Lowen
 */
@SuppressWarnings("unused")
public interface Constant {
    /**
     * 成功
     */
    int SUCCESS = 1;
    /**
     * 失败
     */
    int FAIL = 3;
    /**
     * 菜单根节点标识
     */
    Long MENU_ROOT = 0L;
    /**
     * 部门根节点标识
     */
    Long DEPT_ROOT = 0L;
    /**
     * 系统最小id,用于处理数据隐藏
     */
    Long MIN_SYS_ID = 10L;
    /**
     * 系统保留最大id
     */
    Long MAX_RESERVED_ID = 1000L;
    /**
     * 系统预置/模板租户ID
     */
    Long DEFAULT_TENANT_ID = 0L;
    /**
     * 平台机构租户ID
     */
    Long PLATFORM_TENANT_ID = 1L;
    /**
     * 超管角色标识
     */
    String ROLE_AUTH_SADMIN = "sadmin";
    /**
     * 平台管理员角色标识
     */
    String ROLE_AUTH_ADMIN = "admin";
    /**
     * OK
     */
    String OK = "OK";
    /**
     * 用户标识
     */
    String USER_KEY = "userId";
    /**
     * 沙箱测试环境
     */
    String SANDBOX = "SANDBOX";
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
     * 系统时区
     */
    String SYS_TIMEZONE_KEY = "SYS_TIMEZONE_KEY";
    /**
     * 用户范围
     */
    String SYS_USER_SCOPE_KEY = "SYS_USER_SCOPE_KEY";
    /**
     * 用户范围
     */
    String SYS_DOMAIN_KEY = "SYS_DOMAIN_KEY";
    /**
     * Redis 缓存时间
     */
    String REDIS_EXPIRE_KEY = "REDIS_EXPIRE_KEY";

    /**
     * Telegram 基础配置
     */
    String TELEGRAM_BASE_CONFIG_KEY = "TELEGRAM_BASE_CONFIG_KEY";

    /**
     * OpenAPI 运行配置
     */
    String GK_OPENAPI_CONFIG_KEY = "GK_OPENAPI_CONFIG_KEY";

    /**
     * PSP 回调配置
     */
    String PSP_CALLBACK_CONFIG_KEY = "PSP_CALLBACK_CONFIG_KEY";

    /**
     * 商户默认配置
     */
    String MERCHANT_DEFAULT_CONFIG_KEY = "MERCHANT_DEFAULT_CONFIG_KEY";

    /**
     * 商户通知配置
     */
    String MERCHANT_NOTIFY_CONFIG_KEY = "MERCHANT_NOTIFY_CONFIG_KEY";

    /**
     * PSP 主动查单配置
     */
    String PSP_QUERY_CONFIG_KEY = "PSP_QUERY_CONFIG_KEY";

    /**
     * 代付提交配置
     */
    String PAYOUT_SUBMIT_CONFIG_KEY = "PAYOUT_SUBMIT_CONFIG_KEY";

    /**
     * PSP balance config
     */
    String PSP_BALANCE_CONFIG_KEY = "PSP_BALANCE_CONFIG_KEY";

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
