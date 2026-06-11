package com.gk.merchant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.utils.BizKeyUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.dto.MerchantAppDTO;
import com.gk.merchant.dto.MerchantDTO;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.service.MerchantAppService;
import com.gk.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl extends CrudServiceImpl<MerchantDao, MerchantEntity, MerchantDTO> implements MerchantService {

    private static final String DEFAULT_MERCHANT_TYPE = "COMPANY";
    private static final String DEFAULT_RISK_STATUS = "NORMAL";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final String DEFAULT_LANG = "zh-CN";
    private static final String DEFAULT_SETTLE_MODE = "MANUAL";
    private static final String DEFAULT_SETTLE_CYCLE = "T1";
    private static final int DEFAULT_STATUS = 1;
    private static final int MERCHANT_NO_GENERATE_MAX_ATTEMPTS = 5;

    private final MerchantAppService merchantAppService;

    @Override
    public QueryWrapper<MerchantEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantEntity> wrapper = new QueryWrapper<>();

        Long tenantId = params.getLong("tenantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String merchantNo = params.getStr("merchantNo");
        String merchantName = params.getStr("merchantName");
        String countryCode = params.getStr("countryCode");
        String defaultCurrency = params.getStr("defaultCurrency");
        String riskStatus = params.getStr("riskStatus");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(merchantNo), "merchant_no", merchantNo);
        wrapper.like(StrUtil.isNotBlank(merchantName), "merchant_name", merchantName);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(defaultCurrency), "default_currency", defaultCurrency);
        wrapper.eq(StrUtil.isNotBlank(riskStatus), "risk_status", riskStatus);

        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(MerchantDTO dto) {
        AssertUtils.isNull(dto.getTenantId(), "tenantId");
        AssertUtils.isBlank(dto.getMerchantName(), "merchantName");
        AssertUtils.isBlank(dto.getCountryCode(), "countryCode");
        AssertUtils.isBlank(dto.getDefaultCurrency(), "defaultCurrency");

        MerchantEntity entity = ConvertUtils.sourceToTarget(dto, MerchantEntity.class);
        entity.setId(null);
        applyCreateDefaults(entity);

        boolean userProvidedMerchantNo = StrUtil.isNotBlank(entity.getMerchantNo());
        if (!userProvidedMerchantNo) {
            entity.setMerchantNo(generateUniqueMerchantNo());
        }

        insertWithUniqueMerchantNo(entity, userProvidedMerchantNo);

        dto.setId(entity.getId());
        dto.setMerchantNo(entity.getMerchantNo());

        MerchantAppDTO apiApp = dto.getApiApp();
        if (apiApp != null) {
            apiApp.setId(null);
            apiApp.setTenantId(entity.getTenantId());
            apiApp.setMerchantId(entity.getId());
            if (StrUtil.isBlank(apiApp.getAppName())) {
                apiApp.setAppName(entity.getMerchantName() + " API");
            }
            merchantAppService.save(apiApp);
            dto.setApiApp(apiApp);
        }
    }

    private void applyCreateDefaults(MerchantEntity entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(DEFAULT_STATUS);
        }
        if (StrUtil.isBlank(entity.getMerchantType())) {
            entity.setMerchantType(DEFAULT_MERCHANT_TYPE);
        }
        if (StrUtil.isBlank(entity.getRiskStatus())) {
            entity.setRiskStatus(DEFAULT_RISK_STATUS);
        }
        if (StrUtil.isBlank(entity.getTimezone())) {
            entity.setTimezone(DEFAULT_TIMEZONE);
        }
        if (StrUtil.isBlank(entity.getLang())) {
            entity.setLang(DEFAULT_LANG);
        }
        if (StrUtil.isBlank(entity.getSettleMode())) {
            entity.setSettleMode(DEFAULT_SETTLE_MODE);
        }
        if (StrUtil.isBlank(entity.getSettleCycle())) {
            entity.setSettleCycle(DEFAULT_SETTLE_CYCLE);
        }
    }

    private void insertWithUniqueMerchantNo(MerchantEntity entity, boolean userProvidedMerchantNo) {
        for (int attempt = 0; attempt < MERCHANT_NO_GENERATE_MAX_ATTEMPTS; attempt++) {
            if (attempt > 0 && !userProvidedMerchantNo) {
                entity.setMerchantNo(generateUniqueMerchantNo());
            }
            try {
                insert(entity);
                return;
            } catch (DuplicateKeyException ex) {
                if (userProvidedMerchantNo) {
                    throw ex;
                }
            }
        }
        throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String generateUniqueMerchantNo() {
        for (int attempt = 0; attempt < MERCHANT_NO_GENERATE_MAX_ATTEMPTS; attempt++) {
            String merchantNo = BizKeyUtils.genMerchantNo();
            Long count = baseDao.selectCount(new QueryWrapper<MerchantEntity>().eq("merchant_no", merchantNo));
            if (count == null || count == 0L) {
                return merchantNo;
            }
        }
        throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
