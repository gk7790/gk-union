package com.gk.devtools.entity;

import lombok.Data;

/**
 * 创建菜单
 *
 * @author Lowen
 */
@Data
public class MenuEntity {
    private Long pid;
    private String name;
    private String icon;
    private String moduleName;
    private String className;

}