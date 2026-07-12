package com.gk.devtools.entity;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.time.DateUtils;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 基类管理
 *
 * @author Lowen
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("gen_base_class")
public class BaseClassEntity {
	/**
	 * id
	 */
	private Long id;
	/**
	 * 基类包名
	 */
	private String packageName;
    /**
     * 基类编码
     */
	private String code;
    /**
     * 公共字段，多个用英文逗号分隔
     */
	private String fields;
    /**
     * 备注
     */
	private String remark;
	/**
	 * 创建时间
	 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}