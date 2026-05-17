package com.mianshiba.ai.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度必须为 4-32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须为 8-64 位")
    private String userPassword;
}
