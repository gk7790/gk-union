package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.OrderStatusLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStatusLogDao extends BaseDao<OrderStatusLogEntity> {
}
