package com.gk.payment.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.entity.PayOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayOrderDao extends BaseDao<PayOrderEntity> {
}
