package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PayOrderDao extends BaseDao<PayOrderEntity> {
    @Select("""
            SELECT id, tenant_id, merchant_id, merchant_no, merchant_app_id, app_id,
                   pay_order_no, merchant_order_no, idempotency_key, request_id, order_source,
                   country_code, currency, method_code, amount, paid_amount, merchant_fee_amount,
                   merchant_fee_rule_id, psp_fee_amount, psp_fee_rule_id, settle_amount,
                   subject, description, client_ip, notify_url, return_url, merchant_notify_status,
                   merchant_notify_at, merchant_notify_task_id, status, status_reason, expire_at,
                   paid_at, closed_at, failed_at, payment_plan_catalog_id, payment_plan_version,
                   payment_plan_bucket_id, payment_plan_route_option_id, route_rule_id,
                   route_group_id, route_channel_id, psp_id, psp_code, psp_method_id,
                   psp_method_code, psp_account_id, psp_account_no, psp_request_no,
                   psp_order_no, psp_status, psp_raw_status, psp_pay_url, next_query_at,
                   query_count, submitted_at, ledger_journal_no, settle_status,
                   settle_release_at, settle_at, settle_journal_no, outbox_event_id,
                   version, remark, updated_by, updated_at, created_by, created_at
            FROM pay_order
            WHERE tenant_id = #{tenantId}
              AND merchant_id = #{merchantId}
              AND merchant_order_no = #{merchantOrderNo}
            LIMIT 1
            """)
    PayOrderEntity selectOpenApiByMerchantOrderNo(@Param("tenantId") Long tenantId,
                                                  @Param("merchantId") Long merchantId,
                                                  @Param("merchantOrderNo") String merchantOrderNo);

    @Select("""
            SELECT id, tenant_id, merchant_id, merchant_no, merchant_app_id, app_id,
                   pay_order_no, merchant_order_no, idempotency_key, request_id, order_source,
                   country_code, currency, method_code, amount, paid_amount, merchant_fee_amount,
                   merchant_fee_rule_id, psp_fee_amount, psp_fee_rule_id, settle_amount,
                   subject, description, client_ip, notify_url, return_url, merchant_notify_status,
                   merchant_notify_at, merchant_notify_task_id, status, status_reason, expire_at,
                   paid_at, closed_at, failed_at, payment_plan_catalog_id, payment_plan_version,
                   payment_plan_bucket_id, payment_plan_route_option_id, route_rule_id,
                   route_group_id, route_channel_id, psp_id, psp_code, psp_method_id,
                   psp_method_code, psp_account_id, psp_account_no, psp_request_no,
                   psp_order_no, psp_status, psp_raw_status, psp_pay_url, next_query_at,
                   query_count, submitted_at, ledger_journal_no, settle_status,
                   settle_release_at, settle_at, settle_journal_no, outbox_event_id,
                   version, remark, updated_by, updated_at, created_by, created_at
            FROM pay_order
            WHERE tenant_id = #{tenantId}
              AND merchant_id = #{merchantId}
              AND pay_order_no = #{payOrderNo}
            LIMIT 1
            """)
    PayOrderEntity selectOpenApiByPayOrderNo(@Param("tenantId") Long tenantId,
                                             @Param("merchantId") Long merchantId,
                                             @Param("payOrderNo") String payOrderNo);
}
