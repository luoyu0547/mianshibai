package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.InterviewTurnReviewMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.service.InterviewReportEnhancementQueue;
import com.mianshiba.ai.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewReportEnhancementServiceImplTest {

    @Mock
    private InterviewReportEnhancementMapper enhancementMapper;
    @Mock
    private InterviewTurnReviewMapper turnReviewMapper;
    @Mock
    private InterviewReportEnhancementQueue queue;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewTurnMapper turnMapper;
    @Mock
    private InterviewReportMapper reportMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private InterviewReportEnhancementServiceImpl enhancementService;

    @Test
    void createTaskIfAbsent_shouldCreatePendingTaskAndPublish() {
        InterviewReport report = new InterviewReport();
        report.setId(10L);
        report.setSessionId(20L);
        InterviewSession session = new InterviewSession();
        session.setId(20L);
        session.setUserId(1L);

        when(enhancementMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<InterviewReportEnhancement>>any())).thenReturn(null);
        when(enhancementMapper.insert(ArgumentMatchers.any(InterviewReportEnhancement.class))).thenReturn(1);

        enhancementService.createTaskIfAbsent(session, report);

        verify(enhancementMapper).insert(ArgumentMatchers.<InterviewReportEnhancement>argThat(task ->
                task.getUserId().equals(1L)
                        && task.getSessionId().equals(20L)
                        && task.getReportId().equals(10L)
                        && "pending".equals(task.getStatus())));
        verify(queue).publish(argThat(task -> task.getReportId().equals(10L)));
    }

    @Test
    void createTaskIfAbsent_shouldSkipIfAlreadyExists() {
        InterviewReport report = new InterviewReport();
        report.setId(10L);
        InterviewSession session = new InterviewSession();
        session.setId(20L);
        session.setUserId(1L);

        when(enhancementMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<InterviewReportEnhancement>>any()))
                .thenReturn(new InterviewReportEnhancement());

        enhancementService.createTaskIfAbsent(session, report);

        verify(enhancementMapper, org.mockito.Mockito.never()).insert(ArgumentMatchers.any(InterviewReportEnhancement.class));
        verify(queue, org.mockito.Mockito.never()).publish(ArgumentMatchers.any());
    }

    @Test
    void retry_shouldRejectRunningTask() {
        InterviewReportEnhancement task = new InterviewReportEnhancement();
        task.setId(1L);
        task.setUserId(1L);
        task.setSessionId(20L);
        task.setReportId(10L);
        task.setStatus("running");

        when(jwtUtils.resolveToken("Bearer token")).thenReturn("token");
        JwtUtils.JwtUserClaims claims = new JwtUtils.JwtUserClaims(1L, "user", "user");
        when(jwtUtils.parseToken("token")).thenReturn(claims);
        com.mianshiba.ai.model.entity.User user = new com.mianshiba.ai.model.entity.User();
        user.setId(1L);
        user.setIsDelete(0);
        user.setUserStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        InterviewSession session = new InterviewSession();
        session.setId(20L);
        session.setUserId(1L);
        when(sessionMapper.selectById(20L)).thenReturn(session);

        when(enhancementMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<InterviewReportEnhancement>>any())).thenReturn(task);

        assertThatThrownBy(() -> enhancementService.retry("Bearer token", 20L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void runTask_shouldSkipIfNotFound() {
        when(enhancementMapper.selectById(999L)).thenReturn(null);

        enhancementService.runTask(999L);

        verify(enhancementMapper).selectById(999L);
        verify(enhancementMapper, org.mockito.Mockito.never())
                .updateById(ArgumentMatchers.any(InterviewReportEnhancement.class));
    }
}
