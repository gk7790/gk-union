package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayoutOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PayoutOrderDao extends BaseDao<PayoutOrderEntity> {
    @Select("""
            SELECT id, tenant_id, merchant_id, merchant_no, merchant_app_id, app_id,
                   payout_order_no, merchant_order_no, idempotency_key, request_id, order_source,
                   country_code, currency, method_code, amount, merchant_fee_amount,
                   merchant_fee_rule_id, total_debit_amount, psp_fee_amount, psp_fee_rule_id,
                   payee_name, payee_account_no, payee_bank_code, payee_wallet_type,
                   payee_phone, payee_email, notify_url, merchant_notify_status,
                   merchant_notify_at, merchant_notify_task_id, status, status_reason,
                   submitted_at, completed_at, failed_at, cancelled_at, fail_code, fail_msg,
                   hold_no, freeze_journal_no, success_journal_no, release_journal_no,
                   payment_plan_catalog_id, payment_plan_version, payment_plan_bucket_id,
                   payment_plan_route_option_id, route_rule_id, route_group_id, route_channel_id,
                   psp_id, psp_code, psp_method_id, psp_method_code, psp_account_id,
                   psp_account_no, psp_request_no, psp_order_no, psp_status, psp_raw_status,
                   next_query_at, query_count, outbox_event_id, version, remark,
                   updated_by, updated_at, created_by, created_at
            FROM payout_order
            WHERE tenant_id = #{tenantId}
              AND merchant_id = #{merchantId}
              AND merchant_order_no = #{merchantOrderNo}
            LIMIT 1
            """)
    PayoutOrderEntity selectOpenApiByMerchantOrderNo(@Param("tenantId") Long tenantId,
                                                     @Param("merchantId") Long merchantId,
                                                     @Param("merchantOrderNo") String merchantOrderNo);

    @Select("""
            SELECT id, tenant_id, merchant_id, merchant_no, merchant_app_id, app_id,
                   payout_order_no, merchant_order_no, idempotency_key, request_id, order_source,
                   country_code, currency, method_code, amount, merchant_fee_amount,
                   merchant_fee_rule_id, total_debit_amount, psp_fee_amount, psp_fee_rule_id,
                   payee_name, payee_account_no, payee_bank_code, payee_wallet_type,
                   payee_phone, payee_email, notify_url, merchant_notify_status,
                   merchant_notify_at, merchant_notify_task_id, status, status_reason,
                   submitted_at, completed_at, failed_at, cancelled_at, fail_code, fail_msg,
                   hold_no, freeze_journal_no, success_journal_no, release_journal_no,
                   payment_plan_catalog_id, payment_plan_version, payment_plan_bucket_id,
                   payment_plan_route_option_id, route_rule_id, route_group_id, route_channel_id,
                   psp_id, psp_code, psp_method_id, psp_method_code, psp_account_id,
                   psp_account_no, psp_request_no, psp_order_no, psp_status, psp_raw_status,
                   next_query_at, query_count, outbox_event_id, version, remark,
                   updated_by, updated_at, created_by, created_at
            FROM payout_order
            WHERE tenant_id = #{tenantId}
              AND merchant_id = #{merchantId}
              AND payout_order_no = #{payoutOrderNo}
            LIMIT 1
            """)
    PayoutOrderEntity selectOpenApiByPayoutOrderNo(@Param("tenantId") Long tenantId,
                                                   @Param("merchantId") Long merchantId,
                                                   @Param("payoutOrderNo") String payoutOrderNo);
}
