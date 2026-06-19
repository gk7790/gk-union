package com.gk.dashboard.dao;

import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface TenantDashboardDao {

    TenantDashboardSummaryDTO.OrderMetric selectPayOrderStats(@Param("tenantId") Long tenantId,
                                                              @Param("currency") String currency,
                                                              @Param("rangeStart") Instant rangeStart,
                                                              @Param("rangeEnd") Instant rangeEnd);

    TenantDashboardSummaryDTO.OrderMetric selectPayoutOrderStats(@Param("tenantId") Long tenantId,
                                                                 @Param("currency") String currency,
                                                                 @Param("rangeStart") Instant rangeStart,
                                                                 @Param("rangeEnd") Instant rangeEnd);

    Long countActiveMerchants(@Param("tenantId") Long tenantId,
                              @Param("currency") String currency,
                              @Param("rangeStart") Instant rangeStart,
                              @Param("rangeEnd") Instant rangeEnd);

    TenantDashboardSummaryDTO.Balance selectBalanceSummary(@Param("tenantId") Long tenantId,
                                                         @Param("currency") String currency);

    TenantDashboardSummaryDTO.MerchantCount selectMerchantCount(@Param("tenantId") Long tenantId);

    TenantDashboardSummaryDTO.Todo selectTodoCounts(@Param("tenantId") Long tenantId,
                                                    @Param("currency") String currency);
}
