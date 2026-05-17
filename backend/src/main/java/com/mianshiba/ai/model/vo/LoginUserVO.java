package com.mianshiba.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录用户脱敏信息
 */
@Data
@Schema(description = "登录用户脱敏信息")
public class LoginUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Integer userStatus;
    private String email;
    private String phone;
    private String targetPosition;
    private String techDirection;
    private Integer workYears;
    private String city;
    private String jobStatus;
    private LocalDateTime createTime;
}
