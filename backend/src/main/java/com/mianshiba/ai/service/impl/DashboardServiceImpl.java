package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.AlgorithmRecommendationMapper;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.entity.AlgorithmRecommendation;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.dashboard.DashboardVO;
import com.mianshiba.ai.model.vo.training.AlgorithmRecommendationVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import com.mianshiba.ai.service.DashboardService;
import com.mianshiba.ai.service.TrainingService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Set<String> INTERVIEWING_STATUSES = Set.of(
            "hr_contact", "written_test", "first_interview", "second_interview", "final_interview");

    private final JwtUtils jwtUtils;
    private final ApplicationTodoMapper todoMapper;
    private final JobApplicationMapper applicationMapper;
    private final TrainingQuestionMapper questionMapper;
    private final TrainingAnswerReviewMapper reviewMapper;
    private final AlgorithmRecommendationMapper algorithmMapper;
    private final TrainingService trainingService;

    @Override
    public DashboardVO getDashboard(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);

        DashboardVO dashboard = new DashboardVO();
        dashboard.setTodayPriorities(buildTodayPriorities(userId));
        dashboard.setApplicationStats(buildApplicationStats(userId));
        dashboard.setActivePlan(buildActivePlan(authorizationHeader));
        dashboard.setPendingQuestions(buildPendingQuestions(userId));
        dashboard.setWeakTopics(buildWeakTopics(userId));
        dashboard.setAlgorithmRecommendations(buildAlgorithmRecommendations(userId));
        return dashboard;
    }

    private List<String> buildTodayPriorities(Long userId) {
        List<ApplicationTodo> todos = todoMapper.selectList(
                Wrappers.lambdaQuery(ApplicationTodo.class)
                        .eq(ApplicationTodo::getUserId, userId)
                        .eq(ApplicationTodo::getCompleted, 0)
                        .orderByAsc(ApplicationTodo::getDueAt)
                        .orderByDesc(ApplicationTodo::getPriority)
                        .last("LIMIT 5"));
        return todos.stream().map(ApplicationTodo::getTitle).collect(Collectors.toList());
    }

    private ApplicationStatsVO buildApplicationStats(Long userId) {
        List<JobApplication> applications = applicationMapper.selectList(
                Wrappers.lambdaQuery(JobApplication.class)
                        .eq(JobApplication::getUserId, userId));

        Map<String, Long> statusCount = applications.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getStatus() != null ? app.getStatus() : "pending_submit",
                        Collectors.counting()));

        ApplicationStatsVO stats = new ApplicationStatsVO();
        stats.setTotal((long) applications.size());
        stats.setPendingSubmit(statusCount.getOrDefault("pending_submit", 0L));
        stats.setSubmitted(statusCount.getOrDefault("submitted", 0L));
        stats.setInterviewing(INTERVIEWING_STATUSES.stream()
                .mapToLong(s -> statusCount.getOrDefault(s, 0L))
                .sum());
        stats.setOffer(statusCount.getOrDefault("offer", 0L));
        stats.setClosed(statusCount.getOrDefault("rejected", 0L) + statusCount.getOrDefault("withdrawn", 0L));
        return stats;
    }

    private TrainingPlanVO buildActivePlan(String authorizationHeader) {
        try {
            return trainingService.getActivePlan(authorizationHeader);
        } catch (Exception e) {
            log.warn("获取活跃训练计划失败: {}", e.getMessage());
            return null;
        }
    }

    private List<TrainingQuestionVO> buildPendingQuestions(Long userId) {
        List<TrainingQuestion> questions = questionMapper.selectList(
                Wrappers.lambdaQuery(TrainingQuestion.class)
                        .eq(TrainingQuestion::getUserId, userId)
                        .in(TrainingQuestion::getStatus, "pending", "answered")
                        .orderByAsc(TrainingQuestion::getPlanId)
                        .orderByAsc(TrainingQuestion::getDayIndex)
                        .last("LIMIT 5"));
        return questions.stream()
                .map(this::toLightweightQuestionVO)
                .collect(Collectors.toList());
    }

    private List<String> buildWeakTopics(Long userId) {
        List<TrainingAnswerReview> reviews = reviewMapper.selectList(
                Wrappers.lambdaQuery(TrainingAnswerReview.class)
                        .eq(TrainingAnswerReview::getUserId, userId)
                        .in(TrainingAnswerReview::getMasteryLevel, "weak", "basic")
                        .orderByDesc(TrainingAnswerReview::getCreateTime)
                        .last("LIMIT 20"));

        Map<String, Long> topicCounts = new LinkedHashMap<>();
        for (TrainingAnswerReview review : reviews) {
            TrainingQuestion question = questionMapper.selectById(review.getQuestionId());
            if (question != null && question.getTopic() != null) {
                topicCounts.merge(question.getTopic(), 1L, Long::sum);
            }
        }

        return topicCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<AlgorithmRecommendationVO> buildAlgorithmRecommendations(Long userId) {
        List<AlgorithmRecommendation> recommendations = algorithmMapper.selectList(
                Wrappers.lambdaQuery(AlgorithmRecommendation.class)
                        .eq(AlgorithmRecommendation::getUserId, userId)
                        .eq(AlgorithmRecommendation::getCompleted, 0)
                        .last("LIMIT 5"));
        return recommendations.stream()
                .map(this::toAlgorithmRecommendationVO)
                .collect(Collectors.toList());
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        return claims.userId();
    }

    private TrainingQuestionVO toLightweightQuestionVO(TrainingQuestion question) {
        TrainingQuestionVO vo = new TrainingQuestionVO();
        vo.setId(question.getId());
        vo.setPlanId(question.getPlanId());
        vo.setDayIndex(question.getDayIndex());
        vo.setTitle(question.getTitle());
        vo.setTopic(question.getTopic());
        vo.setDifficulty(question.getDifficulty());
        vo.setStatus(question.getStatus());
        return vo;
    }

    private AlgorithmRecommendationVO toAlgorithmRecommendationVO(AlgorithmRecommendation rec) {
        AlgorithmRecommendationVO vo = new AlgorithmRecommendationVO();
        vo.setId(rec.getId());
        vo.setPlanId(rec.getPlanId());
        vo.setCategory(rec.getCategory());
        vo.setPlatform(rec.getPlatform());
        vo.setProblemRef(rec.getProblemRef());
        vo.setReason(rec.getReason());
        vo.setCompleted(rec.getCompleted());
        return vo;
    }
}
