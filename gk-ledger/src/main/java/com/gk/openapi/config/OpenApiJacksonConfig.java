package com.gk.openapi.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.gk.openapi.dto.BalanceQueryRequest;
import com.gk.openapi.dto.BalanceResponse;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderQueryRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.dto.PaymentMethodQueryRequest;
import com.gk.openapi.dto.PaymentMethodResponse;
import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderQueryRequest;
import com.gk.openapi.dto.PayoutOrderResponse;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 商户 Open API JSON 字段统一 snake_case（注册到全局 ObjectMapper，供 MVC 序列化与参数绑定共用）�?
 */
@Configuration
public class OpenApiJacksonConfig {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface OpenApiSnakeCaseMixIn {
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer openApiSnakeCaseMixInCustomizer() {
        return OpenApiJacksonConfig::applySnakeCaseMixIns;
    }

    public static void applySnakeCaseMixIns(Jackson2ObjectMapperBuilder builder) {
        builder.mixIn(PayOrderResponse.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PayOrderCreateRequest.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PayOrderQueryRequest.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PayoutOrderResponse.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PayoutOrderCreateRequest.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PayoutOrderCreateRequest.Payee.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PayoutOrderQueryRequest.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(BalanceResponse.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(BalanceQueryRequest.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PaymentMethodResponse.class, OpenApiSnakeCaseMixIn.class);
        builder.mixIn(PaymentMethodQueryRequest.class, OpenApiSnakeCaseMixIn.class);
    }
}
