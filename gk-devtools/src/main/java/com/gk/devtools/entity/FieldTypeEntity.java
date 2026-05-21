package com.gk.devtools.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.gk.common.utils.DateUtils;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 字段类型管理
 *
 * @author Lowen
 */
@Data
@TableName("gen_field_type")
public class FieldTypeEntity {
	/**
	 * id
	 */
	@TableId
	private Long id;
    /**
     * 字段类型
     */
	private String columnType;
    /**
     * 属性类型
     */
	private String attrType;
    /**
     * 前端字段类型
     */
    private String uiType;
	/**
	 * 属性包名
	 */
	private String packageName;
	/**
	 * 创建时间
	 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}