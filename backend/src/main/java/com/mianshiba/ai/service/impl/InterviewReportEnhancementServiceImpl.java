package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.InterviewTurnReviewMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.entity.InterviewTurnReview;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.interview.InterviewReportEnhancementVO;
import com.mianshiba.ai.model.vo.interview.InterviewTurnReviewVO;
import com.mianshiba.ai.service.InterviewReportEnhancementQueue;
import com.mianshiba.ai.service.InterviewReportEnhancementService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReportEnhancementServiceImpl implements InterviewReportEnhancementService {

    private final InterviewReportEnhancementMapper enhancementMapper;
    private final InterviewTurnReviewMapper turnReviewMapper;
    private final InterviewReportEnhancementQueue queue;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final InterviewReportMapper reportMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final JobMapper jobMapper;
    private final JobAnalysisMapper jobAnalysisMapper;

    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n([\\s\\S]*?)\\n```");

    @Override
    public void createTaskIfAbsent(InterviewSession session, InterviewReport report) {
        InterviewReportEnhancement existing = enhancementMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getReportId, report.getId()));
        if (existing != null) return;

        InterviewReportEnhancement task = new InterviewReportEnhancement();
        task.setUserId(session.getUserId());
        task.setSessionId(session.getId());
        task.setReportId(report.getId());
        task.setStatus("pending");
        task.setRetryCount(0);
        task.setIsDelete(0);
        enhancementMapper.insert(task);
        queue.publish(task);
    }

    @Override
    public InterviewReportEnhancementVO getEnhancement(String authorizationHeader, Long sessionId) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        InterviewReportEnhancement enhancement = enhancementMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getSessionId, sessionId));
        if (enhancement == null) {
            InterviewReportEnhancementVO vo = new InterviewReportEnhancementVO();
            vo.setStatus("pending");
            return vo;
        }

        InterviewReportEnhancementVO vo = toEnhancementVO(enhancement);
        if ("completed".equals(enhancement.getStatus())) {
            List<InterviewTurnReview> reviews = turnReviewMapper.selectList(
                    Wrappers.lambdaQuery(InterviewTurnReview.class)
                            .eq(InterviewTurnReview::getReportId, enhancement.getReportId()));
            vo.setTurnReviews(reviews.stream().map(this::toTurnReviewVO).collect(Collectors.toList()));
        }
        return vo;
    }

    @Override
    public InterviewReportEnhancementVO retry(String authorizationHeader, Long sessionId) {
        Long userId = resolveUserId(authorizationHeader);
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        InterviewReportEnhancement enhancement = enhancementMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getSessionId, sessionId));
        if (enhancement == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_ENHANCE_ERROR);
        }
        if ("running".equals(enhancement.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_ENHANCE_ERROR);
        }

        enhancement.setStatus("pending");
        enhancement.setErrorMessage(null);
        enhancement.setRetryCount(enhancement.getRetryCount() != null ? enhancement.getRetryCount() + 1 : 1);
        enhancementMapper.updateById(enhancement);
        queue.publish(enhancement);

        return toEnhancementVO(enhancement);
    }

    @Override
    public void runTask(Long enhancementId) {
        // 1. 加载增强任务，已完成或不存在则跳过
        InterviewReportEnhancement task = enhancementMapper.selectById(enhancementId);
        if (task == null || "completed".equals(task.getStatus())) {
            return;
        }

        try {
            // 2. 设置状态为运行中
            task.setStatus("running");
            enhancementMapper.updateById(task);

            // 3. 加载面试报告
            InterviewReport report = reportMapper.selectById(task.getReportId());
            if (report == null) {
                throw new RuntimeException("Report not found: " + task.getReportId());
            }

            // 4. 加载面试会话
            InterviewSession session = sessionMapper.selectById(task.getSessionId());
            if (session == null) {
                throw new RuntimeException("Session not found: " + task.getSessionId());
            }

            // 5. 加载所有已回答的面试轮次
            List<InterviewTurn> turns = turnMapper.selectList(
                    Wrappers.lambdaQuery(InterviewTurn.class)
                            .eq(InterviewTurn::getSessionId, task.getSessionId())
                            .isNotNull(InterviewTurn::getAnswerText)
                            .orderByAsc(InterviewTurn::getQuestionNo)
                            .orderByAsc(InterviewTurn::getId));

            // 6. 构建 AI 提示词
            String systemPrompt = buildEnhancementPrompt(report, session, turns);

            // 7. 调用 AI
            String aiResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user("")
                    .call()
                    .content();

            // 8. 提取 JSON
            String json = extractJsonFromResponse(aiResponse);

            // 9. 解析 JSON
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            // 10. 删除旧的轮次评审
            turnReviewMapper.delete(Wrappers.lambdaQuery(InterviewTurnReview.class)
                    .eq(InterviewTurnReview::getReportId, task.getReportId()));

            // 11. 插入新的轮次评审
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> turnReviews = (List<Map<String, Object>>) result.get("turnReviews");
            if (turnReviews != null) {
                for (Map<String, Object> item : turnReviews) {
                    InterviewTurnReview review = new InterviewTurnReview();
                    review.setUserId(task.getUserId());
                    review.setSessionId(task.getSessionId());
                    review.setReportId(task.getReportId());
                    review.setTurnId(toLong(item.get("turnId")));
                    review.setQuestion(toString(item.get("question")));
                    review.setAnswerSummary(toString(item.get("answerSummary")));
                    review.setDiagnosis(toString(item.get("diagnosis")));
                    review.setExcellentAnswer(toString(item.get("excellentAnswer")));
                    review.setImprovedAnswer(toString(item.get("improvedAnswer")));
                    @SuppressWarnings("unchecked")
                    List<String> knowledgePoints = (List<String>) item.get("knowledgePoints");
                    review.setKnowledgePointsJson(knowledgePoints);
                    review.setIsDelete(0);
                    turnReviewMapper.insert(review);
                }
            }

            // 12. 更新增强任务为已完成
            task.setStatus("completed");
            task.setSummary(toString(result.get("summary")));
            @SuppressWarnings("unchecked")
            Map<String, Integer> radar = (Map<String, Integer>) result.get("radar");
            task.setRadarJson(radar);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> skillGaps = (List<Map<String, String>>) result.get("skillGaps");
            task.setSkillGapsJson(skillGaps);
            @SuppressWarnings("unchecked")
            List<String> actionItems = (List<String>) result.get("actionItems");
            task.setActionItemsJson(actionItems);
            task.setErrorMessage(null);
            enhancementMapper.updateById(task);

        } catch (Exception e) {
            // 13. 异常处理：标记失败
            log.error("增强任务执行失败, enhancementId={}", enhancementId, e);
            task.setStatus("failed");
            String msg = e.getMessage();
            if (msg != null && msg.length() > 512) {
                msg = msg.substring(0, 512);
            }
            task.setErrorMessage(msg);
            enhancementMapper.updateById(task);
        }
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

    private InterviewReportEnhancementVO toEnhancementVO(InterviewReportEnhancement entity) {
        InterviewReportEnhancementVO vo = new InterviewReportEnhancementVO();
        vo.setId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setReportId(entity.getReportId());
        vo.setStatus(entity.getStatus());
        vo.setSummary(entity.getSummary());
        vo.setRadar(entity.getRadarJson());
        vo.setSkillGaps(entity.getSkillGapsJson());
        vo.setActionItems(entity.getActionItemsJson());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setRetryCount(entity.getRetryCount());
        return vo;
    }

    private InterviewTurnReviewVO toTurnReviewVO(InterviewTurnReview entity) {
        InterviewTurnReviewVO vo = new InterviewTurnReviewVO();
        vo.setId(entity.getId());
        vo.setTurnId(entity.getTurnId());
        vo.setQuestion(entity.getQuestion());
        vo.setAnswerSummary(entity.getAnswerSummary());
        vo.setDiagnosis(entity.getDiagnosis());
        vo.setExcellentAnswer(entity.getExcellentAnswer());
        vo.setImprovedAnswer(entity.getImprovedAnswer());
        vo.setKnowledgePoints(entity.getKnowledgePointsJson());
        return vo;
    }

    private String buildEnhancementPrompt(InterviewReport report, InterviewSession session, List<InterviewTurn> turns) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深面试辅导专家。请根据以下面试报告和问答记录，生成一份深度增强分析。\n\n");

        sb.append("## 面试报告\n");
        sb.append("- 综合评分: ").append(report.getTotalScore()).append("\n");
        sb.append("- 技术准确性: ").append(report.getAccuracyScore()).append("\n");
        sb.append("- 表达清晰度: ").append(report.getClarityScore()).append("\n");
        sb.append("- 项目深度: ").append(report.getDepthScore()).append("\n");
        sb.append("- 岗位匹配度: ").append(report.getMatchingScore()).append("\n");
        if (report.getSummary() != null) {
            sb.append("- 总结: ").append(report.getSummary()).append("\n");
        }
        if (report.getSuggestions() != null) {
            sb.append("- 建议: ").append(String.join("；", report.getSuggestions())).append("\n");
        }

        sb.append("\n## 面试问答记录\n");
        for (InterviewTurn turn : turns) {
            sb.append("### 第").append(turn.getQuestionNo()).append("题");
            if ("follow_up".equals(turn.getTurnType())) {
                sb.append("（追问）");
            }
            sb.append("\n");
            sb.append("**问题**: ").append(turn.getQuestionText()).append("\n");
            sb.append("**回答**: ").append(turn.getAnswerText()).append("\n\n");
        }

        sb.append(buildJobContext(session));

        sb.append("\n## 输出要求\n");
        sb.append("请严格以 JSON 格式输出，包含以下字段：\n");
        sb.append("- summary: 面试综合评语（200字以内）\n");
        sb.append("- radar: 雷达图评分 {accuracy, clarity, depth, matching, systemDesign}（0-100整数）\n");
        sb.append("- skillGaps: 技能短板数组 [{name, severity(high/medium/low), evidence}]\n");
        sb.append("- actionItems: 改进建议数组（字符串）\n");
        sb.append("- turnReviews: 每题评审数组 [{turnId, question, answerSummary, diagnosis, excellentAnswer, improvedAnswer, knowledgePoints}]\n");
        sb.append("请用 ```json ``` 包裹输出。\n");

        return sb.toString();
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
            return "\n\n目标岗位: " + job.getTitle() + "，公司: " + (job.getCompanyName() != null ? job.getCompanyName() : "") + "\n";
        }
        return String.format(
                "\n\n目标岗位信息：\n职位名称：%s\n公司：%s\n岗位要求：%s\n核心技术栈：%s\n隐含要求：%s\n面试重点：%s",
                job.getTitle(),
                job.getCompanyName() != null ? job.getCompanyName() : "",
                jobAnalysis.getRequirementSummary() != null ? jobAnalysis.getRequirementSummary() : "",
                jobAnalysis.getCoreSkills() != null ? jobAnalysis.getCoreSkills() : "",
                jobAnalysis.getHiddenRequirements() != null ? jobAnalysis.getHiddenRequirements() : "",
                jobAnalysis.getInterviewFocus() != null ? jobAnalysis.getInterviewFocus() : "");
    }

    private String extractJsonFromResponse(String text) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.trim();
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    private static String toString(Object value) {
        return value != null ? value.toString() : null;
    }
}
