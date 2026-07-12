package com.gk.merchant.service.impl;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.merchant.dto.MerchantOptionsDTO;
import com.gk.merchant.service.MerchantAppService;
import com.gk.merchant.service.MerchantOptionsService;
import com.gk.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantOptionsServiceImpl implements MerchantOptionsService {

    private final MerchantService merchantService;
    private final MerchantAppService merchantAppService;

    @Override
    public MerchantOptionsDTO options() {
        String subjectType = ReqContextHolder.getSubjectType();
        MerchantOptionsDTO response = new MerchantOptionsDTO();
        DynMap params = new DynMap();

        if (SubjectTypeEnum.TENANT.matches(subjectType) || SubjectTypeEnum.PLATFORM.matches(subjectType)) {
            response.setMerchants(merchantService.getDict(params));
            response.setApps(merchantAppService.getDict(params));
            return response;
        }

        if (SubjectTypeEnum.MERCHANT.matches(subjectType)) {
            response.setApps(merchantAppService.getDict(params));
        }
        return response;
    }
}
