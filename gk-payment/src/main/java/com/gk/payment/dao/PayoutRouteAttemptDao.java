package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayoutRouteAttemptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PayoutRouteAttemptDao extends BaseDao<PayoutRouteAttemptEntity> {
    Integer selectMaxAttemptNo(@Param("tenantId") Long tenantId, @Param("payoutOrderId") Long payoutOrderId);
}
