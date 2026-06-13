package com.mianshiba.ai;

import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.InterviewTurnReviewMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.JobFavoriteMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.mapper.AlgorithmRecommendationMapper;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.mapper.ResumeChatMessageMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.CompanyCertificationMapper;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
        "app.infrastructure.validate-on-startup=false",
        "spring.ai.deepseek.api-key=test-api-key",
        "spring.security.jwt.secret=test-jwt-secret-key-must-be-at-least-32-bytes",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class MianshibaAiBackendApplicationTests {

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ResumeMapper resumeMapper;

    @MockBean
    private ResumeSectionMapper resumeSectionMapper;

    @MockBean
    private InterviewSessionMapper interviewSessionMapper;

    @MockBean
    private InterviewTurnMapper interviewTurnMapper;

    @MockBean
    private InterviewReportMapper interviewReportMapper;

    @MockBean
    private JobMapper jobMapper;

    @MockBean
    private JobAnalysisMapper jobAnalysisMapper;

    @MockBean
    private JobFavoriteMapper jobFavoriteMapper;

    @MockBean
    private JobMatchMapper jobMatchMapper;

    @MockBean
    private ResumeChatMessageMapper resumeChatMessageMapper;

    @MockBean
    private ResumeVersionMapper resumeVersionMapper;

    @MockBean
    private CompanyMapper companyMapper;

    @MockBean
    private CompanyCertificationMapper companyCertificationMapper;

    @MockBean
    private CoachDiagnosisMapper coachDiagnosisMapper;

    @MockBean
    private CoachPlanMapper coachPlanMapper;

    @MockBean
    private CoachTaskMapper coachTaskMapper;

    @MockBean
    private JobApplicationMapper jobApplicationMapper;

    @MockBean
    private TrainingPlanMapper trainingPlanMapper;

    @MockBean
    private TrainingAnswerMapper trainingAnswerMapper;

    @MockBean
    private TrainingAnswerReviewMapper trainingAnswerReviewMapper;

    @MockBean
    private TrainingMasteryMapper trainingMasteryMapper;

    @MockBean
    private TrainingQuestionMapper trainingQuestionMapper;

    @MockBean
    private AlgorithmRecommendationMapper algorithmRecommendationMapper;

    @MockBean
    private ApplicationTodoMapper applicationTodoMapper;

    @MockBean
    private InterviewTurnReviewMapper interviewTurnReviewMapper;

    @MockBean
    private InterviewReportEnhancementMapper interviewReportEnhancementMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}
