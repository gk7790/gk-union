package com.gk.infra.dict.dto;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 字典数据
 *
 * @author Lowen
 */
@Data
@Schema(title = "字典数据")
public class SysDictDataDTO implements Serializable {

	@Schema(title = "id")
	private Long id;

	@Schema(title = "字典类型ID")
	private Long dictTypeId;

	@Schema(title = "字典标签")
	private String dictLabel;

	@Schema(title = "字典值")
	private String dictValue;

    @Schema(title = "国际化")
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String i18nKey;

	@Schema(title = "回显样式")
	private String attrType;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "排序")
	private Integer sort;

	@Schema(title = "创建时间")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Instant createdAt;

	@Schema(title = "更新时间")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Instant updatedAt;
}
