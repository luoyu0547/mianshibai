package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.training.TrainingMistakeQueryRequest;
import com.mianshiba.ai.model.vo.training.TrainingMasterySummaryVO;
import com.mianshiba.ai.model.vo.training.TrainingMasteryVO;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;
import com.mianshiba.ai.service.TrainingReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/training/review")
@Tag(name = "八股复习接口")
public class TrainingReviewController {
    private final TrainingReviewService trainingReviewService;

    @GetMapping("/mistakes")
    @Operation(summary = "查询错题本")
    public BaseResponse<List<TrainingMistakeVO>> listMistakes(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            TrainingMistakeQueryRequest request) {
        return ResultUtils.success(trainingReviewService.listMistakes(authorizationHeader, request));
    }

    @GetMapping("/mastery")
    @Operation(summary = "查询 topic 掌握度")
    public BaseResponse<List<TrainingMasteryVO>> listTopicMastery(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.listTopicMastery(authorizationHeader));
    }

    @GetMapping("/mastery/tags")
    @Operation(summary = "查询 skillTag 掌握度")
    public BaseResponse<List<TrainingMasteryVO>> listSkillTagMastery(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.listSkillTagMastery(authorizationHeader));
    }

    @GetMapping("/mastery/summary")
    @Operation(summary = "查询掌握度摘要")
    public BaseResponse<TrainingMasterySummaryVO> getMasterySummary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.getMasterySummary(authorizationHeader));
    }

    @PostMapping("/mastery/rebuild")
    @Operation(summary = "重建掌握度统计")
    public BaseResponse<Boolean> rebuildMastery(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.rebuildMastery(authorizationHeader));
    }
}
