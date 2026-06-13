package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.CoachDiagnosis;
import com.mianshiba.ai.model.entity.CoachPlan;
import com.mianshiba.ai.model.entity.CoachTask;
import com.mianshiba.ai.model.entity.User;
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

    @Test
    void generate_fallsBackAndPersistsDiagnosisPlanTasksWhenAiFails() {
        mockUser();
        CoachGenerateRequest request = new CoachGenerateRequest();
        request.setFocus("准备后端面试");

        var result = coachService.generate("Bearer test-token", request);

        assertThat(result.getDiagnosis().getSource()).isEqualTo("fallback");
        assertThat(result.getPlan().getSource()).isEqualTo("fallback");
        assertThat(result.getPlan().getTasks()).hasSize(14);
        verify(planMapper).update(any(), any());
        verify(diagnosisMapper).insert(any(CoachDiagnosis.class));
        verify(planMapper).insert(any(CoachPlan.class));
        verify(taskMapper, times(14)).insert(any(CoachTask.class));
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
}
