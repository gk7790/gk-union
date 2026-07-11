package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PaymentMethodEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMethodDao extends BaseDao<PaymentMethodEntity> {
}
