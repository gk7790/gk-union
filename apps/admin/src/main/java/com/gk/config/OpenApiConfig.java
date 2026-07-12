package com.gk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI gkOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GK Union Admin API")
                        .version("1.0.0")
                        .description("后台管理接口文档。除登录、OpenAPI、PSP回调等公开接口外，业务接口默认需要在 Swagger UI 中点击 Authorize 并填写 Bearer Token。"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("填写登录接口返回的 accessToken，Swagger 会自动添加 Authorization: Bearer <token>")));
    }
}
