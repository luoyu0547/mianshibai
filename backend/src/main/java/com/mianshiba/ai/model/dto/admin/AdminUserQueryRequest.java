package com.mianshiba.ai.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员用户查询请求")
public class AdminUserQueryRequest {

    @Schema(description = "关键词，匹配账号、昵称、邮箱")
    private String keyword;

    @Schema(description = "用户状态：0 正常，1 禁用")
    private Integer userStatus;

    @Schema(description = "用户角色：user/admin")
    private String userRole;
}
