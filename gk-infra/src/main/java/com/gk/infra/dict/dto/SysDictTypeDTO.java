package com.gk.infra.dict.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 字典类型
 *
 * @author Lowen
 */
@Data
@Schema(title = "字典类型")
public class SysDictTypeDTO implements Serializable {

	@Schema(title = "id")
	private Long id;

	@Schema(title = "字典类型")
	private String dictType;

	@Schema(title = "字典名称")
	private String dictName;

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
