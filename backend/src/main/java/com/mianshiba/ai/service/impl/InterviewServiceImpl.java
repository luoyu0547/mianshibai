package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.interview.InterviewAnswerResultVO;
import com.mianshiba.ai.model.vo.interview.InterviewQuestionVO;
import com.mianshiba.ai.model.vo.interview.InterviewReportVO;
import com.mianshiba.ai.model.vo.interview.InterviewSessionVO;
import com.mianshiba.ai.model.vo.interview.InterviewTurnVO;
import com.mianshiba.ai.service.InterviewService;
import com.mianshiba.ai.service.SpeechService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private static final String GENERATE_QUESTION_PROMPT =
            "你是一位资深技术面试官。请根据以下信息生成一道技术面试问题：\n" +
            "- 目标岗位：%s\n" +
            "- 技术方向：%s\n" +
            "- 工作年限：%s 年\n" +
            "- 简历摘要：%s\n" +
            "- 当前第 %d 题（共 %d 题）\n" +
            "- 历史问答摘要：%s\n\n" +
            "请返回 JSON 格式：{\"questionText\": \"你的问题\"}\n" +
            "直接返回 JSON，不要包含其他文字。";

    private static final String ANSWER_DECISION_PROMPT =
            "你是一位资深技术面试官。用户刚刚回答了以下问题：\n" +
            "问题：%s\n" +
            "用户回答：%s\n" +
            "目标岗位：%s\n" +
            "当前第 %d 题（共 %d 题），本题是否已追问过：%s\n\n" +
            "请判断下一步动作：\n" +
            "1. 如果回答过短、偏题或需要深入，且本题尚未追问过，返回 FOLLOW_UP\n" +
            "2. 如果还需要继续下一题，返回 NEXT_QUESTION\n" +
            "3. 如果已是最后一题或回答充分，返回 REPORT_READY\n\n" +
            "请返回 JSON 格式：\n" +
            "{\"nextAction\": \"FOLLOW_UP|NEXT_QUESTION|REPORT_READY\", \"feedback\": \"简短反馈\", \"questionText\": \"追问或下一题（REPORT_READY 时为空）\"}\n" +
            "直接返回 JSON，不要包含其他文字。";

    private static final String REPORT_PROMPT =
            "你是一位资深技术面试官。请根据以下完整面试问答生成评分报告：\n" +
            "目标岗位：%s\n" +
            "技术方向：%s\n" +
            "面试问答记录：\n%s\n\n" +
            "请返回 JSON 格式：\n" +
            "{\"totalScore\": 85, \"accuracyScore\": 80, \"clarityScore\": 85, \"depthScore\": 78, \"matchingScore\": 86, \"summary\": \"总体评价\", \"suggestions\": [\"建议1\", \"建议2\"]}\n" +
            "分数范围 0-100。直接返回 JSON，不要包含其他文字。";

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewTurnMapper interviewTurnMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final ResumeMapper resumeMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final ChatClient chatClient;
    private final SpeechService speechService;
    private final JobMapper jobMapper;
    private final JobAnalysisMapper jobAnalysisMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO createSession(String authorizationHeader, InterviewCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        Resume resume = resumeMapper.selectById(request.getResumeId());
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        if (request.getJobId() != null) {
            Job job = jobMapper.selectById(request.getJobId());
            if (job == null) {
                throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
            }
        }

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeId(request.getResumeId());
        session.setTitle(request.getTargetPosition() + " 技术模拟面试");
        session.setInterviewType("technical");
        session.setTargetPosition(request.getTargetPosition());
        session.setTechDirection(request.getTechDirection());
        session.setJobId(request.getJobId());
        session.setTotalQuestions(5);
        session.setCurrentQuestionNo(0);
        session.setStatus("created");
        session.setIsDelete(0);
        interviewSessionMapper.insert(session);

        return toInterviewSessionVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionVO startSession(String authorizationHeader, Long sessionId) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = getSessionAndCheckOwner(sessionId, userId);

        if (!"created".equals(session.getStatus())) {
            if (!("in_progress".equals(session.getStatus()) && !hasAnsweredTurns(sessionId))) {
                throw new BusinessException(ErrorCode.INTERVIEW_STATUS_ERROR);
            }
        }

        session.setStatus("in_progress");
        if (session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
        }
        interviewSessionMapper.updateById(session);

        String historySummary = buildHistorySummary(sessionId);
        Resume resume = resumeMapper.selectById(session.getResumeId());
        String resumeSummary = resume != null ? resume.getTitle() : "";
        User user = userMapper.selectById(userId);

        String techDirection = session.getTechDirection() != null ? session.getTechDirection() : "不限";
        String workYears = user != null && user.getWorkYears() != null ? String.valueOf(user.getWorkYears()) : "不限";

        String systemPrompt = String.format(GENERATE_QUESTION_PROMPT,
                session.getTargetPosition(), techDirection, workYears,
                resumeSummary, 1, session.getTotalQuestions(), historySummary);

        systemPrompt += buildJobContext(session);

        String aiResponse = callAi(systemPrompt, "请生成第一道面试题。");
        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> questionMap = parseJson(json, new TypeReference<Map<String, Object>>() {});

        String questionText = (String) questionMap.get("questionText");

        InterviewTurn turn = new InterviewTurn();
        turn.setSessionId(sessionId);
        turn.setQuestionNo(1);
        turn.setTurnType("main");
        turn.setQuestionText(questionText);
        turn.setIsDelete(0);
        interviewTurnMapper.insert(turn);

        session.setCurrentQuestionNo(1);
        interviewSessionMapper.updateById(session);

        String ttsAudioBase64 = synthesizeQuietly(questionText);

        return toInterviewQuestionVO(turn, ttsAudioBase64);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewAnswerResultVO submitAnswer(String authorizationHeader, Long sessionId, Long turnId, InterviewAnswerRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = getSessionAndCheckOwner(sessionId, userId);

        InterviewTurn turn = interviewTurnMapper.selectById(turnId);
        if (turn == null || !turn.getSessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_TURN_ERROR);
        }
        if (turn.getAnswerText() != null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TURN_ERROR);
        }

        turn.setAnswerText(request.getAnswerText());
        turn.setAnswerDurationSeconds(request.getAnswerDurationSeconds());
        interviewTurnMapper.updateById(turn);

        boolean hasFollowUp = interviewTurnMapper.selectCount(Wrappers.lambdaQuery(InterviewTurn.class)
                .eq(InterviewTurn::getSessionId, sessionId)
                .eq(InterviewTurn::getQuestionNo, turn.getQuestionNo())
                .eq(InterviewTurn::getTurnType, "follow_up")) > 0;

        String techDirection = session.getTechDirection() != null ? session.getTechDirection() : "不限";
        String systemPrompt = String.format(ANSWER_DECISION_PROMPT,
                turn.getQuestionText(), request.getAnswerText(),
                session.getTargetPosition(),
                turn.getQuestionNo(), session.getTotalQuestions(),
                hasFollowUp ? "是" : "否");

        systemPrompt += buildJobContext(session);

        String aiResponse = callAi(systemPrompt, "");
        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> decisionMap = parseJson(json, new TypeReference<Map<String, Object>>() {});

        String nextAction = (String) decisionMap.get("nextAction");
        String feedback = (String) decisionMap.get("feedback");
        String newQuestionText = (String) decisionMap.get("questionText");

        turn.setAiFeedback(feedback);
        interviewTurnMapper.updateById(turn);

        InterviewAnswerResultVO result = new InterviewAnswerResultVO();
        result.setNextAction(nextAction);

        switch (nextAction) {
            case "FOLLOW_UP" -> {
                InterviewTurn followUpTurn = new InterviewTurn();
                followUpTurn.setSessionId(sessionId);
                followUpTurn.setQuestionNo(turn.getQuestionNo());
                followUpTurn.setTurnType("follow_up");
                followUpTurn.setQuestionText(newQuestionText);
                followUpTurn.setIsDelete(0);
                interviewTurnMapper.insert(followUpTurn);

                String ttsAudio = synthesizeQuietly(newQuestionText);
                result.setTurn(toInterviewQuestionVO(followUpTurn, ttsAudio));
            }
            case "NEXT_QUESTION" -> {
                int nextQuestionNo = turn.getQuestionNo() + 1;
                InterviewTurn nextTurn = new InterviewTurn();
                nextTurn.setSessionId(sessionId);
                nextTurn.setQuestionNo(nextQuestionNo);
                nextTurn.setTurnType("main");
                nextTurn.setQuestionText(newQuestionText);
                nextTurn.setIsDelete(0);
                interviewTurnMapper.insert(nextTurn);

                session.setCurrentQuestionNo(nextQuestionNo);
                interviewSessionMapper.updateById(session);

                String ttsAudio = synthesizeQuietly(newQuestionText);
                result.setTurn(toInterviewQuestionVO(nextTurn, ttsAudio));
            }
            case "REPORT_READY" -> {
                InterviewReport report = generateReport(session);
                session.setStatus("completed");
                session.setEndedAt(LocalDateTime.now());
                interviewSessionMapper.updateById(session);
                result.setReportId(report.getId());
            }
            default -> throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        return result;
    }

    @Override
    public InterviewSessionVO getSession(String authorizationHeader, Long sessionId) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = getSessionAndCheckOwner(sessionId, userId);
        return toInterviewSessionVO(session);
    }

    @Override
    public List<InterviewSessionVO> listSessions(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .orderByDesc(InterviewSession::getUpdateTime));
        return sessions.stream().map(this::toInterviewSessionVO).collect(Collectors.toList());
    }

    @Override
    public InterviewReportVO getReport(String authorizationHeader, Long sessionId) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = getSessionAndCheckOwner(sessionId, userId);

        InterviewReport report = interviewReportMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReport.class)
                        .eq(InterviewReport::getSessionId, sessionId));
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        List<InterviewTurn> turns = interviewTurnMapper.selectList(
                Wrappers.lambdaQuery(InterviewTurn.class)
                        .eq(InterviewTurn::getSessionId, sessionId)
                        .orderByAsc(InterviewTurn::getQuestionNo)
                        .orderByAsc(InterviewTurn::getId));

        return toInterviewReportVO(report, turns);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSession(String authorizationHeader, Long sessionId) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = getSessionAndCheckOwner(sessionId, userId);

        if (!"created".equals(session.getStatus()) && !"in_progress".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_STATUS_ERROR);
        }

        session.setStatus("cancelled");
        interviewSessionMapper.updateById(session);
    }

    private InterviewReport generateReport(InterviewSession session) {
        List<InterviewTurn> turns = interviewTurnMapper.selectList(
                Wrappers.lambdaQuery(InterviewTurn.class)
                        .eq(InterviewTurn::getSessionId, session.getId())
                        .orderByAsc(InterviewTurn::getQuestionNo)
                        .orderByAsc(InterviewTurn::getId));

        String qaRecord = turns.stream()
                .map(t -> "Q: " + t.getQuestionText() + "\nA: " + (t.getAnswerText() != null ? t.getAnswerText() : ""))
                .collect(Collectors.joining("\n\n"));

        String techDirection = session.getTechDirection() != null ? session.getTechDirection() : "不限";
        String systemPrompt = String.format(REPORT_PROMPT,
                session.getTargetPosition(), techDirection, qaRecord);

        systemPrompt += buildJobContext(session);

        String aiResponse = callAi(systemPrompt, "");
        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> reportMap = parseJson(json, new TypeReference<Map<String, Object>>() {});

        InterviewReport report = new InterviewReport();
        report.setSessionId(session.getId());
        report.setTotalScore(toInteger(reportMap.get("totalScore")));
        report.setAccuracyScore(toInteger(reportMap.get("accuracyScore")));
        report.setClarityScore(toInteger(reportMap.get("clarityScore")));
        report.setDepthScore(toInteger(reportMap.get("depthScore")));
        report.setMatchingScore(toInteger(reportMap.get("matchingScore")));
        report.setSummary((String) reportMap.get("summary"));

        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) reportMap.get("suggestions");
        report.setSuggestions(suggestions);

        interviewReportMapper.insert(report);
        return report;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        return user.getId();
    }

    private InterviewSession getSessionAndCheckOwner(Long sessionId, Long userId) {
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND_ERROR);
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return session;
    }

    private boolean hasAnsweredTurns(Long sessionId) {
        return interviewTurnMapper.selectCount(Wrappers.lambdaQuery(InterviewTurn.class)
                .eq(InterviewTurn::getSessionId, sessionId)
                .isNotNull(InterviewTurn::getAnswerText)) == 0;
    }

    private String buildHistorySummary(Long sessionId) {
        List<InterviewTurn> turns = interviewTurnMapper.selectList(
                Wrappers.lambdaQuery(InterviewTurn.class)
                        .eq(InterviewTurn::getSessionId, sessionId)
                        .isNotNull(InterviewTurn::getAnswerText)
                        .orderByAsc(InterviewTurn::getQuestionNo));
        if (turns.isEmpty()) {
            return "暂无";
        }
        return turns.stream()
                .map(t -> "Q" + t.getQuestionNo() + ": " + t.getQuestionText() + " → " + t.getAnswerText())
                .collect(Collectors.joining("\n"));
    }

    private String buildJobContext(InterviewSession session) {
        if (session.getJobId() == null) {
            return "";
        }
        Job job = jobMapper.selectById(session.getJobId());
        if (job == null) {
            return "";
        }
        JobAnalysis jobAnalysis = jobAnalysisMapper.selectOne(
                Wrappers.lambdaQuery(JobAnalysis.class)
                        .eq(JobAnalysis::getJobId, session.getJobId()));
        if (jobAnalysis == null) {
            return "";
        }
        return String.format(
                "\n\n目标岗位信息：\n职位名称：%s\n公司：%s\n岗位要求：%s\n核心技术栈：%s\n隐含要求：%s\n面试重点：%s\n请围绕以上岗位要求生成面试问题。",
                job.getTitle(),
                job.getCompanyName() != null ? job.getCompanyName() : "",
                jobAnalysis.getRequirementSummary() != null ? jobAnalysis.getRequirementSummary() : "",
                jobAnalysis.getCoreSkills() != null ? jobAnalysis.getCoreSkills() : "",
                jobAnalysis.getHiddenRequirements() != null ? jobAnalysis.getHiddenRequirements() : "",
                jobAnalysis.getInterviewFocus() != null ? jobAnalysis.getInterviewFocus() : "");
    }

    private String callAi(String systemPrompt, String userMessage) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private String extractJsonFromResponse(String text) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.trim();
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("AI 响应解析失败: {}", json, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String synthesizeQuietly(String text) {
        try {
            return speechService.synthesizeToBase64(text);
        } catch (Exception e) {
            log.warn("TTS 合成失败", e);
            return null;
        }
    }

    private InterviewSessionVO toInterviewSessionVO(InterviewSession session) {
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setResumeId(session.getResumeId());
        vo.setTitle(session.getTitle());
        vo.setInterviewType(session.getInterviewType());
        vo.setTargetPosition(session.getTargetPosition());
        vo.setTechDirection(session.getTechDirection());
        vo.setTotalQuestions(session.getTotalQuestions());
        vo.setCurrentQuestionNo(session.getCurrentQuestionNo());
        vo.setStatus(session.getStatus());
        vo.setStartedAt(session.getStartedAt());
        vo.setEndedAt(session.getEndedAt());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    private InterviewQuestionVO toInterviewQuestionVO(InterviewTurn turn, String ttsAudioBase64) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setTurnId(turn.getId());
        vo.setQuestionNo(turn.getQuestionNo());
        vo.setTurnType(turn.getTurnType());
        vo.setQuestionText(turn.getQuestionText());
        vo.setTtsAudioBase64(ttsAudioBase64);
        return vo;
    }

    private InterviewTurnVO toInterviewTurnVO(InterviewTurn turn) {
        InterviewTurnVO vo = new InterviewTurnVO();
        vo.setId(turn.getId());
        vo.setSessionId(turn.getSessionId());
        vo.setQuestionNo(turn.getQuestionNo());
        vo.setTurnType(turn.getTurnType());
        vo.setQuestionText(turn.getQuestionText());
        vo.setAnswerText(turn.getAnswerText());
        vo.setAiFeedback(turn.getAiFeedback());
        vo.setAnswerDurationSeconds(turn.getAnswerDurationSeconds());
        vo.setCreateTime(turn.getCreateTime());
        vo.setUpdateTime(turn.getUpdateTime());
        return vo;
    }

    private InterviewReportVO toInterviewReportVO(InterviewReport report, List<InterviewTurn> turns) {
        InterviewReportVO vo = new InterviewReportVO();
        vo.setId(report.getId());
        vo.setSessionId(report.getSessionId());
        vo.setTotalScore(report.getTotalScore());
        vo.setAccuracyScore(report.getAccuracyScore());
        vo.setClarityScore(report.getClarityScore());
        vo.setDepthScore(report.getDepthScore());
        vo.setMatchingScore(report.getMatchingScore());
        vo.setSummary(report.getSummary());
        vo.setSuggestions(report.getSuggestions());
        vo.setTurns(turns.stream().map(this::toInterviewTurnVO).collect(Collectors.toList()));
        vo.setCreateTime(report.getCreateTime());
        return vo;
    }
}
