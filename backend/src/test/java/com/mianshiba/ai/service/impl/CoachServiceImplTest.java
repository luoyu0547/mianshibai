package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.CoachDiagnosis;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.CoachPlan;
import com.mianshiba.ai.model.entity.CoachTask;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoachServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ChatClient chatClient;
    @Mock
    private CoachDiagnosisMapper diagnosisMapper;
    @Mock
    private CoachPlanMapper planMapper;
    @Mock
    private CoachTaskMapper taskMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeSectionMapper resumeSectionMapper;
    @Mock
    private InterviewSessionMapper interviewSessionMapper;
    @Mock
    private InterviewReportMapper interviewReportMapper;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private TrainingPlanMapper trainingPlanMapper;
    @Mock
    private TrainingAnswerMapper trainingAnswerMapper;
    @Mock
    private TrainingMasteryMapper trainingMasteryMapper;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @InjectMocks
    private CoachServiceImpl coachService;

    private void mockAuth() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "test", "user"));
    }

    private void mockUser() {
        mockAuth();
        User user = new User();
        user.setId(1L);
        user.setTargetPosition("Java 后端开发");
        user.setTechDirection("Java/Spring Boot");
        user.setJobStatus("looking");
        when(userMapper.selectById(1L)).thenReturn(user);
    }

    private void mockBusinessCounts(long resumeCount, long interviewCount, long applicationCount,
                                    long trainingPlanCount, long trainingAnswerCount) {
        when(resumeMapper.selectList(any())).thenReturn(List.of());
        when(interviewSessionMapper.selectCount(any())).thenReturn(interviewCount);
        when(jobApplicationMapper.selectCount(any())).thenReturn(applicationCount);
        when(trainingPlanMapper.selectCount(any())).thenReturn(trainingPlanCount);
        when(trainingAnswerMapper.selectCount(any())).thenReturn(trainingAnswerCount);
        when(trainingMasteryMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void generate_fallsBackAndPersistsDiagnosisPlanTasksWhenAiFails() {
        mockUser();
        mockBusinessCounts(0L, 0L, 0L, 0L, 0L);
        CoachGenerateRequest request = new CoachGenerateRequest();
        request.setFocus("准备后端面试");

        var result = coachService.generate("Bearer test-token", request);

        assertThat(result.getDiagnosis().getSource()).isEqualTo("fallback");
        assertThat(result.getPlan().getSource()).isEqualTo("fallback");
        assertThat(result.getPlan().getTasks()).hasSize(7);
        assertThat(result.getPlan().getTasks().get(0).getTitle()).isEqualTo("创建简历");
        verify(planMapper).update(any(), any());
        verify(diagnosisMapper).insert(any(CoachDiagnosis.class));
        verify(planMapper).insert(any(CoachPlan.class));
        verify(taskMapper, times(7)).insert(any(CoachTask.class));
    }

    @Test
    void generate_acceptsFlatAiResponseAndKeepsAnalysisItems() {
        mockUser();
        mockResumeAndCompletedInterview();
        when(jobApplicationMapper.selectCount(any())).thenReturn(0L);
        when(trainingPlanMapper.selectCount(any())).thenReturn(0L);
        when(trainingAnswerMapper.selectCount(any())).thenReturn(0L);
        when(trainingMasteryMapper.selectList(any())).thenReturn(List.of());
        mockAiResponse("""
                ```json
                {
                  "title": "Java 后端开发求职诊断",
                  "overallScore": 82,
                  "summary": "简历和模拟面试数据足够支撑诊断，项目深度仍需加强。",
                  "strengths": ["已完成简历", "已完成模拟面试"],
                  "weaknesses": ["项目深度表达不足"],
                  "suggestions": ["补充项目技术难点", "复盘面试报告低分维度"],
                  "planTitle": "7 天 Java 后端提升计划",
                  "planSummary": "围绕简历项目和面试深度提升。",
                  "tasks": [
                    {"dayIndex": 1, "title": "复盘面试报告", "description": "整理低分维度。", "taskType": "interview", "priority": "high"},
                    {"dayIndex": 2, "title": "补充项目难点", "description": "完善项目技术难点。", "taskType": "resume", "priority": "high"},
                    {"dayIndex": 3, "title": "准备 STAR 回答", "description": "结构化项目表达。", "taskType": "interview", "priority": "medium"},
                    {"dayIndex": 4, "title": "优化技能关键词", "description": "匹配 Java 后端岗位。", "taskType": "resume", "priority": "medium"},
                    {"dayIndex": 5, "title": "模拟追问练习", "description": "围绕项目细节追问。", "taskType": "interview", "priority": "medium"},
                    {"dayIndex": 6, "title": "投递岗位筛选", "description": "筛选匹配岗位。", "taskType": "application", "priority": "medium"},
                    {"dayIndex": 7, "title": "总结一周改进", "description": "形成复盘清单。", "taskType": "habit", "priority": "low"}
                  ]
                }
                ```
                """);

        var result = coachService.generate("Bearer test-token", new CoachGenerateRequest());

        assertThat(result.getDiagnosis().getOverallScore()).isEqualTo(82);
        assertThat(result.getDiagnosis().getStrengths()).contains("已完成简历", "已完成模拟面试");
        assertThat(result.getDiagnosis().getWeaknesses()).contains("项目深度表达不足");
        assertThat(result.getDiagnosis().getSuggestions()).contains("补充项目技术难点", "复盘面试报告低分维度");
        assertThat(result.getDiagnosis().getDataCompleteness()).isGreaterThan(60);
    }

    @Test
    void completeTask_marksTaskCompletedAndCompletesPlanWhenAllTasksDone() {
        mockAuth();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(1L);
        plan.setStatus("active");
        CoachTask task = new CoachTask();
        task.setId(20L);
        task.setUserId(1L);
        task.setPlanId(10L);
        task.setStatus("pending");
        when(taskMapper.selectById(20L)).thenReturn(task);
        when(planMapper.selectById(10L)).thenReturn(plan);
        when(taskMapper.selectCount(any())).thenReturn(0L);

        var result = coachService.completeTask("Bearer test-token", 20L);

        assertThat(result.getStatus()).isEqualTo("completed");
        ArgumentCaptor<CoachPlan> planCaptor = ArgumentCaptor.forClass(CoachPlan.class);
        verify(planMapper).updateById(planCaptor.capture());
        assertThat(planCaptor.getValue().getStatus()).isEqualTo("completed");
    }

    @Test
    void reopenTask_reopensCompletedPlan() {
        mockAuth();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(1L);
        plan.setStatus("completed");
        CoachTask task = new CoachTask();
        task.setId(20L);
        task.setUserId(1L);
        task.setPlanId(10L);
        task.setStatus("completed");
        when(taskMapper.selectById(20L)).thenReturn(task);
        when(planMapper.selectById(10L)).thenReturn(plan);

        var result = coachService.reopenTask("Bearer test-token", 20L);

        assertThat(result.getStatus()).isEqualTo("pending");
        ArgumentCaptor<CoachPlan> planCaptor = ArgumentCaptor.forClass(CoachPlan.class);
        verify(planMapper).updateById(planCaptor.capture());
        assertThat(planCaptor.getValue().getStatus()).isEqualTo("active");
    }

    @Test
    void getPlan_rejectsOtherUsersPlan() {
        mockAuth();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(2L);
        when(planMapper.selectById(10L)).thenReturn(plan);

        assertThatThrownBy(() -> coachService.getPlan("Bearer test-token", 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
    }

    @Test
    void listPlans_returnsCurrentUserPlans() {
        mockAuth();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(1L);
        plan.setStatus("active");
        when(planMapper.selectList(any())).thenReturn(List.of(plan));
        when(taskMapper.selectList(any())).thenReturn(List.of());

        var result = coachService.listPlans("Bearer test-token");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    private void mockAiResponse(String response) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }

    private void mockResumeAndCompletedInterview() {
        Resume resume = new Resume();
        resume.setId(10L);
        resume.setUserId(1L);
        resume.setTitle("admin 的 Java 后端简历");
        resume.setUpdateTime(LocalDateTime.now());
        when(resumeMapper.selectList(any())).thenReturn(List.of(resume));

        ResumeSection skills = new ResumeSection();
        skills.setResumeId(10L);
        skills.setSectionType("skills");
        skills.setSortOrder(1);
        skills.setSectionData(Map.of("skills", List.of("Java", "Spring Boot", "MySQL")));
        ResumeSection project = new ResumeSection();
        project.setResumeId(10L);
        project.setSectionType("projects");
        project.setSortOrder(2);
        project.setSectionData(Map.of("name", "面试吧", "role", "后端开发", "description", "负责求职诊断和模拟面试模块"));
        when(resumeSectionMapper.selectList(any())).thenReturn(List.of(skills, project));

        InterviewSession session = new InterviewSession();
        session.setId(20L);
        session.setUserId(1L);
        session.setStatus("completed");
        session.setEndedAt(LocalDateTime.now());
        when(interviewSessionMapper.selectCount(any())).thenReturn(1L, 1L);
        when(interviewSessionMapper.selectList(any())).thenReturn(List.of(session));

        InterviewReport report = new InterviewReport();
        report.setSessionId(20L);
        report.setTotalScore(76);
        report.setAccuracyScore(80);
        report.setClarityScore(72);
        report.setDepthScore(64);
        report.setMatchingScore(78);
        report.setSummary("基础较好，项目深度表达不足。");
        report.setSuggestions(List.of("加强项目难点表达", "补充量化结果"));
        when(interviewReportMapper.selectList(any())).thenReturn(List.of(report));
    }
}
