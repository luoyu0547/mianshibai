package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.vo.coach.CoachDiagnosisVO;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.model.vo.coach.CoachPlanVO;
import com.mianshiba.ai.model.vo.coach.CoachTaskVO;
import com.mianshiba.ai.service.CoachService;
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
@RequestMapping("/api/coach")
@Tag(name = "AI 求职教练接口")
public class CoachController {

    private final CoachService coachService;

    @PostMapping("/generate")
    @Operation(summary = "生成求职诊断和 7 天计划")
    public BaseResponse<CoachGenerateResultVO> generate(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                        @Valid @RequestBody CoachGenerateRequest request) {
        return ResultUtils.success(coachService.generate(auth, request));
    }

    @GetMapping("/overview")
    @Operation(summary = "求职教练总览")
    public BaseResponse<CoachOverviewVO> getOverview(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(coachService.getOverview(auth));
    }

    @GetMapping("/diagnoses")
    @Operation(summary = "诊断历史")
    public BaseResponse<List<CoachDiagnosisVO>> listDiagnoses(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(coachService.listDiagnoses(auth));
    }

    @GetMapping("/diagnoses/{id}")
    @Operation(summary = "诊断详情")
    public BaseResponse<CoachDiagnosisVO> getDiagnosis(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                       @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.getDiagnosis(auth, id));
    }

    @GetMapping("/plans")
    @Operation(summary = "教练计划历史")
    public BaseResponse<List<CoachPlanVO>> listPlans(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(coachService.listPlans(auth));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "教练计划详情")
    public BaseResponse<CoachPlanVO> getPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                             @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.getPlan(auth, id));
    }

    @PutMapping("/tasks/{id}/complete")
    @Operation(summary = "完成教练任务")
    public BaseResponse<CoachTaskVO> completeTask(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                  @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.completeTask(auth, id));
    }

    @PutMapping("/tasks/{id}/reopen")
    @Operation(summary = "重开教练任务")
    public BaseResponse<CoachTaskVO> reopenTask(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.reopenTask(auth, id));
    }
}
