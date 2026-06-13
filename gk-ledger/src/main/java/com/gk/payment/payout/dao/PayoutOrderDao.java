package com.gk.payment.payout.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayoutOrderDao extends BaseDao<PayoutOrderEntity> {
}
