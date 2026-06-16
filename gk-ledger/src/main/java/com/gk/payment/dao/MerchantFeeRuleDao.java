package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.Instant;

@Mapper
public interface MerchantFeeRuleDao extends BaseDao<MerchantFeeRuleEntity> {
    MerchantFeeRuleEntity selectBestMatchForOrder(
            @Param("tenantId") Long tenantId,
            @Param("merchantId") Long merchantId,
            @Param("merchantAppId") Long merchantAppId,
            @Param("countryCode") String countryCode,
            @Param("currency") String currency,
            @Param("methodCode") String methodCode,
            @Param("orderAmount") BigDecimal orderAmount,
            @Param("orderType") String orderType,
            @Param("now") Instant now,
            @Param("status") Integer status
    );
}
