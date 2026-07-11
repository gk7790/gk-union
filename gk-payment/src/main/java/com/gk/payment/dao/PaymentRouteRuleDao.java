package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.common.model.DynMap;
import com.gk.payment.dto.PaymentRouteRuleDTO;
import com.gk.payment.entity.PaymentRouteRuleEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PaymentRouteRuleDao extends BaseDao<PaymentRouteRuleEntity> {
    Long countPageWithName(DynMap params);

    List<PaymentRouteRuleDTO> selectPageWithName(DynMap params);
}
