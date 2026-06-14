package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.JobRecommendationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.JobMatch;
import com.mianshiba.ai.model.entity.JobRecommendation;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.service.ResumeJobMatchService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRecommendationServiceImplTest {

    @Mock
    private JobRecommendationMapper recommendationMapper;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private JobMatchMapper jobMatchMapper;
    @Mock
    private JobApplicationMapper applicationMapper;
    @Mock
    private ApplicationTodoMapper todoMapper;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ResumeJobMatchService resumeJobMatchService;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private JobRecommendationServiceImpl service;

    private void mockUser() {
        when(jwtUtils.resolveToken("Bearer token")).thenReturn("token");
        when(jwtUtils.parseToken("token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "user", "user"));
        User user = new User();
        user.setId(1L);
        user.setIsDelete(0);
        user.setUserStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);
    }

    @Test
    void listRecommendations_shouldReturnActiveRecommendations() {
        mockUser();

        JobRecommendation rec = new JobRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setJobId(10L);
        rec.setDismissed(0);
        rec.setApplied(0);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("测试职位");

        when(recommendationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rec));
        when(jobMapper.selectById(10L)).thenReturn(job);

        assertThat(service.listRecommendations("Bearer token")).hasSize(1);
    }

    @Test
    void refine_shouldCallResumeJobMatchAndPersist() {
        mockUser();

        JobRecommendation rec = new JobRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setJobId(10L);
        rec.setResumeId(5L);
        rec.setDismissed(0);
        rec.setApplied(0);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("测试职位");

        Resume resume = new Resume();
        resume.setId(5L);
        resume.setUserId(1L);

        JobMatch match = new JobMatch();
        match.setId(100L);

        when(recommendationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rec));
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);
        when(resumeJobMatchService.match(anyLong(), anyLong(), anyLong())).thenReturn(match);
        when(jobMapper.selectById(10L)).thenReturn(job);

        com.mianshiba.ai.model.dto.job.JobRecommendationRefineRequest request =
                new com.mianshiba.ai.model.dto.job.JobRecommendationRefineRequest();
        request.setResumeId(5L);

        service.refine("Bearer token", request);

        ArgumentCaptor<JobRecommendation> captor = ArgumentCaptor.forClass(JobRecommendation.class);
        verify(recommendationMapper).updateById(captor.capture());
        JobRecommendation updated = captor.getValue();
        assertThat(updated.getStage()).isEqualTo("refined");
        assertThat(updated.getMatchId()).isEqualTo(100L);
    }

    @Test
    void dismiss_shouldMarkDismissed() {
        mockUser();

        JobRecommendation rec = new JobRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setDismissed(0);
        when(recommendationMapper.selectById(1L)).thenReturn(rec);

        service.dismiss("Bearer token", 1L);

        ArgumentCaptor<JobRecommendation> captor = ArgumentCaptor.forClass(JobRecommendation.class);
        verify(recommendationMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDismissed()).isEqualTo(1);
    }

    @Test
    void apply_shouldCreatePendingSubmitApplicationAndTodos() {
        mockUser();

        JobRecommendation rec = new JobRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setJobId(10L);
        rec.setResumeId(5L);
        rec.setApplied(0);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("测试职位");
        job.setCompanyName("测试公司");
        job.setSourcePlatform("Boss直聘");
        job.setSalaryRange("15-25K");
        job.setCity("北京");

        when(recommendationMapper.selectById(1L)).thenReturn(rec);
        when(jobMapper.selectById(10L)).thenReturn(job);
        when(applicationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(applicationMapper.insert(any(JobApplication.class))).thenReturn(1);

        service.apply("Bearer token", 1L);

        ArgumentCaptor<JobApplication> appCaptor = ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationMapper).insert(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo("pending_submit");
        assertThat(appCaptor.getValue().getJobTitle()).isEqualTo("测试职位");

        ArgumentCaptor<ApplicationTodo> todoCaptor = ArgumentCaptor.forClass(ApplicationTodo.class);
        verify(todoMapper, org.mockito.Mockito.times(3)).insert(todoCaptor.capture());
        List<ApplicationTodo> todos = todoCaptor.getAllValues();
        assertThat(todos).hasSize(3);

        ArgumentCaptor<JobRecommendation> recCaptor = ArgumentCaptor.forClass(JobRecommendation.class);
        verify(recommendationMapper).updateById(recCaptor.capture());
        assertThat(recCaptor.getValue().getApplied()).isEqualTo(1);
    }

    @Test
    void apply_shouldReturnExistingWhenDuplicate() {
        mockUser();

        JobRecommendation rec = new JobRecommendation();
        rec.setId(1L);
        rec.setUserId(1L);
        rec.setJobId(10L);
        rec.setApplied(0);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("测试职位");
        job.setCompanyName("测试公司");

        JobApplication existingApp = new JobApplication();
        existingApp.setId(99L);
        existingApp.setUserId(1L);
        existingApp.setJobId(10L);
        existingApp.setStatus("pending_submit");

        when(recommendationMapper.selectById(1L)).thenReturn(rec);
        when(jobMapper.selectById(10L)).thenReturn(job);
        when(applicationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existingApp));

        com.mianshiba.ai.model.vo.application.JobApplicationVO result = service.apply("Bearer token", 1L);

        assertThat(result.getId()).isEqualTo(99L);
        verify(applicationMapper, never()).insert(ArgumentMatchers.any(JobApplication.class));
    }
}
