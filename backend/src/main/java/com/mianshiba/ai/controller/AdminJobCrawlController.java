package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskCreateRequest;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskQueryRequest;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskUpdateRequest;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlItemVO;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlRunVO;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlTaskVO;
import com.mianshiba.ai.service.AdminJobCrawlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/api/admin/job-crawl")
@Tag(name = "管理员职位采集接口")
public class AdminJobCrawlController {
    private final AdminJobCrawlService adminJobCrawlService;

    @PostMapping("/tasks")
    @Operation(summary = "创建采集任务")
    public BaseResponse<AdminJobCrawlTaskVO> createTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody AdminJobCrawlTaskCreateRequest request) {
        return ResultUtils.success(adminJobCrawlService.createTask(authorizationHeader, request));
    }

    @GetMapping("/tasks")
    @Operation(summary = "查询采集任务列表")
    public BaseResponse<List<AdminJobCrawlTaskVO>> listTasks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            AdminJobCrawlTaskQueryRequest request) {
        return ResultUtils.success(adminJobCrawlService.listTasks(authorizationHeader, request));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "查询采集任务详情")
    public BaseResponse<AdminJobCrawlTaskVO> getTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminJobCrawlService.getTask(authorizationHeader, id));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "更新采集任务")
    public BaseResponse<AdminJobCrawlTaskVO> updateTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminJobCrawlTaskUpdateRequest request) {
        return ResultUtils.success(adminJobCrawlService.updateTask(authorizationHeader, id, request));
    }

    @PutMapping("/tasks/{id}/enable")
    @Operation(summary = "启用任务")
    public BaseResponse<AdminJobCrawlTaskVO> enableTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminJobCrawlService.enableTask(authorizationHeader, id));
    }

    @PutMapping("/tasks/{id}/disable")
    @Operation(summary = "禁用任务")
    public BaseResponse<AdminJobCrawlTaskVO> disableTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminJobCrawlService.disableTask(authorizationHeader, id));
    }

    @PostMapping("/tasks/{id}/run")
    @Operation(summary = "立即运行")
    public BaseResponse<AdminJobCrawlRunVO> runTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminJobCrawlService.runTask(authorizationHeader, id));
    }

    @GetMapping("/tasks/{id}/runs")
    @Operation(summary = "查询运行记录")
    public BaseResponse<List<AdminJobCrawlRunVO>> listTaskRuns(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long taskId) {
        return ResultUtils.success(adminJobCrawlService.listTaskRuns(authorizationHeader, taskId));
    }

    @GetMapping("/runs/{runId}/items")
    @Operation(summary = "查询单次运行明细")
    public BaseResponse<List<AdminJobCrawlItemVO>> listRunItems(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("runId") Long runId) {
        return ResultUtils.success(adminJobCrawlService.listRunItems(authorizationHeader, runId));
    }
}
