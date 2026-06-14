package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.vo.FileUploadVO;
import com.mianshiba.ai.service.FileService;
import com.mianshiba.ai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 文件接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
@Tag(name = "文件接口", description = "文件上传相关接口")
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    @PostMapping("/avatar")
    @Operation(summary = "上传头像", description = "上传用户头像图片并返回可访问 URL")
    public BaseResponse<FileUploadVO> uploadAvatar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        // 1. 校验当前用户登录状态
        userService.getCurrentUser(authorizationHeader);

        // 2. 上传头像文件
        FileUploadVO result = fileService.uploadAvatar(file);

        // 3. 转换为完整访问 URL，便于前端直接保存与展示
        if (result.getUrl().startsWith("/")) {
            result.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(result.getUrl())
                    .toUriString());
        }
        return ResultUtils.success(result);
    }
}
