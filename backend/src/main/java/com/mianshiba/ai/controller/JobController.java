package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.job.JobImportRequest;
import com.mianshiba.ai.model.dto.job.JobListQueryRequest;
import com.mianshiba.ai.model.dto.job.JobMatchRequest;
import com.mianshiba.ai.model.vo.job.CompanyVO;
import com.mianshiba.ai.model.vo.job.JobGapAnalysisVO;
import com.mianshiba.ai.model.vo.job.JobImportResultVO;
import com.mianshiba.ai.model.vo.job.JobKeywordVO;
import com.mianshiba.ai.model.vo.job.JobMatchVO;
import com.mianshiba.ai.model.vo.job.JobQuestionPredictionVO;
import com.mianshiba.ai.model.vo.job.JobVO;
import com.mianshiba.ai.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/job")
@Tag(name = "职位接口")
public class JobController {

    private final JobService jobService;

    @PostMapping("/import-url")
    @Operation(summary = "解析职位/公司链接")
    public BaseResponse<JobImportResultVO> importUrl(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody JobImportRequest request) {
        return ResultUtils.success(jobService.importUrl(authorizationHeader, request));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "获取职位详情")
    public BaseResponse<JobVO> getJob(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId) {
        return ResultUtils.success(jobService.getJob(authorizationHeader, jobId));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "获取公司画像")
    public BaseResponse<CompanyVO> getCompany(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("companyId") Long companyId) {
        return ResultUtils.success(jobService.getCompany(authorizationHeader, companyId));
    }

    @PostMapping("/{jobId}/match")
    @Operation(summary = "分析职位与简历匹配")
    public BaseResponse<JobMatchVO> matchJob(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId,
            @Valid @RequestBody JobMatchRequest request) {
        return ResultUtils.success(jobService.matchJob(authorizationHeader, jobId, request));
    }

    @PostMapping("/{jobId}/favorite")
    @Operation(summary = "收藏职位")
    public BaseResponse<Void> favoriteJob(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId) {
        jobService.favoriteJob(authorizationHeader, jobId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{jobId}/favorite")
    @Operation(summary = "取消收藏")
    public BaseResponse<Void> unfavoriteJob(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId) {
        jobService.unfavoriteJob(authorizationHeader, jobId);
        return ResultUtils.success(null);
    }

    @GetMapping("/favorites")
    @Operation(summary = "收藏职位列表")
    public BaseResponse<List<JobVO>> listFavorites(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(jobService.listFavorites(authorizationHeader));
    }

    @GetMapping("/list")
    @Operation(summary = "职位列表")
    public BaseResponse<List<JobVO>> listJobs(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            JobListQueryRequest request) {
        return ResultUtils.success(jobService.listJobs(authorizationHeader, request));
    }

    @PostMapping("/{jobId}/keywords")
    @Operation(summary = "JD 关键词提取")
    public BaseResponse<JobKeywordVO> extractKeywords(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId) {
        return ResultUtils.success(jobService.extractKeywords(authorizationHeader, jobId));
    }

    @PostMapping("/{jobId}/gap")
    @Operation(summary = "简历差距分析")
    public BaseResponse<JobGapAnalysisVO> analyzeGap(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId,
            @RequestParam("resumeId") Long resumeId) {
        return ResultUtils.success(jobService.analyzeGap(authorizationHeader, jobId, resumeId));
    }

    @PostMapping("/{jobId}/questions")
    @Operation(summary = "预测面试题")
    public BaseResponse<JobQuestionPredictionVO> predictQuestions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("jobId") Long jobId) {
        return ResultUtils.success(jobService.predictQuestions(authorizationHeader, jobId));
    }
}
