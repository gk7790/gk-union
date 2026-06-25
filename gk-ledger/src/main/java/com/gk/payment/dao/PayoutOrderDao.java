package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayoutOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PayoutOrderDao extends BaseDao<PayoutOrderEntity> {

    PayoutOrderEntity selectOpenApiByMerchantOrderNo(@Param("tenantId") Long tenantId,
                                                     @Param("merchantId") Long merchantId,
                                                     @Param("merchantOrderNo") String merchantOrderNo);

    PayoutOrderEntity selectOpenApiByPayoutOrderNo(@Param("tenantId") Long tenantId,
                                                   @Param("merchantId") Long merchantId,
                                                   @Param("payoutOrderNo") String payoutOrderNo);
}
