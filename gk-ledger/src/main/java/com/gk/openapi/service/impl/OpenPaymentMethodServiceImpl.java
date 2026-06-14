package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.infra.enums.StatusEnum;
import com.gk.openapi.dto.PaymentMethodResponse;
import com.gk.openapi.service.OpenPaymentMethodService;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.entity.PspMethodEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenPaymentMethodServiceImpl implements OpenPaymentMethodService {
    private final PspMethodDao pspMethodDao;

    @Override
    public List<PaymentMethodResponse> list(String countryCode, String currency, String direction) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<PspMethodEntity>().eq("status", StatusEnum.NORMAL.code());
        if (StringUtils.isNotBlank(countryCode)) {
            wrapper.eq("country_code", countryCode);
        }
        if (StringUtils.isNotBlank(currency)) {
            wrapper.eq("currency", currency);
        }
        if (StringUtils.isNotBlank(direction)) {
            wrapper.eq("direction", direction);
        }

        return pspMethodDao.selectList(wrapper).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PaymentMethodResponse::getMethodCode))
                .toList();
    }

    private PaymentMethodResponse toResponse(PspMethodEntity entity) {
        PaymentMethodResponse response = new PaymentMethodResponse();
        response.setMethodCode(entity.getMethodCode());
        response.setMethodName(entity.getMethodName());
        response.setCountryCode(entity.getCountryCode());
        response.setCurrency(entity.getCurrency());
        response.setDirection(entity.getDirection());
        response.setMinAmount(entity.getMinAmount());
        response.setMaxAmount(entity.getMaxAmount());
        return response;
    }
}
