package com.mianshiba.ai;

import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobFavoriteMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.mapper.ResumeChatMessageMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.CompanyCertificationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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

    @Test
    void contextLoads() {
    }
}
