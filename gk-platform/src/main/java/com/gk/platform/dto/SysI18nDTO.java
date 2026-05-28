package com.gk.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
* 系统-国际化
*
* @author Lowen lowen@gmail.com
* @since 3.0 2026-05-23
*/
@Data
@Schema(title = "系统-国际化")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysI18nDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(title = "NULL=全局；有值=租户覆盖")
    private Long tenantId;
    @Schema(title = "MENU / DICT / GAME / CONFIG / NOTICE / PAGE")
    private String bizType;
    @Schema(title = "对应业务ID")
    private Long bizId;
    @Schema(title = "国际化key")
    private String i18nKey;
    @Schema(title = "zh_CN / en_US")
    private String lang;
    @Schema(title = "翻译内容")
    private String value;
    @Schema(title = "创建者")
    private Long createdBy;
    @Schema(title = "创建时间")
    private LocalDateTime createdAt;
    @Schema(title = "修改者")
    private Long updatedBy;
    @Schema(title = "修改时间")
    private LocalDateTime updatedAt;

    private Boolean hasChild;

    private Map<String, String> items;

}