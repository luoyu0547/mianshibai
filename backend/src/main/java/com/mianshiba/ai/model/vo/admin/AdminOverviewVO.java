package com.mianshiba.ai.model.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员平台总览")
public class AdminOverviewVO {

    private Long totalUsers;
    private Long enabledUsers;
    private Long disabledUsers;
    private Long adminUsers;
    private Long resumeCount;
    private Long interviewCount;
    private Long completedInterviewCount;
    private Long applicationCount;
    private Long trainingPlanCount;
    private Long trainingAnswerCount;
    private Long trainingReviewCount;
}
