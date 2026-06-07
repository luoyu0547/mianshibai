package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import com.mianshiba.ai.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 八股训练接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/training")
@Tag(name = "八股训练接口")
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/plan/generate")
    @Operation(summary = "生成训练计划")
    public BaseResponse<TrainingPlanVO> generatePlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestBody TrainingPlanGenerateRequest request) {
        return ResultUtils.success(trainingService.generatePlan(auth, request));
    }

    @GetMapping("/plan/active")
    @Operation(summary = "获取当前活跃训练计划")
    public BaseResponse<TrainingPlanVO> getActivePlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(trainingService.getActivePlan(auth));
    }

    @GetMapping("/plan")
    @Operation(summary = "训练计划列表")
    public BaseResponse<List<TrainingPlanVO>> listPlans(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(trainingService.listPlans(auth));
    }

    @GetMapping("/plan/{id}")
    @Operation(summary = "训练计划详情")
    public BaseResponse<TrainingPlanVO> getPlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.getPlan(auth, id));
    }

    @PutMapping("/plan/{id}/archive")
    @Operation(summary = "归档训练计划")
    public BaseResponse<Boolean> archivePlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.archivePlan(auth, id));
    }

    @PutMapping("/plan/{id}/complete")
    @Operation(summary = "完成训练计划")
    public BaseResponse<Boolean> completePlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.completePlan(auth, id));
    }

    @GetMapping("/question/{id}")
    @Operation(summary = "训练题目详情")
    public BaseResponse<TrainingQuestionVO> getQuestion(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.getQuestion(auth, id));
    }

    @PutMapping("/question/{id}/master")
    @Operation(summary = "标记题目已掌握")
    public BaseResponse<Boolean> markQuestionMastered(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.markQuestionMastered(auth, id));
    }

    @PutMapping("/question/{id}/skip")
    @Operation(summary = "跳过题目")
    public BaseResponse<Boolean> skipQuestion(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.skipQuestion(auth, id));
    }

    @PostMapping("/question/{id}/answer")
    @Operation(summary = "提交答案")
    public BaseResponse<TrainingAnswerVO> submitAnswer(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long questionId,
            @RequestBody TrainingAnswerSubmitRequest request) {
        return ResultUtils.success(trainingService.submitAnswer(auth, questionId, request));
    }

    @GetMapping("/question/{id}/answers")
    @Operation(summary = "题目答案列表")
    public BaseResponse<List<TrainingAnswerVO>> listQuestionAnswers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long questionId) {
        return ResultUtils.success(trainingService.listQuestionAnswers(auth, questionId));
    }

    @PutMapping("/algorithm/{id}/complete")
    @Operation(summary = "完成算法推荐")
    public BaseResponse<Boolean> completeAlgorithmRecommendation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.completeAlgorithmRecommendation(auth, id));
    }

    @PutMapping("/algorithm/{id}/reopen")
    @Operation(summary = "重新开启算法推荐")
    public BaseResponse<Boolean> reopenAlgorithmRecommendation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable("id") Long id) {
        return ResultUtils.success(trainingService.reopenAlgorithmRecommendation(auth, id));
    }
}
