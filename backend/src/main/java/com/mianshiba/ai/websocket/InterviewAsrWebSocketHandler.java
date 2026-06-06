package com.mianshiba.ai.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.service.SpeechService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAsrWebSocketHandler extends TextWebSocketHandler {

    private final SpeechService speechService;
    private final JwtUtils jwtUtils;
    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewTurnMapper interviewTurnMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SpeechService.AsrStreamSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.path("type").asText();

            switch (type) {
                case "START" -> handleStart(session, json);
                case "END" -> handleEnd(session);
                default -> sendError(session, "未知的消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理文本消息失败", e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SpeechService.AsrStreamSession asrSession = activeSessions.get(session.getId());
        if (asrSession == null) {
            log.warn("未找到活跃的 ASR 会话: {}", session.getId());
            return;
        }
        try {
            byte[] payload = new byte[message.getPayloadLength()];
            System.arraycopy(message.getPayload().array(), message.getPayload().arrayOffset(), payload, 0, payload.length);
            asrSession.sendAudio(payload);
        } catch (Exception e) {
            log.error("发送音频数据失败", e);
            sendError(session, "音频发送失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 连接关闭: {}, status: {}", session.getId(), status);
        closeAsrSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: {}", session.getId(), exception);
        closeAsrSession(session);
    }

    private void handleStart(WebSocketSession session, JsonNode json) throws Exception {
        String token = getQueryParam(session, "token");
        if (token == null || token.isBlank()) {
            sendError(session, "缺少 token 参数");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long sessionIdParam = json.path("sessionId").asLong();
        Long turnId = json.path("turnId").asLong();

        if (sessionIdParam == 0 || turnId == 0) {
            sendError(session, "缺少 sessionId 或 turnId");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId;
        try {
            String resolvedToken = jwtUtils.resolveToken("Bearer " + token);
            JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(resolvedToken);
            User user = userMapper.selectById(claims.userId());
            if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
                sendError(session, "用户不存在或已删除");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            if (Integer.valueOf(1).equals(user.getUserStatus())) {
                sendError(session, "用户已被禁用");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            userId = user.getId();
        } catch (BusinessException e) {
            sendError(session, "认证失败: " + e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        InterviewSession interviewSession = interviewSessionMapper.selectById(sessionIdParam);
        if (interviewSession == null || !interviewSession.getUserId().equals(userId)) {
            sendError(session, "面试会话不存在或无权访问");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (!"in_progress".equals(interviewSession.getStatus())) {
            sendError(session, "面试会话状态异常");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        InterviewTurn turn = interviewTurnMapper.selectById(turnId);
        if (turn == null || !turn.getSessionId().equals(sessionIdParam)) {
            sendError(session, "面试轮次不存在或不属于该会话");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (turn.getAnswerText() != null) {
            sendError(session, "该轮次已回答");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        SpeechService.AsrStreamSession asrSession = speechService.createAsrStreamSession(
                partial -> sendMessage(session, "ASR_PARTIAL", partial),
                finalText -> sendMessage(session, "ASR_FINAL", finalText),
                error -> sendError(session, "ASR 错误: " + error.getMessage())
        );
        asrSession.start();

        activeSessions.put(session.getId(), asrSession);
        log.info("ASR 会话已创建: wsSession={}, interviewSession={}, turn={}", session.getId(), sessionIdParam, turnId);
    }

    private void handleEnd(WebSocketSession session) {
        closeAsrSession(session);
    }

    private void closeAsrSession(WebSocketSession session) {
        SpeechService.AsrStreamSession asrSession = activeSessions.remove(session.getId());
        if (asrSession != null) {
            try {
                asrSession.stop();
                asrSession.close();
            } catch (Exception e) {
                log.warn("关闭 ASR 会话异常: {}", session.getId(), e);
            }
        }
    }

    private void sendMessage(WebSocketSession session, String type, String text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(JsonNodeFactory.instance.objectNode()
                    .put("type", type)
                    .put("text", text));
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("发送 WebSocket 消息失败", e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(JsonNodeFactory.instance.objectNode()
                    .put("type", "ERROR")
                    .put("message", message));
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("发送错误消息失败", e);
        }
    }

    private String getQueryParam(WebSocketSession session, String paramName) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst(paramName);
    }
}
