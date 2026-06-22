package com.gk.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.psp.entity.PspRouteRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalTime;

@Mapper
public interface PspRouteRuleDao extends BaseDao<PspRouteRuleEntity> {
    PspRouteRuleEntity selectBestRouteRuleForOrder(
            @Param("tenantId") Long tenantId,
            @Param("merchantId") Long merchantId,
            @Param("merchantAppId") Long merchantAppId,
            @Param("countryCode") String countryCode,
            @Param("currency") String currency,
            @Param("methodCode") String methodCode,
            @Param("bankCode") String bankCode,
            @Param("amount") BigDecimal amount,
            @Param("direction") String direction,
            @Param("nowTime") LocalTime nowTime,
            @Param("status") Integer status
    );
}
