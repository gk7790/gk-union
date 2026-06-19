package com.gk.dashboard.dao;

import com.gk.dashboard.dto.TenantDashboardRecentOrderDTO;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTodoDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface MerchantDashboardDao {

    TenantDashboardSummaryDTO.OrderMetric selectPayOrderStats(@Param("tenantId") Long tenantId,
                                                              @Param("merchantId") Long merchantId,
                                                              @Param("currency") String currency,
                                                              @Param("rangeStart") Instant rangeStart,
                                                              @Param("rangeEnd") Instant rangeEnd);

    TenantDashboardSummaryDTO.OrderMetric selectPayoutOrderStats(@Param("tenantId") Long tenantId,
                                                                 @Param("merchantId") Long merchantId,
                                                                 @Param("currency") String currency,
                                                                 @Param("rangeStart") Instant rangeStart,
                                                                 @Param("rangeEnd") Instant rangeEnd);

    TenantDashboardSummaryDTO.Balance selectBalanceSummary(@Param("tenantId") Long tenantId,
                                                           @Param("merchantId") Long merchantId,
                                                           @Param("currency") String currency);

    TenantDashboardSummaryDTO.Todo selectTodoCounts(@Param("tenantId") Long tenantId,
                                                  @Param("merchantId") Long merchantId,
                                                  @Param("currency") String currency);

    List<TenantDashboardTrendDTO.TrendPoint> selectPayTrend(@Param("tenantId") Long tenantId,
                                                            @Param("merchantId") Long merchantId,
                                                            @Param("currency") String currency,
                                                            @Param("rangeStart") Instant rangeStart,
                                                            @Param("rangeEnd") Instant rangeEnd,
                                                            @Param("tzOffset") String tzOffset);

    List<TenantDashboardTrendDTO.TrendPoint> selectPayoutTrend(@Param("tenantId") Long tenantId,
                                                               @Param("merchantId") Long merchantId,
                                                               @Param("currency") String currency,
                                                               @Param("rangeStart") Instant rangeStart,
                                                               @Param("rangeEnd") Instant rangeEnd,
                                                               @Param("tzOffset") String tzOffset);

    List<TenantDashboardTodoDTO.TodoItem> listTodos(@Param("tenantId") Long tenantId,
                                                    @Param("merchantId") Long merchantId,
                                                    @Param("currency") String currency,
                                                    @Param("type") String type,
                                                    @Param("limit") int limit);

    List<TenantDashboardRecentOrderDTO.RecentOrder> selectRecentPayOrders(@Param("tenantId") Long tenantId,
                                                                          @Param("merchantId") Long merchantId,
                                                                          @Param("currency") String currency,
                                                                          @Param("limit") int limit);

    List<TenantDashboardRecentOrderDTO.RecentOrder> selectRecentPayoutOrders(@Param("tenantId") Long tenantId,
                                                                              @Param("merchantId") Long merchantId,
                                                                              @Param("currency") String currency,
                                                                              @Param("limit") int limit);
}
