package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_request_log")
public class MerchantRequestLogEntity extends BaseEntity {
    /** 租户ID */
    private Long tenantId;
    /** 商户ID */
    private Long merchantId;
    /** 商户号快照 */
    private String merchantNo;
    /** 商户应用ID */
    private Long merchantAppId;
    /** 商户应用ID快照 */
    private String appId;
    /** 请求日志号 */
    private String requestNo;
    /** 接口路径 */
    private String apiPath;
    /** 接口名称 */
    private String apiName;
    /** HTTP方法 */
    private String httpMethod;
    /** 客户端IP */
    private String clientIp;
    /** User-Agent */
    private String userAgent;
    /** 请求体哈希 */
    private String requestBodyHash;
    /** 请求体JSON，脱敏后保存 */
    private String requestBodyJson;
    /** 请求参数JSON，脱敏后保存 */
    private String requestParamsJson;
    /** 签名类型 */
    private String signType;
    /** 商户提交签名，脱敏后保存 */
    private String signValue;
    /** 验签结果: 0失败 1成功 */
    private Integer signValid;
    /** 商户提交时间戳 */
    private String timestampValue;
    /** 商户提交nonce */
    private String nonceValue;
    /** 业务类型 */
    private String bizType;
    /** 平台业务单号 */
    private String bizNo;
    /** 商户订单号 */
    private String merchantOrderNo;
    /** 响应码 */
    private String responseCode;
    /** 响应消息 */
    private String responseMessage;
    /** 响应体JSON，脱敏或摘要后保存 */
    private String responseBodyJson;
    /** 处理状态 */
    private String status;
    /** 错误码 */
    private String errorCode;
    /** 错误信息 */
    private String errorMessage;
    /** 处理耗时毫秒 */
    private Long costMs;
    /** 链路追踪ID */
    private String traceId;
}
