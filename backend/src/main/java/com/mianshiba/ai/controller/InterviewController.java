package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.interview.InterviewAnswerRequest;
import com.mianshiba.ai.model.dto.interview.InterviewCreateRequest;
import com.mianshiba.ai.model.vo.interview.InterviewAnswerResultVO;
import com.mianshiba.ai.model.vo.interview.InterviewQuestionVO;
import com.mianshiba.ai.model.vo.interview.InterviewReportVO;
import com.mianshiba.ai.model.vo.interview.InterviewSessionVO;
import com.mianshiba.ai.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview")
@Tag(name = "AI 模拟面试接口")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/session")
    @Operation(summary = "创建面试会话")
    public BaseResponse<InterviewSessionVO> createSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody InterviewCreateRequest request) {
        return ResultUtils.success(interviewService.createSession(authorizationHeader, request));
    }

    @PostMapping("/session/{sessionId}/start")
    @Operation(summary = "开始面试")
    public BaseResponse<InterviewQuestionVO> startSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("sessionId") Long sessionId) {
        return ResultUtils.success(interviewService.startSession(authorizationHeader, sessionId));
    }

    @PostMapping("/session/{sessionId}/turn/{turnId}/answer")
    @Operation(summary = "提交回答")
    public BaseResponse<InterviewAnswerResultVO> submitAnswer(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("turnId") Long turnId,
            @Valid @RequestBody InterviewAnswerRequest request) {
        return ResultUtils.success(interviewService.submitAnswer(authorizationHeader, sessionId, turnId, request));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取面试会话详情")
    public BaseResponse<InterviewSessionVO> getSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("sessionId") Long sessionId) {
        return ResultUtils.success(interviewService.getSession(authorizationHeader, sessionId));
    }

    @GetMapping("/session/list")
    @Operation(summary = "获取面试会话列表")
    public BaseResponse<List<InterviewSessionVO>> listSessions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(interviewService.listSessions(authorizationHeader));
    }

    @GetMapping("/session/{sessionId}/report")
    @Operation(summary = "获取面试报告")
    public BaseResponse<InterviewReportVO> getReport(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("sessionId") Long sessionId) {
        return ResultUtils.success(interviewService.getReport(authorizationHeader, sessionId));
    }

    @PostMapping("/session/{sessionId}/cancel")
    @Operation(summary = "取消面试会话")
    public BaseResponse<Void> cancelSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("sessionId") Long sessionId) {
        interviewService.cancelSession(authorizationHeader, sessionId);
        return ResultUtils.success(null);
    }
}
