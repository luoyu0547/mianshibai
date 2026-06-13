package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;
import com.mianshiba.ai.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "管理员后台接口")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/overview")
    @Operation(summary = "平台总览")
    public BaseResponse<AdminOverviewVO> getOverview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(adminService.getOverview(authorizationHeader));
    }

    @GetMapping("/users")
    @Operation(summary = "管理员用户列表")
    public BaseResponse<List<AdminUserListItemVO>> listUsers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            AdminUserQueryRequest request) {
        return ResultUtils.success(adminService.listUsers(authorizationHeader, request));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "管理员用户详情")
    public BaseResponse<AdminUserDetailVO> getUserDetail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminService.getUserDetail(authorizationHeader, id));
    }

    @PutMapping("/users/{id}/disable")
    @Operation(summary = "禁用用户")
    public BaseResponse<AdminUserDetailVO> disableUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminService.disableUser(authorizationHeader, id));
    }

    @PutMapping("/users/{id}/enable")
    @Operation(summary = "启用用户")
    public BaseResponse<AdminUserDetailVO> enableUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminService.enableUser(authorizationHeader, id));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "更新用户角色")
    public BaseResponse<AdminUserDetailVO> updateUserRole(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return ResultUtils.success(adminService.updateUserRole(authorizationHeader, id, request));
    }
}
