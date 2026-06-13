package com.mianshiba.ai.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员更新用户角色请求")
public class AdminUserRoleUpdateRequest {

    @NotBlank(message = "用户角色不能为空")
    @Schema(description = "用户角色：user/admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userRole;
}
