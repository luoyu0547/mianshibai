package com.mianshiba.ai.model.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理员用户详情")
public class AdminUserDetailVO {

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Integer userStatus;
    private String email;
    private String phone;
    private String targetPosition;
    private String techDirection;
    private Integer workYears;
    private String city;
    private String jobStatus;
    private LocalDateTime createTime;
    private Long resumeCount;
    private Long interviewCount;
    private Long completedInterviewCount;
    private Long applicationCount;
    private Long trainingPlanCount;
    private Long trainingAnswerCount;
    private Long trainingReviewCount;
}
