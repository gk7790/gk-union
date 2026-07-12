package com.gk.quartz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 定时任务
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@Schema(title = "定时任务")
public class ScheduleJobDTO implements Serializable {

    @Schema(title = "id")
    private Long id;

    @Schema(title = "spring bean名称")
    private String beanName;

    @Schema(title = "参数")
    private String params;

    @Schema(title = "cron表达式")
    private String cronExpression;

    @Schema(title = "任务状态  0：暂停  1：正常")
    private Integer status;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建时间")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

}
