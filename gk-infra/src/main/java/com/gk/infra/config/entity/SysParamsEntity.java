package com.gk.infra.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_params")
public class SysParamsEntity extends SimpleEntity {
    /**
     * 参数编码
     */
    private String paramCode;
    /**
     * 参数值
     */
    private String paramValue;
    /**
     * 类型   0：系统参数   1：非系统参数
     */
    private Integer paramType;
    /**
     * 备注
     */
    private String remark;
}
