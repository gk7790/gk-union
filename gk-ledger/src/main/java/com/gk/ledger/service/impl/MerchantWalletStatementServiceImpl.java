package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.ledger.dao.MerchantWalletStatementDao;
import com.gk.ledger.dto.MerchantWalletStatementDTO;
import com.gk.ledger.entity.MerchantWalletStatementEntity;
import com.gk.ledger.service.MerchantWalletStatementService;
import org.springframework.stereotype.Service;

@Service
public class MerchantWalletStatementServiceImpl extends CrudServiceImpl<MerchantWalletStatementDao, MerchantWalletStatementEntity, MerchantWalletStatementDTO>
        implements MerchantWalletStatementService {

    @Override
    public QueryWrapper<MerchantWalletStatementEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantWalletStatementEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long journalId = params.getLong("journalId", null);
        Long entryId = params.getLong("entryId", null);
        Long accountId = params.getLong("accountId", null);
        Long bizId = params.getLong("bizId", null);
        String statementNo = params.getStr("statementNo");
        String merchantNo = params.getStr("merchantNo");
        String journalNo = params.getStr("journalNo");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String merchantOrderNo = params.getStr("merchantOrderNo");
        String eventType = params.getStr("eventType");
        String accountNo = params.getStr("accountNo");
        String accountType = params.getStr("accountType");
        String currency = params.getStr("currency");
        String effectType = params.getStr("effectType");
        String sourceType = params.getStr("sourceType");
        String status = params.getStr("status");
        String traceId = params.getStr("traceId");
        ReqContext context = ReqContextHolder.get();

        if (context != null && SubjectTypeEnum.PLATFORM.code().equals(context.getSubjectType())) {
            wrapper.eq(tenantId != null, "tenant_id", tenantId);
        } else if (context != null && SubjectTypeEnum.TENANT.code().equals(context.getSubjectType())) {
            wrapper.eq("tenant_id", context.getTenantId());
        } else if (context != null) {
            wrapper.eq("tenant_id", context.getTenantId());
            merchantId = context.getMerchantId();
        } else {
            wrapper.eq(tenantId != null, "tenant_id", tenantId);
        }

        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(journalId != null, "journal_id", journalId);
        wrapper.eq(entryId != null, "entry_id", entryId);
        wrapper.eq(accountId != null, "account_id", accountId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(StrUtil.isNotBlank(statementNo), "statement_no", statementNo);
        wrapper.eq(StrUtil.isNotBlank(merchantNo), "merchant_no", merchantNo);
        wrapper.eq(StrUtil.isNotBlank(journalNo), "journal_no", journalNo);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(merchantOrderNo), "merchant_order_no", merchantOrderNo);
        wrapper.eq(StrUtil.isNotBlank(eventType), "event_type", eventType);
        wrapper.eq(StrUtil.isNotBlank(accountNo), "account_no", accountNo);
        wrapper.eq(StrUtil.isNotBlank(accountType), "account_type", accountType);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(effectType), "effect_type", effectType);
        wrapper.eq(StrUtil.isNotBlank(sourceType), "source_type", sourceType);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        if (StrUtil.isBlank(params.getStr(Constant.ORDER_FIELD))) {
            wrapper.orderByDesc("posted_at").orderByDesc("id");
        }
        return wrapper;
    }
}
