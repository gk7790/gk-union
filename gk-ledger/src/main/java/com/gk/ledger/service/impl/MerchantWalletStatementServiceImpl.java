package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dao.MerchantWalletStatementDao;
import com.gk.ledger.dto.MerchantWalletStatementDTO;
import com.gk.ledger.entity.MerchantWalletStatementEntity;
import com.gk.ledger.service.MerchantWalletStatementService;
import com.gk.ledger.support.SubjectDisplayEnricher;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.entity.MerchantAppEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MerchantWalletStatementServiceImpl extends CrudServiceImpl<MerchantWalletStatementDao, MerchantWalletStatementEntity, MerchantWalletStatementDTO>
        implements MerchantWalletStatementService {
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    private static final Map<String, String> ORDER_FIELD_MAP = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("tenantId", "tenant_id"),
            Map.entry("statementNo", "statement_no"),
            Map.entry("merchantId", "merchant_id"),
            Map.entry("merchantNo", "merchant_no"),
            Map.entry("merchantAppId", "merchant_app_id"),
            Map.entry("journalId", "journal_id"),
            Map.entry("journalNo", "journal_no"),
            Map.entry("entryId", "entry_id"),
            Map.entry("bizType", "biz_type"),
            Map.entry("bizId", "biz_id"),
            Map.entry("bizNo", "biz_no"),
            Map.entry("merchantOrderNo", "merchant_order_no"),
            Map.entry("eventType", "event_type"),
            Map.entry("accountId", "account_id"),
            Map.entry("accountNo", "account_no"),
            Map.entry("accountType", "account_type"),
            Map.entry("currency", "currency"),
            Map.entry("effectType", "effect_type"),
            Map.entry("sourceType", "source_type"),
            Map.entry("status", "status"),
            Map.entry("postedAt", "posted_at"),
            Map.entry("traceId", "trace_id"),
            Map.entry("createdAt", "created_at"),
            Map.entry("updatedAt", "updated_at")
    );

    @Autowired(required = false)
    private SubjectDisplayEnricher subjectDisplayEnricher;
    @Autowired(required = false)
    private MerchantAppDao merchantAppDao;

    @Override
    public PageData<MerchantWalletStatementDTO> page(DynMap params) {
        normalizeOrderField(params);
        IPage<MerchantWalletStatementEntity> page = baseDao.selectPage(
                getPage(params, null, false),
                getWrapper(params)
        );

        PageData<MerchantWalletStatementDTO> result = getPageData(page, currentDtoClass());
        enrichStatements(result.getItems());
        return result;
    }

    @Override
    public List<MerchantWalletStatementDTO> list(DynMap params) {
        List<MerchantWalletStatementDTO> items = super.list(params);
        enrichStatements(items);
        return items;
    }

    @Override
    public MerchantWalletStatementDTO get(Long id) {
        MerchantWalletStatementDTO dto = super.get(id);
        if (dto != null) {
            enrichStatements(List.of(dto));
        }
        return dto;
    }

    private void enrichStatements(List<MerchantWalletStatementDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        if (subjectDisplayEnricher != null) {
            subjectDisplayEnricher.enrichMerchantWalletStatements(items);
        }
        enrichMerchantAppNames(items);
        applyDisplayScope(items);
    }

    private void enrichMerchantAppNames(List<MerchantWalletStatementDTO> items) {
        if (merchantAppDao == null) {
            return;
        }
        Set<Long> appIds = items.stream()
                .map(MerchantWalletStatementDTO::getMerchantAppId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (appIds.isEmpty()) {
            return;
        }
        Map<Long, MerchantAppEntity> apps = merchantAppDao.selectBatchIds(appIds).stream()
                .collect(Collectors.toMap(MerchantAppEntity::getId, Function.identity(), (left, right) -> left));
        items.forEach(item -> {
            MerchantAppEntity app = apps.get(item.getMerchantAppId());
            if (app != null) {
                item.setMerchantAppName(app.getAppName());
            }
        });
    }

    private void applyDisplayScope(List<MerchantWalletStatementDTO> items) {
        String subjectType = ReqContextHolder.getSubjectType();
        boolean platform = SubjectTypeEnum.PLATFORM.matches(subjectType);
        boolean tenant = SubjectTypeEnum.TENANT.matches(subjectType);
        items.forEach(item -> {
            if (!platform) {
                item.setTenantName(null);
            }
            if (!tenant) {
                item.setMerchantNo(null);
                item.setMerchantName(null);
            }
        });
    }

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
        Instant postedAtStart = getInstant(params, "postedAtStart", "startTime");
        Instant postedAtEnd = getInstant(params, "postedAtEnd", "endTime");
        ReqContext context = ReqContextHolder.get();

        if (SubjectTypeEnum.PLATFORM.code().equals(context.getSubjectType())) {
            wrapper.eq(tenantId != null, "tenant_id", tenantId);
        } else if (SubjectTypeEnum.TENANT.code().equals(context.getSubjectType())) {
            wrapper.eq("tenant_id", context.getTenantId());
        } else {
            wrapper.eq("tenant_id", context.getTenantId());
            merchantId = context.getMerchantId();
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
        wrapper.ge(postedAtStart != null, "posted_at", postedAtStart);
        wrapper.le(postedAtEnd != null, "posted_at", postedAtEnd);
        if (StrUtil.isBlank(params.getStr(Constant.ORDER_FIELD))) {
            wrapper.orderByDesc("posted_at").orderByDesc("id");
        }
        return wrapper;
    }

    private void normalizeOrderField(DynMap params) {
        String orderField = params.getStr(Constant.ORDER_FIELD);
        if (StrUtil.isBlank(orderField)) {
            return;
        }
        String column = ORDER_FIELD_MAP.get(orderField);
        if (StrUtil.isBlank(column) && ORDER_FIELD_MAP.containsValue(orderField)) {
            column = orderField;
        }
        if (StrUtil.isBlank(column)) {
            params.remove(Constant.ORDER_FIELD);
            params.remove(Constant.ORDER);
            return;
        }
        params.put(Constant.ORDER_FIELD, column);
    }

    private Instant getInstant(DynMap params, String... keys) {
        for (String key : keys) {
            LocalDateTime value;
            try {
                value = params.getLocalDateTime(key);
            } catch (Exception ignored) {
                continue;
            }
            if (value != null) {
                return value.atZone(DEFAULT_ZONE_ID).toInstant();
            }
        }
        return null;
    }
}
