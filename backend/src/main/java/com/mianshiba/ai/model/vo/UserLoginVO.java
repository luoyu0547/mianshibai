package com.mianshiba.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class UserLoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;

    private LoginUserVO user;
}
