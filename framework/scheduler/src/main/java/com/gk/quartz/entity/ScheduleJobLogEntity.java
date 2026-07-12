package com.gk.quartz.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 定时任务日志
 *
 * @author Lowen
 */
@Data
@TableName("schedule_job_log")
public class ScheduleJobLogEntity implements Serializable {

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * 任务id
	 */
	private Long jobId;
	/**
	 * spring bean名称
	 */
	private String beanName;
	/**
	 * 参数
	 */
	private String params;
	/**
	 * 任务状态    0：失败    1：成功
	 */
	private Integer status;
	/**
	 * 任务执行结果
	 */
	private String result;
	/**
	 * 失败信息
	 */
	private String error;
	/**
	 * 耗时(单位：毫秒)
	 */
	private Integer times;
	/**
	 * 创建时间
	 */
	private LocalDateTime createdAt;

}