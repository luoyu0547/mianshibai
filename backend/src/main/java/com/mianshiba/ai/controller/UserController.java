package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "用户接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public BaseResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResultUtils.success(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public BaseResponse<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return ResultUtils.success(userService.login(request));
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户")
    public BaseResponse<LoginUserVO> current(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(userService.getCurrentUser(authorizationHeader));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户资料")
    public BaseResponse<LoginUserVO> updateProfile(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                                   @Valid @RequestBody UserUpdateProfileRequest request) {
        return ResultUtils.success(userService.updateProfile(authorizationHeader, request));
    }
}
