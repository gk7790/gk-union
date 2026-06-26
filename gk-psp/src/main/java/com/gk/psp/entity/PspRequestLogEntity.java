package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_request_log")
public class PspRequestLogEntity extends SimpleEntity {
    /** 租户ID */
    private Long tenantId;
    /** 平台商户ID */
    private Long merchantId;
    /** PSP ID */
    private Long pspId;
    /** PSP编码快照 */
    private String pspCode;
    /** 业务类型: PAY_ORDER/PAYOUT_ORDER/QUERY/REFUND*/
    private String bizType;
    /** 业务ID */
    private Long bizId;
    /** 业务编号 */
    private String bizNo;
    /** 平台请求编号 */
    private String requestNo;
    /** PSP请求编号 */
    private String pspRequestNo;
    /** PSP订单*/
    private String pspOrderNo;
    /** 请求URL */
    private String requestUrl;
    /** HTTP方法 */
    private String httpMethod;
    /** 请求头JSON，敏感字段需要脱*/
    private String requestHeadersJson;
    /** 请求体，敏感字段需要脱*/
    private String requestBody;
    /** HTTP响应状态码 */
    private Integer responseStatus;
    /** 响应体，敏感字段需要脱*/
    private String responseBody;
    /** 是否成功: 01*/
    private Integer success;
    /** 错误*/
    private String errorCode;
    /** 错误信息 */
    private String errorMsg;
    /** 耗时毫秒 */
    private Long costMs;
    /** 链路追踪ID */
    private String traceId;
}
