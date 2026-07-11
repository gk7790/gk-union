package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.common.model.DynMap;
import com.gk.payment.dto.PayinOrderDTO;
import com.gk.payment.entity.PayinOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PayinOrderDao extends BaseDao<PayinOrderEntity> {

    Long countPageWithName(DynMap params);

    List<PayinOrderDTO> selectPageWithName(DynMap params);

    PayinOrderEntity selectOpenApiByMerchantOrderNo(@Param("tenantId") Long tenantId,
                                                  @Param("merchantId") Long merchantId,
                                                  @Param("merchantOrderNo") String merchantOrderNo);

    PayinOrderEntity selectOpenApiByPayinOrderNo(@Param("tenantId") Long tenantId,
                                             @Param("merchantId") Long merchantId,
                                             @Param("payinOrderNo") String payinOrderNo);
}
