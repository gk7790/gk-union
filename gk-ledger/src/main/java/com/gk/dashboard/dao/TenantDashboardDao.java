package com.gk.dashboard.dao;

import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTodoDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

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

    List<TenantDashboardTrendDTO.TrendPoint> selectPayTrend(@Param("tenantId") Long tenantId,
                                                          @Param("currency") String currency,
                                                          @Param("rangeStart") Instant rangeStart,
                                                          @Param("rangeEnd") Instant rangeEnd,
                                                          @Param("tzOffset") String tzOffset);

    List<TenantDashboardTrendDTO.TrendPoint> selectPayoutTrend(@Param("tenantId") Long tenantId,
                                                               @Param("currency") String currency,
                                                               @Param("rangeStart") Instant rangeStart,
                                                               @Param("rangeEnd") Instant rangeEnd,
                                                               @Param("tzOffset") String tzOffset);

    List<TenantDashboardTodoDTO.TodoItem> listTodos(@Param("tenantId") Long tenantId,
                                                    @Param("currency") String currency,
                                                    @Param("type") String type,
                                                    @Param("limit") int limit);
}
