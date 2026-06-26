package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PayOrderDao extends BaseDao<PayOrderEntity> {

    PayOrderEntity selectOpenApiByMerchantOrderNo(@Param("tenantId") Long tenantId,
                                                  @Param("merchantId") Long merchantId,
                                                  @Param("merchantOrderNo") String merchantOrderNo);

    PayOrderEntity selectOpenApiByPayOrderNo(@Param("tenantId") Long tenantId,
                                             @Param("merchantId") Long merchantId,
                                             @Param("payOrderNo") String payOrderNo);
}
