package com.gk.infra.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改密码
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@Schema(title = "修改密码")
public class PasswordDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "原密码")
    @NotBlank(message="{sysuser.password.require}")
    private String password;

    @Schema(title = "新密码")
    @NotBlank(message="{sysuser.password.require}")
    @Size(min = 8, max = 72, message = "新密码长度必须为 8 到 72 位")
    private String newPassword;

    @Schema(title = "确认新密码")
    @NotBlank(message="{sysuser.password.require}")
    @Size(min = 8, max = 72, message = "确认密码长度必须为 8 到 72 位")
    private String confirmPassword;

}
