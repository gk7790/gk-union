package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayinOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PayinOrderDao extends BaseDao<PayinOrderEntity> {

    PayinOrderEntity selectOpenApiByMerchantOrderNo(@Param("tenantId") Long tenantId,
                                                  @Param("merchantId") Long merchantId,
                                                  @Param("merchantOrderNo") String merchantOrderNo);

    PayinOrderEntity selectOpenApiByPayinOrderNo(@Param("tenantId") Long tenantId,
                                             @Param("merchantId") Long merchantId,
                                             @Param("payinOrderNo") String payinOrderNo);
}
