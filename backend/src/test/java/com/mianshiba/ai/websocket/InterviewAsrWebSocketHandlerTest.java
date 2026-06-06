package com.mianshiba.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.service.SpeechService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewAsrWebSocketHandlerTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private SpeechService speechService;
    private JwtUtils jwtUtils;
    @Mock
    private InterviewSessionMapper interviewSessionMapper;
    @Mock
    private InterviewTurnMapper interviewTurnMapper;
    @Mock
    private UserMapper userMapper;

    private InterviewAsrWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        objectMapper = new ObjectMapper();
        handler = new InterviewAsrWebSocketHandler(
                speechService, jwtUtils, interviewSessionMapper, interviewTurnMapper, userMapper, objectMapper);
    }

    @Test
    void handleTextMessage_missingToken_sendsError() throws Exception {
        WebSocketSession session = mockSession("ws://localhost/ws/interview/asr");
        when(session.isOpen()).thenReturn(true);

        String payload = objectMapper.writeValueAsString(java.util.Map.of("type", "START", "sessionId", 1, "turnId", 1));
        handler.handleTextMessage(session, new TextMessage(payload));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String response = captor.getValue().getPayload();
        assertThat(response).contains("\"type\":\"ERROR\"");
        assertThat(response).contains("缺少 token 参数");
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void handleTextMessage_start_createsAsrSession() throws Exception {
        String token = jwtUtils.generateToken(1001L, "developer_001", "user");

        WebSocketSession session = mockSession("ws://localhost/ws/interview/asr?token=" + token + "&sessionId=100&turnId=200");

        when(userMapper.selectById(1001L)).thenReturn(normalUser());

        InterviewSession interviewSession = inProgressSession();
        when(interviewSessionMapper.selectById(100L)).thenReturn(interviewSession);

        InterviewTurn turn = unansweredTurn();
        when(interviewTurnMapper.selectById(200L)).thenReturn(turn);

        SpeechService.AsrStreamSession asrSession = mock(SpeechService.AsrStreamSession.class);
        when(speechService.createAsrStreamSession(any(), any(), any())).thenReturn(asrSession);

        String payload = objectMapper.writeValueAsString(java.util.Map.of("type", "START", "sessionId", 100, "turnId", 200));
        handler.handleTextMessage(session, new TextMessage(payload));

        verify(speechService).createAsrStreamSession(any(), any(), any());
        verify(asrSession).start();
    }

    @Test
    void handleBinaryMessage_forwardsAudio() throws Exception {
        String token = jwtUtils.generateToken(1001L, "developer_001", "user");

        WebSocketSession session = mockSession("ws://localhost/ws/interview/asr?token=" + token + "&sessionId=100&turnId=200");

        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(interviewSessionMapper.selectById(100L)).thenReturn(inProgressSession());
        when(interviewTurnMapper.selectById(200L)).thenReturn(unansweredTurn());

        SpeechService.AsrStreamSession asrSession = mock(SpeechService.AsrStreamSession.class);
        when(speechService.createAsrStreamSession(any(), any(), any())).thenReturn(asrSession);

        String startPayload = objectMapper.writeValueAsString(java.util.Map.of("type", "START", "sessionId", 100, "turnId", 200));
        handler.handleTextMessage(session, new TextMessage(startPayload));

        byte[] audioData = new byte[]{1, 2, 3, 4, 5};
        BinaryMessage binaryMessage = new BinaryMessage(ByteBuffer.wrap(audioData));
        handler.handleBinaryMessage(session, binaryMessage);

        verify(asrSession).sendAudio(any(byte[].class));
    }

    @Test
    void handleTextMessage_end_stopsAsrSession() throws Exception {
        String token = jwtUtils.generateToken(1001L, "developer_001", "user");

        WebSocketSession session = mockSession("ws://localhost/ws/interview/asr?token=" + token + "&sessionId=100&turnId=200");

        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(interviewSessionMapper.selectById(100L)).thenReturn(inProgressSession());
        when(interviewTurnMapper.selectById(200L)).thenReturn(unansweredTurn());

        SpeechService.AsrStreamSession asrSession = mock(SpeechService.AsrStreamSession.class);
        when(speechService.createAsrStreamSession(any(), any(), any())).thenReturn(asrSession);

        String startPayload = objectMapper.writeValueAsString(java.util.Map.of("type", "START", "sessionId", 100, "turnId", 200));
        handler.handleTextMessage(session, new TextMessage(startPayload));

        String endPayload = objectMapper.writeValueAsString(java.util.Map.of("type", "END"));
        handler.handleTextMessage(session, new TextMessage(endPayload));

        verify(asrSession).stop();
        verify(asrSession).close();
    }

    private WebSocketSession mockSession(String url) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        lenient().when(session.getId()).thenReturn("test-ws-session-id");
        when(session.getUri()).thenReturn(new java.net.URI(url));
        return session;
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("developer_001");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
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
