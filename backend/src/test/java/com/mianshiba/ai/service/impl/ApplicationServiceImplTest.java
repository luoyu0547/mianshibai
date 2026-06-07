package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationStatusUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoCreateRequest;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private JobApplicationMapper applicationMapper;
    @Mock
    private ApplicationTodoMapper todoMapper;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private ResumeMapper resumeMapper;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private void mockResolveUserId() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));
    }

    @Test
    void createApplication_insertsRecordForCurrentUser() {
        mockResolveUserId();
        when(applicationMapper.insert(any(JobApplication.class))).thenReturn(1);

        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setCompanyName("TestCompany");
        request.setJobTitle("Dev");

        applicationService.createApplication("Bearer test-token", request);

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getCompanyName()).isEqualTo("TestCompany");
        assertThat(captor.getValue().getJobTitle()).isEqualTo("Dev");
    }

    @Test
    void updateStatus_rejectsInvalidStatus() {
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("invalid_status");

        assertThatThrownBy(() -> applicationService.updateStatus("Bearer test-token", 1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void deleteApplication_deletesOwnedTodos() {
        mockResolveUserId();

        JobApplication app = new JobApplication();
        app.setId(1L);
        app.setUserId(1L);
        when(applicationMapper.selectById(1L)).thenReturn(app);

        applicationService.deleteApplication("Bearer test-token", 1L);

        verify(todoMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void createApplicationTodo_requiresOwnedApplication() {
        mockResolveUserId();
        when(applicationMapper.selectById(999L)).thenReturn(null);

        ApplicationTodoCreateRequest request = new ApplicationTodoCreateRequest();
        request.setTitle("Prepare docs");

        assertThatThrownBy(() -> applicationService.createApplicationTodo("Bearer test-token", 999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.APPLICATION_NOT_FOUND_ERROR.getCode());
    }

    @Test
    void completeTodo_setsCompletedAt() {
        mockResolveUserId();

        ApplicationTodo todo = new ApplicationTodo();
        todo.setId(1L);
        todo.setUserId(1L);
        todo.setApplicationId(10L);
        todo.setCompleted(0);
        when(todoMapper.selectById(1L)).thenReturn(todo);
        when(applicationMapper.selectById(10L)).thenReturn(new JobApplication());

        applicationService.completeTodo("Bearer test-token", 1L);

        ArgumentCaptor<ApplicationTodo> captor = ArgumentCaptor.forClass(ApplicationTodo.class);
        verify(todoMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCompleted()).isEqualTo(1);
        assertThat(captor.getValue().getCompletedAt()).isNotNull();
    }

    @Test
    void reopenTodo_clearsCompletedAt() {
        mockResolveUserId();

        ApplicationTodo todo = new ApplicationTodo();
        todo.setId(1L);
        todo.setUserId(1L);
        todo.setApplicationId(10L);
        todo.setCompleted(1);
        todo.setCompletedAt(java.time.LocalDateTime.now());
        when(todoMapper.selectById(1L)).thenReturn(todo);
        when(applicationMapper.selectById(10L)).thenReturn(new JobApplication());

        applicationService.reopenTodo("Bearer test-token", 1L);

        ArgumentCaptor<ApplicationTodo> captor2 = ArgumentCaptor.forClass(ApplicationTodo.class);
        verify(todoMapper).updateById(captor2.capture());
        assertThat(captor2.getValue().getCompleted()).isEqualTo(0);
        assertThat(captor2.getValue().getCompletedAt()).isNull();
    }
}