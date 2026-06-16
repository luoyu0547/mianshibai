package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationListQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationRoundCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationRoundResultRequest;
import com.mianshiba.ai.model.dto.application.ApplicationRoundUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationStatusUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationUpdateRequest;
import com.mianshiba.ai.model.vo.application.ApplicationRoundVO;
import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import com.mianshiba.ai.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/application")
@Tag(name = "求职投递接口")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "创建投递记录")
    public BaseResponse<JobApplicationVO> createApplication(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ApplicationCreateRequest request) {
        return ResultUtils.success(applicationService.createApplication(authorizationHeader, request));
    }

    @GetMapping
    @Operation(summary = "投递记录列表")
    public BaseResponse<List<JobApplicationVO>> listApplications(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            ApplicationListQueryRequest request) {
        return ResultUtils.success(applicationService.listApplications(authorizationHeader, request));
    }

    @GetMapping("/stats")
    @Operation(summary = "投递统计")
    public BaseResponse<ApplicationStatsVO> getStats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(applicationService.getStats(authorizationHeader));
    }

    @GetMapping("/{id}")
    @Operation(summary = "投递记录详情")
    public BaseResponse<JobApplicationVO> getApplication(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(applicationService.getApplication(authorizationHeader, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新投递记录")
    public BaseResponse<JobApplicationVO> updateApplication(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id,
            @RequestBody ApplicationUpdateRequest request) {
        return ResultUtils.success(applicationService.updateApplication(authorizationHeader, id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新投递状态")
    public BaseResponse<JobApplicationVO> updateStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResultUtils.success(applicationService.updateStatus(authorizationHeader, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除投递记录")
    public BaseResponse<Void> deleteApplication(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        applicationService.deleteApplication(authorizationHeader, id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{applicationId}/todo")
    @Operation(summary = "创建投递待办")
    public BaseResponse<ApplicationTodoVO> createApplicationTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("applicationId") Long applicationId,
            @Valid @RequestBody ApplicationTodoCreateRequest request) {
        return ResultUtils.success(applicationService.createApplicationTodo(authorizationHeader, applicationId, request));
    }

    @PostMapping("/todo")
    @Operation(summary = "创建全局待办")
    public BaseResponse<ApplicationTodoVO> createGlobalTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ApplicationTodoCreateRequest request) {
        return ResultUtils.success(applicationService.createGlobalTodo(authorizationHeader, request));
    }

    @GetMapping("/todo")
    @Operation(summary = "待办列表")
    public BaseResponse<List<ApplicationTodoVO>> listTodos(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            ApplicationTodoQueryRequest request) {
        return ResultUtils.success(applicationService.listTodos(authorizationHeader, request));
    }

    @PutMapping("/todo/{todoId}")
    @Operation(summary = "更新待办")
    public BaseResponse<ApplicationTodoVO> updateTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("todoId") Long todoId,
            @RequestBody ApplicationTodoUpdateRequest request) {
        return ResultUtils.success(applicationService.updateTodo(authorizationHeader, todoId, request));
    }

    @PutMapping("/todo/{todoId}/complete")
    @Operation(summary = "完成待办")
    public BaseResponse<ApplicationTodoVO> completeTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("todoId") Long todoId) {
        return ResultUtils.success(applicationService.completeTodo(authorizationHeader, todoId));
    }

    @PutMapping("/todo/{todoId}/reopen")
    @Operation(summary = "重新开启待办")
    public BaseResponse<ApplicationTodoVO> reopenTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("todoId") Long todoId) {
        return ResultUtils.success(applicationService.reopenTodo(authorizationHeader, todoId));
    }

    @DeleteMapping("/todo/{todoId}")
    @Operation(summary = "删除待办")
    public BaseResponse<Void> deleteTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("todoId") Long todoId) {
        applicationService.deleteTodo(authorizationHeader, todoId);
        return ResultUtils.success(null);
    }

    @GetMapping("/{id}/round")
    @Operation(summary = "获取面试轮次列表")
    public BaseResponse<List<ApplicationRoundVO>> listRounds(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(applicationService.listRounds(authorizationHeader, id));
    }

    @PostMapping("/{id}/round")
    @Operation(summary = "添加面试轮次")
    public BaseResponse<ApplicationRoundVO> createRound(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplicationRoundCreateRequest request) {
        return ResultUtils.success(applicationService.createRound(authorizationHeader, id, request));
    }

    @PutMapping("/{id}/round/{roundId}")
    @Operation(summary = "更新面试轮次")
    public BaseResponse<ApplicationRoundVO> updateRound(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @PathVariable("roundId") Long roundId,
            @RequestBody ApplicationRoundUpdateRequest request) {
        return ResultUtils.success(applicationService.updateRound(authorizationHeader, id, roundId, request));
    }

    @PutMapping("/{id}/round/{roundId}/result")
    @Operation(summary = "标记面试轮次结果")
    public BaseResponse<ApplicationRoundVO> setRoundResult(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @PathVariable("roundId") Long roundId,
            @Valid @RequestBody ApplicationRoundResultRequest request) {
        return ResultUtils.success(applicationService.setRoundResult(authorizationHeader, id, roundId, request));
    }

    @DeleteMapping("/{id}/round/{roundId}")
    @Operation(summary = "删除面试轮次")
    public BaseResponse<Void> deleteRound(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @PathVariable("roundId") Long roundId) {
        applicationService.deleteRound(authorizationHeader, id, roundId);
        return ResultUtils.success(null);
    }
}