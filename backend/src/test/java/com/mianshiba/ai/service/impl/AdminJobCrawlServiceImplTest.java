package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskCreateRequest;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlTaskVO;
import com.mianshiba.ai.service.JobBatchCrawlService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminJobCrawlServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JobCrawlTaskMapper jobCrawlTaskMapper;
    @Mock
    private JobCrawlRunMapper jobCrawlRunMapper;
    @Mock
    private JobCrawlItemMapper jobCrawlItemMapper;
    @Mock
    private JobBatchCrawlService jobBatchCrawlService;

    @InjectMocks
    private AdminJobCrawlServiceImpl adminJobCrawlService;

    @Test
    void createTask_shouldRequireAdminAndPersistDisabledTask() {
        mockAdmin();

        AdminJobCrawlTaskCreateRequest request = new AdminJobCrawlTaskCreateRequest();
        request.setName("测试采集任务");
        request.setSourceType("company_career_page");
        request.setSourceUrl("https://example.com/careers");
        request.setKeywords("Java,Spring");
        request.setScheduleType("manual");

        ArgumentCaptor<JobCrawlTask> captor = ArgumentCaptor.forClass(JobCrawlTask.class);

        adminJobCrawlService.createTask("Bearer token", request);

        verify(jobCrawlTaskMapper).insert(captor.capture());
        JobCrawlTask inserted = captor.getValue();
        assertThat(inserted.getName()).isEqualTo("测试采集任务");
        assertThat(inserted.getSourceType()).isEqualTo("company_career_page");
        assertThat(inserted.getStatus()).isEqualTo("disabled");
        assertThat(inserted.getCreatedBy()).isEqualTo(1L);
    }

    @Test
    void enableTask_shouldSetEnabledAndNextRunAt() {
        mockAdmin();
        JobCrawlTask task = existingTask(1L, "disabled");
        when(jobCrawlTaskMapper.selectById(1L)).thenReturn(task);

        adminJobCrawlService.enableTask("Bearer token", 1L);

        assertThat(task.getStatus()).isEqualTo("enabled");
        assertThat(task.getNextRunAt()).isNotNull();
        verify(jobCrawlTaskMapper).updateById(task);
    }

    @Test
    void disableTask_shouldSetDisabled() {
        mockAdmin();
        JobCrawlTask task = existingTask(1L, "enabled");
        when(jobCrawlTaskMapper.selectById(1L)).thenReturn(task);

        adminJobCrawlService.disableTask("Bearer token", 1L);

        assertThat(task.getStatus()).isEqualTo("disabled");
        verify(jobCrawlTaskMapper).updateById(task);
    }

    @Test
    void createTask_shouldRejectInvalidSourceType() {
        mockAdmin();

        AdminJobCrawlTaskCreateRequest request = new AdminJobCrawlTaskCreateRequest();
        request.setName("测试");
        request.setSourceType("invalid_type");
        request.setSourceUrl("https://example.com");
        request.setScheduleType("manual");

        assertThatThrownBy(() -> adminJobCrawlService.createTask("Bearer token", request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void createTask_shouldRejectNonAdminUser() {
        mockClaims(2L, "user");
        User user = normalUser(2L);
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminJobCrawlTaskCreateRequest request = new AdminJobCrawlTaskCreateRequest();
        request.setName("测试");
        request.setSourceType("company_career_page");
        request.setSourceUrl("https://example.com");
        request.setScheduleType("manual");

        assertThatThrownBy(() -> adminJobCrawlService.createTask("Bearer token", request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
    }

    @Test
    void createTask_shouldRejectInvalidScheduleType() {
        mockAdmin();

        AdminJobCrawlTaskCreateRequest request = new AdminJobCrawlTaskCreateRequest();
        request.setName("测试");
        request.setSourceType("public_feed");
        request.setSourceUrl("https://example.com");
        request.setScheduleType("invalid_schedule");

        assertThatThrownBy(() -> adminJobCrawlService.createTask("Bearer token", request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void listTasks_shouldReturnTaskList() {
        mockAdmin();
        JobCrawlTask task = existingTask(1L, "enabled");
        when(jobCrawlTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));

        List<AdminJobCrawlTaskVO> tasks = adminJobCrawlService.listTasks("Bearer token", null);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getId()).isEqualTo(1L);
        assertThat(tasks.get(0).getName()).isEqualTo("测试任务");
    }

    @Test
    void runTask_shouldCreateRunRecordAndReturnVO() {
        mockAdmin();
        JobCrawlTask task = existingTask(1L, "enabled");
        when(jobCrawlTaskMapper.selectById(1L)).thenReturn(task);

        JobCrawlRun mockRun = new JobCrawlRun();
        mockRun.setId(100L);
        mockRun.setTaskId(1L);
        mockRun.setStatus("running");
        mockRun.setStartedAt(LocalDateTime.now());
        when(jobBatchCrawlService.runTask(1L)).thenReturn(mockRun);

        adminJobCrawlService.runTask("Bearer token", 1L);

        verify(jobBatchCrawlService).runTask(1L);
    }

    private void mockAdmin() {
        mockClaims(1L, "admin");
        when(userMapper.selectById(1L)).thenReturn(adminUser(1L));
    }

    private void mockClaims(Long userId, String role) {
        when(jwtUtils.resolveToken("Bearer token")).thenReturn("token");
        when(jwtUtils.parseToken("token")).thenReturn(new JwtUtils.JwtUserClaims(userId, "username", role));
    }

    private User adminUser(long id) {
        User user = new User();
        user.setId(id);
        user.setUserRole("admin");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
    }

    private User normalUser(long id) {
        User user = new User();
        user.setId(id);
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
    }

    private JobCrawlTask existingTask(long id, String status) {
        JobCrawlTask task = new JobCrawlTask();
        task.setId(id);
        task.setName("测试任务");
        task.setSourceType("company_career_page");
        task.setSourceUrl("https://example.com/careers");
        task.setKeywords("Java,Spring");
        task.setScheduleType("manual");
        task.setStatus(status);
        task.setCreatedBy(1L);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }
}
