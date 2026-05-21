package com.gk.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysUserDTO implements Serializable {

	private Long id;
    private Long tenantId;
    private Long deptId;
    private String username;
    private String nickname;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
    private String realName;
    private String avatar;
    private Integer gender;
    private String email;
    private String mobile;
	private Integer status;
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime createdAt;
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer superAdmin;
	private List<Long> roleIdList;
	private List<Long> postIdList;
	private String deptName;
    private String tenantName;
	private Integer authType;
	private String authSecret;

}
