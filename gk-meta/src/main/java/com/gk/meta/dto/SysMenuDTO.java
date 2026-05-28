package com.gk.meta.dto;

import com.gk.common.tools.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 菜单管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "菜单管理")
public class SysMenuDTO extends TreeNode<SysMenuDTO> implements Serializable {

	@Schema(title = "id")
	private Long id;

	@Schema(title = "上级ID")
	private Long pid;

    @Schema(title = "租户id")
    private Long tenantId;

    @Schema(title = "菜单名称")
	private String name;

    @Schema(title = "地址栏路径")
    private String path;

    @Schema(title = "授权(多个用逗号分隔，如：sys:user:list,sys:user:save)")
	private String authCode;

    @Schema(title = "排序")
	private Integer sort;

    @Schema(title = "上级菜单名称")
	private String parentName;

    @Schema(title = "状态")
    private Integer status;

    @Schema(title = "激活路径")
    private String activePath;

    @Schema(title = "组件路径")
    private String component;

    @Schema(title = "标题")
    private String title;

    @Schema(title = "类型")
    private Integer type;

    @Schema(title = "领域")
    private Integer scope;

    @Schema(title = "业务领域")
    private Integer domain;

    /**
     * 样式
     */
    private SysMenuMeta meta;
}
