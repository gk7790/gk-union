
package com.gk.platform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gk.common.utils.DateUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
* 岗位管理
*
* @author Mark sunlightcs@gmail.com
*/
@Data
@Schema(title = "岗位管理")
public class SysPostDTO implements Serializable {
    @Schema(title = "id")
    private Long id;
    @Schema(title = "岗位编码")
    private String postCode;
    @Schema(title = "岗位名称")
    private String postName;
    @Schema(title = "排序")
    private Integer sort;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "租户编码")
    private Long tenantCode;
    @Schema(title = "创建时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    private LocalDateTime createdAt;

}