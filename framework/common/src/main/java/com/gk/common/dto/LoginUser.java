package com.gk.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {
    private Long id;
    private Long deptId;
    private Long tenantId;
    private String username;
    private String nickName;
    private String realName;
    private String email;
    private Boolean isAdmin;
    private Integer gender;
    private List<String> roles;
    private List<String> authCode;
}