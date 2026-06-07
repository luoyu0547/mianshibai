package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.interview.InterviewAnswerRequest;
import com.mianshiba.ai.model.dto.interview.InterviewCreateRequest;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.interview.InterviewAnswerResultVO;
import com.mianshiba.ai.model.vo.interview.InterviewQuestionVO;
import com.mianshiba.ai.service.SpeechService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private InterviewSessionMapper interviewSessionMapper;
    @Mock
    private InterviewTurnMapper interviewTurnMapper;
    @Mock
    private InterviewReportMapper interviewReportMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatClient chatClient;
    @Mock
    private SpeechService speechService;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private JobAnalysisMapper jobAnalysisMapper;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private JwtUtils jwtUtils;
    private InterviewServiceImpl interviewService;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        interviewService = new InterviewServiceImpl(
                interviewSessionMapper, interviewTurnMapper, interviewReportMapper,
                resumeMapper, userMapper, jwtUtils, chatClient, speechService,
                jobMapper, jobAnalysisMapper);
    }

    @Test
    void createSession_rejectsForeignResume() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume foreignResume = new Resume();
        foreignResume.setId(1L);
        foreignResume.setUserId(9999L);
        when(resumeMapper.selectById(1L)).thenReturn(foreignResume);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setResumeId(1L);
        request.setTargetPosition("Java开发");
        request.setTechDirection("后端");

        assertThatThrownBy(() -> interviewService.createSession(auth, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
    }

    @Test
    void startSession_createsFirstTurn() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());

        InterviewSession session = createdSession();
        when(interviewSessionMapper.selectById(100L)).thenReturn(session);
        when(interviewSessionMapper.updateById(any(InterviewSession.class))).thenReturn(1);
        when(interviewTurnMapper.selectList(any())).thenReturn(List.of());
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setTitle("测试简历");
        when(resumeMapper.selectById(1L)).thenReturn(resume);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{\"questionText\": \"请解释 Spring Bean 的生命周期\"}");

        when(interviewTurnMapper.insert(any(InterviewTurn.class))).thenAnswer(invocation -> {
            InterviewTurn turn = invocation.getArgument(0);
            turn.setId(200L);
            return 1;
        });
        when(speechService.synthesizeToBase64(anyString())).thenReturn("base64audio");

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        InterviewQuestionVO result = interviewService.startSession(auth, 100L);

        assertThat(result.getTurnId()).isEqualTo(200L);
        assertThat(result.getQuestionNo()).isEqualTo(1);
        assertThat(result.getTurnType()).isEqualTo("main");
        assertThat(result.getQuestionText()).isEqualTo("请解释 Spring Bean 的生命周期");
        assertThat(result.getTtsAudioBase64()).isEqualTo("base64audio");
        verify(speechService).synthesizeToBase64("请解释 Spring Bean 的生命周期");
    }

    @Test
    void submitAnswer_savesAndCreatesNextTurn() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());

        InterviewSession session = inProgressSession();
        when(interviewSessionMapper.selectById(100L)).thenReturn(session);

        InterviewTurn turn = unansweredTurn();
        when(interviewTurnMapper.selectById(200L)).thenReturn(turn);
        when(interviewTurnMapper.updateById(any(InterviewTurn.class))).thenReturn(1);
        when(interviewTurnMapper.selectCount(any())).thenReturn(0L);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                "{\"nextAction\": \"NEXT_QUESTION\", \"feedback\": \"回答不错\", \"questionText\": \"请解释 HashMap 的原理\"}");

        when(interviewTurnMapper.insert(any(InterviewTurn.class))).thenAnswer(invocation -> {
            InterviewTurn t = invocation.getArgument(0);
            t.setId(201L);
            return 1;
        });
        when(interviewSessionMapper.updateById(any(InterviewSession.class))).thenReturn(1);
        when(speechService.synthesizeToBase64(anyString())).thenReturn("base64audio2");

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        InterviewAnswerRequest answerRequest = new InterviewAnswerRequest();
        answerRequest.setAnswerText("Spring Bean 经历实例化、属性注入、初始化和销毁等阶段");
        answerRequest.setAnswerDurationSeconds(30);

        InterviewAnswerResultVO result = interviewService.submitAnswer(auth, 100L, 200L, answerRequest);

        assertThat(result.getNextAction()).isEqualTo("NEXT_QUESTION");
        assertThat(result.getTurn()).isNotNull();
        assertThat(result.getTurn().getQuestionNo()).isEqualTo(2);
        assertThat(result.getTurn().getQuestionText()).isEqualTo("请解释 HashMap 的原理");
        assertThat(result.getReportId()).isNull();
        verify(speechService).synthesizeToBase64("请解释 HashMap 的原理");
    }

    @Test
    void submitAnswer_onFinalQuestion_createsReport() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());

        InterviewSession session = inProgressSession();
        session.setCurrentQuestionNo(5);
        session.setTotalQuestions(5);
        when(interviewSessionMapper.selectById(100L)).thenReturn(session);

        InterviewTurn turn = unansweredTurn();
        turn.setQuestionNo(5);
        when(interviewTurnMapper.selectById(200L)).thenReturn(turn);
        when(interviewTurnMapper.updateById(any(InterviewTurn.class))).thenReturn(1);
        when(interviewTurnMapper.selectCount(any())).thenReturn(0L);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                "{\"nextAction\": \"REPORT_READY\", \"feedback\": \"面试结束\", \"questionText\": \"\"}");

        InterviewTurn answeredTurn = new InterviewTurn();
        answeredTurn.setId(200L);
        answeredTurn.setSessionId(100L);
        answeredTurn.setQuestionNo(1);
        answeredTurn.setQuestionText("Q1");
        answeredTurn.setAnswerText("A1");
        when(interviewTurnMapper.selectList(any())).thenReturn(List.of(answeredTurn));

        when(interviewReportMapper.insert(any(InterviewReport.class))).thenAnswer(invocation -> {
            InterviewReport report = invocation.getArgument(0);
            report.setId(300L);
            return 1;
        });
        when(interviewSessionMapper.updateById(any(InterviewSession.class))).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        InterviewAnswerRequest answerRequest = new InterviewAnswerRequest();
        answerRequest.setAnswerText("最终回答");
        answerRequest.setAnswerDurationSeconds(60);

        InterviewAnswerResultVO result = interviewService.submitAnswer(auth, 100L, 200L, answerRequest);

        assertThat(result.getNextAction()).isEqualTo("REPORT_READY");
        assertThat(result.getTurn()).isNull();
        assertThat(result.getReportId()).isEqualTo(300L);
        verify(interviewReportMapper).insert(any(InterviewReport.class));
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("developer_001");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        user.setWorkYears(3);
        return user;
    }

    private InterviewSession createdSession() {
        InterviewSession session = new InterviewSession();
        session.setId(100L);
        session.setUserId(1001L);
        session.setResumeId(1L);
        session.setTitle("Java开发 技术模拟面试");
        session.setInterviewType("technical");
        session.setTargetPosition("Java开发");
        session.setTechDirection("后端");
        session.setTotalQuestions(5);
        session.setCurrentQuestionNo(0);
        session.setStatus("created");
        session.setIsDelete(0);
        return session;
    }

    private InterviewSession inProgressSession() {
        InterviewSession session = new InterviewSession();
        session.setId(100L);
        session.setUserId(1001L);
        session.setResumeId(1L);
        session.setTitle("Java开发 技术模拟面试");
        session.setInterviewType("technical");
        session.setTargetPosition("Java开发");
        session.setTechDirection("后端");
        session.setTotalQuestions(5);
        session.setCurrentQuestionNo(1);
        session.setStatus("in_progress");
        session.setIsDelete(0);
        return session;
    }

    private InterviewTurn unansweredTurn() {
        InterviewTurn turn = new InterviewTurn();
        turn.setId(200L);
        turn.setSessionId(100L);
        turn.setQuestionNo(1);
        turn.setTurnType("main");
        turn.setQuestionText("请解释 Spring Bean 的生命周期");
        turn.setIsDelete(0);
        return turn;
    }
}
