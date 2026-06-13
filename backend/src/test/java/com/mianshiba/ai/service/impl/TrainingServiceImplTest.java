package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.AlgorithmRecommendationMapper;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.entity.AlgorithmRecommendation;
import com.mianshiba.ai.model.entity.TrainingPlan;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.service.TrainingReviewService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ChatClient chatClient;
    @Mock
    private TrainingPlanMapper planMapper;
    @Mock
    private TrainingQuestionMapper questionMapper;
    @Mock
    private TrainingAnswerMapper answerMapper;
    @Mock
    private TrainingAnswerReviewMapper reviewMapper;
    @Mock
    private AlgorithmRecommendationMapper algorithmMapper;
    @Mock
    private InterviewReportEnhancementMapper enhancementMapper;
    @Mock
    private JobApplicationMapper applicationMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private TrainingReviewService trainingReviewService;

    @InjectMocks
    private TrainingServiceImpl trainingService;

    private void mockResolveUserId() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));
    }

    private void mockResolveOtherUser() {
        when(jwtUtils.resolveToken("Bearer other-token")).thenReturn("other-token");
        when(jwtUtils.parseToken("other-token")).thenReturn(new JwtUtils.JwtUserClaims(2L, "otheruser", "user"));
    }

    @Test
    void markQuestionMastered_updatesOwnedQuestion() {
        mockResolveUserId();

        TrainingQuestion question = new TrainingQuestion();
        question.setId(1L);
        question.setUserId(1L);
        question.setStatus("reviewed");
        when(questionMapper.selectById(1L)).thenReturn(question);

        trainingService.markQuestionMastered("Bearer test-token", 1L);

        ArgumentCaptor<TrainingQuestion> captor = ArgumentCaptor.forClass(TrainingQuestion.class);
        verify(questionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("mastered");
    }

    @Test
    void getQuestion_rejectsOtherUsersQuestion() {
        mockResolveUserId();

        TrainingQuestion question = new TrainingQuestion();
        question.setId(1L);
        question.setUserId(2L);
        when(questionMapper.selectById(1L)).thenReturn(question);

        assertThatThrownBy(() -> trainingService.getQuestion("Bearer test-token", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.TRAINING_QUESTION_NOT_FOUND_ERROR.getCode());
    }

    @Test
    void completeAlgorithmRecommendation_updatesOwnedRecommendation() {
        mockResolveUserId();

        AlgorithmRecommendation rec = new AlgorithmRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setCompleted(0);
        when(algorithmMapper.selectById(1L)).thenReturn(rec);

        trainingService.completeAlgorithmRecommendation("Bearer test-token", 1L);

        ArgumentCaptor<AlgorithmRecommendation> captor = ArgumentCaptor.forClass(AlgorithmRecommendation.class);
        verify(algorithmMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCompleted()).isEqualTo(1);
    }

    @Test
    void archivePlan_setsStatusToArchived() {
        mockResolveUserId();

        TrainingPlan plan = new TrainingPlan();
        plan.setId(1L);
        plan.setUserId(1L);
        plan.setStatus("active");
        when(planMapper.selectById(1L)).thenReturn(plan);

        trainingService.archivePlan("Bearer test-token", 1L);

        ArgumentCaptor<TrainingPlan> captor = ArgumentCaptor.forClass(TrainingPlan.class);
        verify(planMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("archived");
    }

    @Test
    void listPlans_returnsOnlyCurrentUserPlans() {
        mockResolveUserId();

        TrainingPlan plan1 = new TrainingPlan();
        plan1.setId(1L);
        plan1.setUserId(1L);
        plan1.setStatus("active");

        when(planMapper.selectList(any())).thenReturn(List.of(plan1));

        var result = trainingService.listPlans("Bearer test-token");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void submitAnswer_rejectsBlankAnswer() {
        mockResolveUserId();

        var request = new com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest();
        request.setAnswerText("");

        assertThatThrownBy(() -> trainingService.submitAnswer("Bearer test-token", 1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void submitAnswer_rejectsTooLongAnswer() {
        mockResolveUserId();

        var request = new com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest();
        request.setAnswerText("a".repeat(8001));

        assertThatThrownBy(() -> trainingService.submitAnswer("Bearer test-token", 1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }
}
