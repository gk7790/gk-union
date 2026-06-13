package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayoutOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayoutOrderDao extends BaseDao<PayoutOrderEntity> {
}
