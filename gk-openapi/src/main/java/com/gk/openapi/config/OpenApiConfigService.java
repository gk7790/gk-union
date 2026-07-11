package com.gk.openapi.config;

import com.gk.common.constant.Constant;
import com.gk.payment.domain.enums.SignTypeEnum;
import com.gk.infra.config.service.SysParamConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenApiConfigService {
    private final SysParamConfigReader configReader;

    public GkOpenApiConfig get() {
        GkOpenApiConfig config = configReader.getObject(
                Constant.GK_OPENAPI_CONFIG_KEY, GkOpenApiConfig.class, new GkOpenApiConfig());
        config.setDefaultSignType(SignTypeEnum.normalizeOpenApiSignType(config.getDefaultSignType()));
        return config;
    }
}
