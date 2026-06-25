package com.gk.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "商户主体范围下拉选项")
public class MerchantOptionsDTO {

    @Schema(description = "商户列表，租�?平台用户返回")
    private List<MerchantDTO> merchants;

    @Schema(description = "商户APP列表")
    private List<MerchantAppDTO> apps;
}
