package com.gk.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.psp.entity.PspFeeRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.Instant;

@Mapper
public interface PspFeeRuleDao extends BaseDao<PspFeeRuleEntity> {
    PspFeeRuleEntity selectBestMatchForOrder(
            @Param("tenantId") Long tenantId,
            @Param("pspId") Long pspId,
            @Param("pspAccountId") Long pspAccountId,
            @Param("pspMethodId") Long pspMethodId,
            @Param("countryCode") String countryCode,
            @Param("currency") String currency,
            @Param("methodCode") String methodCode,
            @Param("orderAmount") BigDecimal orderAmount,
            @Param("direction") String direction,
            @Param("now") Instant now,
            @Param("status") Integer status
    );
}
