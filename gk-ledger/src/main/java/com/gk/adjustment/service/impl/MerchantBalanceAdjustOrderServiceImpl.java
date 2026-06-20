package com.gk.adjustment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.utils.BizKeyUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.adjustment.dao.MerchantBalanceAdjustOrderDao;
import com.gk.adjustment.dto.MerchantBalanceAdjustOrderDTO;
import com.gk.adjustment.entity.MerchantBalanceAdjustOrderEntity;
import com.gk.adjustment.service.MerchantBalanceAdjustOrderService;
import com.gk.ledger.enums.LedgerJournalSourceEnum;
import com.gk.adjustment.enums.MerchantBalanceAdjustStatusEnum;
import com.gk.adjustment.enums.MerchantBalanceAdjustTypeEnum;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.MerchantBalanceAdjustPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MerchantBalanceAdjustOrderServiceImpl extends CrudServiceImpl<MerchantBalanceAdjustOrderDao, MerchantBalanceAdjustOrderEntity, MerchantBalanceAdjustOrderDTO>
        implements MerchantBalanceAdjustOrderService {

    private final MerchantDao merchantDao;
    private final LedgerPostingService ledgerPostingService;

    @Override
    public QueryWrapper<MerchantBalanceAdjustOrderEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantBalanceAdjustOrderEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        String merchantNo = params.getStr("merchantNo");
        String adjustOrderNo = params.getStr("adjustOrderNo");
        String adjustType = params.getStr("adjustType");
        String sourceType = params.getStr("sourceType");
        String currency = params.getStr("currency");
        String status = params.getStr("status");
        String ledgerJournalNo = params.getStr("ledgerJournalNo");
        String relatedOrderNo = params.getStr("relatedOrderNo");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(StrUtil.isNotBlank(merchantNo), "merchant_no", merchantNo);
        wrapper.eq(StrUtil.isNotBlank(adjustOrderNo), "adjust_order_no", adjustOrderNo);
        wrapper.eq(StrUtil.isNotBlank(adjustType), "adjust_type", adjustType);
        wrapper.eq(StrUtil.isNotBlank(sourceType), "source_type", sourceType);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(ledgerJournalNo), "ledger_journal_no", ledgerJournalNo);
        wrapper.eq(StrUtil.isNotBlank(relatedOrderNo), "related_order_no", relatedOrderNo);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantBalanceAdjustOrderDTO submit(MerchantBalanceAdjustOrderDTO dto) {
        MerchantBalanceAdjustTypeEnum adjustType = validateSubmit(dto);
        MerchantEntity merchant = merchantDao.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new GkException("Merchant not found: " + dto.getMerchantId());
        }
        if (!dto.getTenantId().equals(merchant.getTenantId())) {
            throw new GkException("Merchant tenant does not match");
        }

        MerchantBalanceAdjustOrderEntity entity = ConvertUtils.sourceToTarget(dto, MerchantBalanceAdjustOrderEntity.class);
        entity.setMerchantNo(merchant.getMerchantNo());
        entity.setAdjustOrderNo(StringUtils.defaultIfBlank(entity.getAdjustOrderNo(), BizKeyUtils.genMerchantBalanceAdjustOrderNo()));
        entity.setAdjustType(adjustType.code());
        entity.setSourceType(LedgerJournalSourceEnum.MANUAL.code());
        entity.setCurrency(normalizeCurrency(entity.getCurrency()));
        entity.setStatus(MerchantBalanceAdjustStatusEnum.CREATED.code());
        entity.setVersion(0);
        baseDao.insert(entity);

        LedgerPostingResult postingResult = ledgerPostingService.postMerchantBalanceAdjust(toPostingRequest(entity));
        entity.setStatus(MerchantBalanceAdjustStatusEnum.POSTED.code());
        entity.setLedgerJournalNo(postingResult.getJournalNo());
        entity.setPostedAt(Instant.now());
        entity.setVersion(1);
        baseDao.updateById(entity);
        return ConvertUtils.sourceToTarget(entity, MerchantBalanceAdjustOrderDTO.class);
    }

    private MerchantBalanceAdjustTypeEnum validateSubmit(MerchantBalanceAdjustOrderDTO dto) {
        if (dto == null || dto.getTenantId() == null || dto.getMerchantId() == null
                || StringUtils.isBlank(dto.getCurrency()) || dto.getAmount() == null
                || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new GkException("Invalid merchant balance adjust order");
        }
        MerchantBalanceAdjustTypeEnum adjustType = StringCodeEnum.fromCode(MerchantBalanceAdjustTypeEnum.class, dto.getAdjustType());
        if (adjustType == null) {
            throw new GkException("Invalid merchant balance adjust type");
        }
        return adjustType;
    }

    private MerchantBalanceAdjustPostingRequest toPostingRequest(MerchantBalanceAdjustOrderEntity entity) {
        MerchantBalanceAdjustPostingRequest request = new MerchantBalanceAdjustPostingRequest();
        request.setTenantId(entity.getTenantId());
        request.setMerchantId(entity.getMerchantId());
        request.setMerchantNo(entity.getMerchantNo());
        request.setBizId(entity.getId());
        request.setAdjustOrderNo(entity.getAdjustOrderNo());
        request.setAdjustType(entity.getAdjustType());
        request.setCurrency(entity.getCurrency());
        request.setAmount(entity.getAmount());
        request.setReverseOfJournalNo(entity.getReverseOfJournalNo());
        request.setTraceId(entity.getTraceId());
        request.setReason(entity.getReason());
        return request;
    }

    private String normalizeCurrency(String currency) {
        return StringUtils.defaultString(currency).trim().toUpperCase(Locale.ROOT);
    }
}
