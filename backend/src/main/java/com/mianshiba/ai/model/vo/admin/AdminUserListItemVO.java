package com.mianshiba.ai.model.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理员用户列表项")
public class AdminUserListItemVO {

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Integer userStatus;
    private String email;
    private String targetPosition;
    private String techDirection;
    private String city;
    private LocalDateTime createTime;
}
