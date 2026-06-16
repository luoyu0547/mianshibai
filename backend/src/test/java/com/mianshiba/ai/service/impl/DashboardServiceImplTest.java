package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.model.entity.AlgorithmRecommendation;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.model.vo.dashboard.DashboardVO;
import com.mianshiba.ai.model.vo.training.TrainingMasterySummaryVO;
import com.mianshiba.ai.model.vo.training.TrainingMasteryVO;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;
import com.mianshiba.ai.mapper.AlgorithmRecommendationMapper;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.service.TrainingReviewService;
import com.mianshiba.ai.service.TrainingService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ApplicationTodoMapper todoMapper;
    @Mock
    private JobApplicationMapper applicationMapper;
    @Mock
    private TrainingQuestionMapper questionMapper;
    @Mock
    private TrainingAnswerReviewMapper reviewMapper;
    @Mock
    private AlgorithmRecommendationMapper algorithmMapper;
    @Mock
    private TrainingService trainingService;
    @Mock
    private TrainingReviewService trainingReviewService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private void mockUser() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));
        when(trainingReviewService.listMistakes(eq("Bearer test-token"), any())).thenReturn(List.of());
        when(trainingReviewService.listTopicMastery("Bearer test-token")).thenReturn(List.of());
        when(trainingReviewService.getMasterySummary("Bearer test-token")).thenReturn(null);
    }

    @Test
    void getDashboard_includesTodosAndPendingQuestions() {
        mockUser();

        ApplicationTodo todo = new ApplicationTodo();
        todo.setId(1L);
        todo.setTitle("跟进 HR 面试");
        todo.setCompleted(0);
        when(todoMapper.selectList(any())).thenReturn(List.of(todo));
        when(applicationMapper.selectList(any())).thenReturn(List.of());

        TrainingQuestion question = new TrainingQuestion();
        question.setId(2L);
        question.setTitle("讲讲 MySQL 索引");
        question.setStatus("pending");
        question.setUserId(1L);
        when(questionMapper.selectList(any())).thenReturn(List.of(question));
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(algorithmMapper.selectList(any())).thenReturn(List.of());
        when(trainingService.getActivePlan("Bearer test-token")).thenReturn(null);

        DashboardVO vo = dashboardService.getDashboard("Bearer test-token");

        assertThat(vo.getTodayPriorities()).contains("跟进 HR 面试");
        assertThat(vo.getPendingQuestions()).hasSize(1);
    }

    @Test
    void getDashboard_includesApplicationStats() {
        mockUser();

        JobApplication app1 = new JobApplication();
        app1.setId(1L);
        app1.setUserId(1L);
        app1.setStatus("submitted");

        JobApplication app2 = new JobApplication();
        app2.setId(2L);
        app2.setUserId(1L);
        app2.setStatus("interviewing");

        when(todoMapper.selectList(any())).thenReturn(List.of());
        when(applicationMapper.selectList(any())).thenReturn(List.of(app1, app2));
        when(questionMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(algorithmMapper.selectList(any())).thenReturn(List.of());
        when(trainingService.getActivePlan("Bearer test-token")).thenReturn(null);

        DashboardVO vo = dashboardService.getDashboard("Bearer test-token");

        assertThat(vo.getApplicationStats()).isNotNull();
        assertThat(vo.getApplicationStats().getTotal()).isEqualTo(2);
        assertThat(vo.getApplicationStats().getSubmitted()).isEqualTo(1);
        assertThat(vo.getApplicationStats().getInterviewing()).isEqualTo(1);
    }

    @Test
    void getDashboard_includesWeakTopics() {
        mockUser();

        TrainingAnswerReview review = new TrainingAnswerReview();
        review.setId(1L);
        review.setUserId(1L);
        review.setQuestionId(10L);
        review.setMasteryLevel("weak");

        TrainingQuestion reviewedQuestion = new TrainingQuestion();
        reviewedQuestion.setId(10L);
        reviewedQuestion.setTopic("MySQL 索引");

        when(todoMapper.selectList(any())).thenReturn(List.of());
        when(applicationMapper.selectList(any())).thenReturn(List.of());
        when(questionMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectList(any())).thenReturn(List.of(review));
        when(questionMapper.selectById(10L)).thenReturn(reviewedQuestion);
        when(algorithmMapper.selectList(any())).thenReturn(List.of());
        when(trainingService.getActivePlan("Bearer test-token")).thenReturn(null);

        DashboardVO vo = dashboardService.getDashboard("Bearer test-token");

        assertThat(vo.getWeakTopics()).containsExactly("MySQL 索引");
    }

    @Test
    void getDashboard_includesAlgorithmRecommendations() {
        mockUser();

        AlgorithmRecommendation rec = new AlgorithmRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setCategory("动态规划");
        rec.setPlatform("LeetCode");
        rec.setProblemRef("LC-70");
        rec.setReason("面试高频题");
        rec.setCompleted(0);

        when(todoMapper.selectList(any())).thenReturn(List.of());
        when(applicationMapper.selectList(any())).thenReturn(List.of());
        when(questionMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(algorithmMapper.selectList(any())).thenReturn(List.of(rec));
        when(trainingService.getActivePlan("Bearer test-token")).thenReturn(null);

        DashboardVO vo = dashboardService.getDashboard("Bearer test-token");

        assertThat(vo.getAlgorithmRecommendations()).hasSize(1);
        assertThat(vo.getAlgorithmRecommendations().get(0).getCategory()).isEqualTo("动态规划");
    }

    @Test
    void getDashboard_includesActivePlan() {
        mockUser();

        when(todoMapper.selectList(any())).thenReturn(List.of());
        when(applicationMapper.selectList(any())).thenReturn(List.of());
        when(questionMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(algorithmMapper.selectList(any())).thenReturn(List.of());

        var planVO = new com.mianshiba.ai.model.vo.training.TrainingPlanVO();
        planVO.setId(100L);
        planVO.setTitle("春招冲刺计划");
        when(trainingService.getActivePlan("Bearer test-token")).thenReturn(planVO);

        DashboardVO vo = dashboardService.getDashboard("Bearer test-token");

        assertThat(vo.getActivePlan()).isNotNull();
        assertThat(vo.getActivePlan().getId()).isEqualTo(100L);
    }

    @Test
    void getDashboard_includesReviewQuestionsAndMasterySummary() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));
        when(todoMapper.selectList(any())).thenReturn(List.of());
        when(applicationMapper.selectList(any())).thenReturn(List.of());
        when(questionMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(algorithmMapper.selectList(any())).thenReturn(List.of());

        TrainingMistakeVO mistake = new TrainingMistakeVO();
        mistake.setQuestionId(10L);
        mistake.setTitle("Redis 缓存穿透");
        when(trainingReviewService.listMistakes(eq("Bearer test-token"), any())).thenReturn(List.of(mistake));

        TrainingMasteryVO mastery = new TrainingMasteryVO();
        mastery.setTargetName("Redis");
        mastery.setMasteryLevel("weak");
        when(trainingReviewService.listTopicMastery("Bearer test-token")).thenReturn(List.of(mastery));

        TrainingMasterySummaryVO summary = new TrainingMasterySummaryVO();
        summary.setWeak(1L);
        summary.setBasic(0L);
        summary.setGood(0L);
        summary.setMastered(0L);
        when(trainingReviewService.getMasterySummary("Bearer test-token")).thenReturn(summary);

        DashboardVO dashboard = dashboardService.getDashboard("Bearer test-token");

        assertThat(dashboard.getReviewQuestions()).hasSize(1);
        assertThat(dashboard.getWeakMasteries()).hasSize(1);
        assertThat(dashboard.getMasterySummary().getWeak()).isEqualTo(1L);
    }
}
