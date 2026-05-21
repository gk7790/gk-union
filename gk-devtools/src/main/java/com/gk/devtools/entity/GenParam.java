package com.gk.devtools.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 代码生成参数配置
 *
 * @author Lowen
 */
@Data
public class GenParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private String packageName;
    private String version;
    private String author;
    private String email;
    private String backendPath;
    private String frontendPath;
}