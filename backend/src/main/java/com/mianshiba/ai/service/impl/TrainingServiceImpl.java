package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.AlgorithmRecommendationMapper;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.entity.AlgorithmRecommendation;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.TrainingAnswer;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingPlan;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.model.vo.training.AlgorithmRecommendationVO;
import com.mianshiba.ai.model.vo.training.TrainingAnswerReviewVO;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import com.mianshiba.ai.service.TrainingReviewService;
import com.mianshiba.ai.service.TrainingService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 八股训练服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private static final Set<String> PLAN_STATUSES = Set.of("active", "completed", "archived");
    private static final Set<String> QUESTION_STATUSES = Set.of("pending", "answered", "reviewed", "mastered", "skipped");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private static final String PLAN_SYSTEM_PROMPT =
            "你是一位资深 Java 后端面试教练。请根据用户面试短板和技术背景，生成一份八股文训练计划。\n" +
            "只返回 JSON，不要解释。JSON 结构：\n" +
            "{\n" +
            "  \"title\": \"计划标题\",\n" +
            "  \"summary\": \"计划说明\",\n" +
            "  \"targetDays\": 7,\n" +
            "  \"focusTopics\": [\"JVM\", \"MySQL\"],\n" +
            "  \"questions\": [{\"dayIndex\":1,\"title\":\"题目标题\",\"content\":\"题目正文\"," +
            "\"topic\":\"Java\",\"skillTags\":[\"JVM\"],\"difficulty\":\"medium\"," +
            "\"referenceAnswer\":\"参考答案要点\",\"followUpQuestions\":[\"追问1\"]}],\n" +
            "  \"algorithmRecommendations\": [{\"category\":\"数组\",\"platform\":\"LeetCode\"," +
            "\"problemRef\":\"LeetCode 1, 15, 26\",\"reason\":\"巩固高频数组题\"}]\n" +
            "}\n\n" +
            "规则：\n" +
            "- 每天安排 3-4 道八股题\n" +
            "- 八股题覆盖 Java、Spring、MySQL、Redis、JVM、计算机网络、操作系统、分布式、项目经历\n" +
            "- 难度根据用户水平调整\n" +
            "- 算法只推荐去 LeetCode、力扣、CodeTop 等平台刷题\n" +
            "- 算法不要求在本系统提交代码";

    private static final String REVIEW_SYSTEM_PROMPT =
            "你是一位严格的程序员八股面试官。请批改用户的八股答案。\n" +
            "只返回 JSON，结构：\n" +
            "{\n" +
            "  \"totalScore\": 75,\n" +
            "  \"accuracyScore\": 70,\n" +
            "  \"clarityScore\": 80,\n" +
            "  \"depthScore\": 65,\n" +
            "  \"projectScore\": 60,\n" +
            "  \"strengths\": [\"优点1\"],\n" +
            "  \"mistakes\": [\"错误1\"],\n" +
            "  \"missingPoints\": [\"遗漏1\"],\n" +
            "  \"suggestions\": [\"建议1\"],\n" +
            "  \"recommendedAnswer\": \"推荐回答，适合面试口述\",\n" +
            "  \"followUpQuestions\": [\"追问1\"],\n" +
            "  \"masteryLevel\": \"basic\"\n" +
            "}\n\n" +
            "masteryLevel 只能是 weak/basic/good/mastered。\n" +
            "推荐回答要适合面试口述，不要只给百科定义。\n" +
            "评分关注：技术准确性、原理深度、表达结构、项目结合。";

    private final JwtUtils jwtUtils;
    private final ChatClient chatClient;
    private final TrainingPlanMapper planMapper;
    private final TrainingQuestionMapper questionMapper;
    private final TrainingAnswerMapper answerMapper;
    private final TrainingAnswerReviewMapper reviewMapper;
    private final AlgorithmRecommendationMapper algorithmMapper;
    private final InterviewReportEnhancementMapper enhancementMapper;
    private final JobApplicationMapper applicationMapper;
    private final ResumeMapper resumeMapper;
    private final TrainingReviewService trainingReviewService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainingPlanVO generatePlan(String authorizationHeader, TrainingPlanGenerateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        String sourceType = (request.getSourceType() != null && !request.getSourceType().isBlank())
                ? request.getSourceType() : "manual";
        int targetDays = request.getTargetDays() != null ? Math.max(1, Math.min(request.getTargetDays(), 14)) : 7;

        planMapper.update(null, Wrappers.lambdaUpdate(TrainingPlan.class)
                .eq(TrainingPlan::getUserId, userId)
                .eq(TrainingPlan::getStatus, "active")
                .set(TrainingPlan::getStatus, "archived"));

        String contextMessage = buildPlanContext(userId, request);

        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                    .system(PLAN_SYSTEM_PROMPT)
                    .user(contextMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 生成训练计划失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> planData;
        try {
            planData = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("AI 训练计划响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setTitle((String) planData.getOrDefault("title", "八股训练计划"));
        plan.setSourceType(sourceType);
        plan.setSourceId(request.getSourceId());
        plan.setTargetDays(targetDays);
        plan.setStatus("active");
        plan.setSummary((String) planData.getOrDefault("summary", ""));

        @SuppressWarnings("unchecked")
        List<String> focusTopics = (List<String>) planData.get("focusTopics");
        plan.setFocusTopics(focusTopics != null ? focusTopics : Collections.emptyList());

        planMapper.insert(plan);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questionsData = (List<Map<String, Object>>) planData.get("questions");
        if (questionsData != null) {
            for (Map<String, Object> qData : questionsData) {
                TrainingQuestion question = new TrainingQuestion();
                question.setUserId(userId);
                question.setPlanId(plan.getId());
                question.setDayIndex(qData.get("dayIndex") != null ? ((Number) qData.get("dayIndex")).intValue() : 1);
                question.setTitle((String) qData.getOrDefault("title", ""));
                question.setContent((String) qData.getOrDefault("content", ""));
                question.setTopic((String) qData.getOrDefault("topic", ""));
                @SuppressWarnings("unchecked")
                List<String> skillTags = (List<String>) qData.get("skillTags");
                question.setSkillTags(skillTags != null ? skillTags : Collections.emptyList());
                question.setDifficulty((String) qData.getOrDefault("difficulty", "medium"));
                question.setSourceType(sourceType);
                question.setReferenceAnswer((String) qData.getOrDefault("referenceAnswer", ""));
                @SuppressWarnings("unchecked")
                List<String> followUpQuestions = (List<String>) qData.get("followUpQuestions");
                question.setFollowUpQuestions(followUpQuestions != null ? followUpQuestions : Collections.emptyList());
                question.setStatus("pending");
                questionMapper.insert(question);
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> algoData = (List<Map<String, Object>>) planData.get("algorithmRecommendations");
        if (algoData != null) {
            for (Map<String, Object> aData : algoData) {
                AlgorithmRecommendation rec = new AlgorithmRecommendation();
                rec.setUserId(userId);
                rec.setPlanId(plan.getId());
                rec.setCategory((String) aData.getOrDefault("category", ""));
                rec.setPlatform((String) aData.getOrDefault("platform", "LeetCode"));
                rec.setProblemRef((String) aData.getOrDefault("problemRef", ""));
                rec.setReason((String) aData.getOrDefault("reason", ""));
                rec.setCompleted(0);
                algorithmMapper.insert(rec);
            }
        }

        return toPlanVO(plan);
    }

    @Override
    public TrainingPlanVO getActivePlan(String authorizationHeader) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询活跃计划
        TrainingPlan plan = planMapper.selectOne(
                Wrappers.lambdaQuery(TrainingPlan.class)
                        .eq(TrainingPlan::getUserId, userId)
                        .eq(TrainingPlan::getStatus, "active"));

        if (plan == null) {
            return null;
        }

        // 3. 构建完整 VO
        return toPlanVO(plan);
    }

    @Override
    public List<TrainingPlanVO> listPlans(String authorizationHeader) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询所有计划，按创建时间倒序
        List<TrainingPlan> plans = planMapper.selectList(
                Wrappers.lambdaQuery(TrainingPlan.class)
                        .eq(TrainingPlan::getUserId, userId)
                        .orderByDesc(TrainingPlan::getCreateTime));

        // 3. 转换为 VO（不含子项）
        return plans.stream()
                .map(this::toPlanVO)
                .collect(Collectors.toList());
    }

    @Override
    public TrainingPlanVO getPlan(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取计划
        TrainingPlan plan = getOwnedPlan(authorizationHeader, id);

        // 2. 构建完整 VO
        return toPlanVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean archivePlan(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取计划
        TrainingPlan plan = getOwnedPlan(authorizationHeader, id);

        // 2. 更新状态为 archived
        plan.setStatus("archived");
        planMapper.updateById(plan);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completePlan(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取计划
        TrainingPlan plan = getOwnedPlan(authorizationHeader, id);

        // 2. 更新状态为 completed
        plan.setStatus("completed");
        planMapper.updateById(plan);

        return true;
    }

    @Override
    public TrainingQuestionVO getQuestion(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取题目
        TrainingQuestion question = getOwnedQuestion(authorizationHeader, id);

        // 2. 查询最新评审以获取分数和掌握等级
        TrainingAnswerReview latestReview = reviewMapper.selectOne(
                Wrappers.lambdaQuery(TrainingAnswerReview.class)
                        .eq(TrainingAnswerReview::getQuestionId, id)
                        .orderByDesc(TrainingAnswerReview::getCreateTime)
                        .last("LIMIT 1"));

        return toQuestionVO(question, latestReview);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean markQuestionMastered(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取题目
        Long userId = resolveUserId(authorizationHeader);
        TrainingQuestion question = getOwnedQuestion(authorizationHeader, id);

        // 2. 更新状态为 mastered
        question.setStatus("mastered");
        questionMapper.updateById(question);

        trainingReviewService.refreshMasteryForQuestion(userId, id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean skipQuestion(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取题目
        TrainingQuestion question = getOwnedQuestion(authorizationHeader, id);

        // 2. 更新状态为 skipped
        question.setStatus("skipped");
        questionMapper.updateById(question);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainingAnswerVO submitAnswer(String authorizationHeader, Long questionId, TrainingAnswerSubmitRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        if (request.getAnswerText() == null || request.getAnswerText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案不能为空");
        }
        if (request.getAnswerText().length() > 8000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案长度不能超过8000字");
        }

        TrainingQuestion question = getOwnedQuestion(authorizationHeader, questionId);

        TrainingAnswer answer = new TrainingAnswer();
        answer.setUserId(userId);
        answer.setQuestionId(questionId);
        answer.setAnswerText(request.getAnswerText());
        answerMapper.insert(answer);

        String userMessage = "题目：" + question.getTitle() + "\n题目内容：" + question.getContent() + "\n用户答案：" + request.getAnswerText();

        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                    .system(REVIEW_SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 批改训练答案失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> reviewData;
        try {
            reviewData = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("AI 批改响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        TrainingAnswerReview review = new TrainingAnswerReview();
        review.setUserId(userId);
        review.setQuestionId(questionId);
        review.setAnswerId(answer.getId());
        review.setTotalScore(reviewData.get("totalScore") != null ? ((Number) reviewData.get("totalScore")).intValue() : 0);
        review.setAccuracyScore(reviewData.get("accuracyScore") != null ? ((Number) reviewData.get("accuracyScore")).intValue() : 0);
        review.setClarityScore(reviewData.get("clarityScore") != null ? ((Number) reviewData.get("clarityScore")).intValue() : 0);
        review.setDepthScore(reviewData.get("depthScore") != null ? ((Number) reviewData.get("depthScore")).intValue() : 0);
        review.setProjectScore(reviewData.get("projectScore") != null ? ((Number) reviewData.get("projectScore")).intValue() : 0);

        @SuppressWarnings("unchecked")
        List<String> strengths = (List<String>) reviewData.get("strengths");
        review.setStrengthsJson(strengths != null ? strengths : Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<String> mistakes = (List<String>) reviewData.get("mistakes");
        review.setMistakesJson(mistakes != null ? mistakes : Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<String> missingPoints = (List<String>) reviewData.get("missingPoints");
        review.setMissingPointsJson(missingPoints != null ? missingPoints : Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) reviewData.get("suggestions");
        review.setSuggestionsJson(suggestions != null ? suggestions : Collections.emptyList());
        review.setRecommendedAnswer((String) reviewData.getOrDefault("recommendedAnswer", ""));
        @SuppressWarnings("unchecked")
        List<String> followUpQuestions = (List<String>) reviewData.get("followUpQuestions");
        review.setFollowUpQuestionsJson(followUpQuestions != null ? followUpQuestions : Collections.emptyList());
        review.setMasteryLevel((String) reviewData.getOrDefault("masteryLevel", "basic"));

        reviewMapper.insert(review);

        question.setStatus("reviewed");
        questionMapper.updateById(question);

        trainingReviewService.refreshMasteryForQuestion(userId, questionId);

        return toAnswerVO(answer, review);
    }

    @Override
    public List<TrainingAnswerVO> listQuestionAnswers(String authorizationHeader, Long questionId) {
        // 1. 解析用户身份并获取题目
        TrainingQuestion question = getOwnedQuestion(authorizationHeader, questionId);

        // 2. 查询该题目所有答案
        List<TrainingAnswer> answers = answerMapper.selectList(
                Wrappers.lambdaQuery(TrainingAnswer.class)
                        .eq(TrainingAnswer::getQuestionId, questionId)
                        .orderByDesc(TrainingAnswer::getCreateTime));

        // 3. 为每个答案附带最新评审
        return answers.stream().map(answer -> {
            TrainingAnswerReview review = reviewMapper.selectOne(
                    Wrappers.lambdaQuery(TrainingAnswerReview.class)
                            .eq(TrainingAnswerReview::getAnswerId, answer.getId())
                            .orderByDesc(TrainingAnswerReview::getCreateTime)
                            .last("LIMIT 1"));
            return toAnswerVO(answer, review);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeAlgorithmRecommendation(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取推荐
        AlgorithmRecommendation rec = getOwnedAlgorithmRecommendation(authorizationHeader, id);

        // 2. 标记为已完成
        rec.setCompleted(1);
        algorithmMapper.updateById(rec);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reopenAlgorithmRecommendation(String authorizationHeader, Long id) {
        // 1. 解析用户身份并获取推荐
        AlgorithmRecommendation rec = getOwnedAlgorithmRecommendation(authorizationHeader, id);

        // 2. 标记为未完成
        rec.setCompleted(0);
        algorithmMapper.updateById(rec);

        return true;
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        return claims.userId();
    }

    private TrainingPlan getOwnedPlan(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        TrainingPlan plan = planMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TRAINING_PLAN_NOT_FOUND_ERROR);
        }
        return plan;
    }

    private TrainingQuestion getOwnedQuestion(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        TrainingQuestion question = questionMapper.selectById(id);
        if (question == null || !question.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TRAINING_QUESTION_NOT_FOUND_ERROR);
        }
        return question;
    }

    private AlgorithmRecommendation getOwnedAlgorithmRecommendation(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        AlgorithmRecommendation rec = algorithmMapper.selectById(id);
        if (rec == null || !rec.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TRAINING_ANSWER_NOT_FOUND_ERROR);
        }
        return rec;
    }

    private TrainingPlanVO toPlanVO(TrainingPlan plan) {
        TrainingPlanVO vo = new TrainingPlanVO();
        vo.setId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setSourceType(plan.getSourceType());
        vo.setSourceId(plan.getSourceId());
        vo.setTargetDays(plan.getTargetDays());
        vo.setStatus(plan.getStatus());
        vo.setSummary(plan.getSummary());
        vo.setFocusTopics(plan.getFocusTopics() != null ? plan.getFocusTopics() : Collections.emptyList());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());

        // 查询关联题目
        List<TrainingQuestion> questions = questionMapper.selectList(
                Wrappers.lambdaQuery(TrainingQuestion.class)
                        .eq(TrainingQuestion::getPlanId, plan.getId())
                        .orderByAsc(TrainingQuestion::getDayIndex));

        List<TrainingQuestionVO> questionVOs = questions.stream().map(q -> {
            TrainingAnswerReview latestReview = reviewMapper.selectOne(
                    Wrappers.lambdaQuery(TrainingAnswerReview.class)
                            .eq(TrainingAnswerReview::getQuestionId, q.getId())
                            .orderByDesc(TrainingAnswerReview::getCreateTime)
                            .last("LIMIT 1"));
            return toQuestionVO(q, latestReview);
        }).collect(Collectors.toList());
        vo.setQuestions(questionVOs);

        // 查询关联算法推荐
        List<AlgorithmRecommendation> algorithms = algorithmMapper.selectList(
                Wrappers.lambdaQuery(AlgorithmRecommendation.class)
                        .eq(AlgorithmRecommendation::getPlanId, plan.getId()));
        vo.setAlgorithmRecommendations(algorithms.stream()
                .map(this::toAlgorithmRecommendationVO)
                .collect(Collectors.toList()));

        return vo;
    }

    private TrainingQuestionVO toQuestionVO(TrainingQuestion question, TrainingAnswerReview latestReview) {
        TrainingQuestionVO vo = new TrainingQuestionVO();
        vo.setId(question.getId());
        vo.setPlanId(question.getPlanId());
        vo.setDayIndex(question.getDayIndex());
        vo.setTitle(question.getTitle());
        vo.setContent(question.getContent());
        vo.setTopic(question.getTopic());
        vo.setSkillTags(question.getSkillTags() != null ? question.getSkillTags() : Collections.emptyList());
        vo.setDifficulty(question.getDifficulty());
        vo.setSourceType(question.getSourceType());
        vo.setReferenceAnswer(question.getReferenceAnswer());
        vo.setFollowUpQuestions(question.getFollowUpQuestions() != null ? question.getFollowUpQuestions() : Collections.emptyList());
        vo.setStatus(question.getStatus());
        vo.setCreateTime(question.getCreateTime());
        vo.setUpdateTime(question.getUpdateTime());

        if (latestReview != null) {
            vo.setLatestScore(latestReview.getTotalScore());
            vo.setLatestMasteryLevel(latestReview.getMasteryLevel());
        }

        return vo;
    }

    private TrainingAnswerVO toAnswerVO(TrainingAnswer answer, TrainingAnswerReview review) {
        TrainingAnswerVO vo = new TrainingAnswerVO();
        vo.setId(answer.getId());
        vo.setQuestionId(answer.getQuestionId());
        vo.setAnswerText(answer.getAnswerText());
        vo.setCreateTime(answer.getCreateTime());

        if (review != null) {
            vo.setReview(toAnswerReviewVO(review));
        }

        return vo;
    }

    private TrainingAnswerReviewVO toAnswerReviewVO(TrainingAnswerReview review) {
        TrainingAnswerReviewVO vo = new TrainingAnswerReviewVO();
        vo.setId(review.getId());
        vo.setAnswerId(review.getAnswerId());
        vo.setTotalScore(review.getTotalScore());
        vo.setAccuracyScore(review.getAccuracyScore());
        vo.setClarityScore(review.getClarityScore());
        vo.setDepthScore(review.getDepthScore());
        vo.setProjectScore(review.getProjectScore());
        vo.setStrengths(review.getStrengthsJson() != null ? review.getStrengthsJson() : Collections.emptyList());
        vo.setMistakes(review.getMistakesJson() != null ? review.getMistakesJson() : Collections.emptyList());
        vo.setMissingPoints(review.getMissingPointsJson() != null ? review.getMissingPointsJson() : Collections.emptyList());
        vo.setSuggestions(review.getSuggestionsJson() != null ? review.getSuggestionsJson() : Collections.emptyList());
        vo.setRecommendedAnswer(review.getRecommendedAnswer());
        vo.setFollowUpQuestions(review.getFollowUpQuestionsJson() != null ? review.getFollowUpQuestionsJson() : Collections.emptyList());
        vo.setMasteryLevel(review.getMasteryLevel());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private AlgorithmRecommendationVO toAlgorithmRecommendationVO(AlgorithmRecommendation rec) {
        AlgorithmRecommendationVO vo = new AlgorithmRecommendationVO();
        vo.setId(rec.getId());
        vo.setPlanId(rec.getPlanId());
        vo.setCategory(rec.getCategory());
        vo.setPlatform(rec.getPlatform());
        vo.setProblemRef(rec.getProblemRef());
        vo.setReason(rec.getReason());
        vo.setCompleted(rec.getCompleted());
        return vo;
    }

    private String extractJsonFromResponse(String response) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return response.trim();
    }

    private String buildPlanContext(Long userId, TrainingPlanGenerateRequest request) {
        StringBuilder sb = new StringBuilder();

        List<InterviewReportEnhancement> enhancements = enhancementMapper.selectList(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getUserId, userId)
                        .eq(InterviewReportEnhancement::getStatus, "completed")
                        .orderByDesc(InterviewReportEnhancement::getCreateTime)
                        .last("LIMIT 10"));

        if (!enhancements.isEmpty()) {
            sb.append("## 面试报告分析\n");
            for (InterviewReportEnhancement e : enhancements) {
                if (e.getRadarJson() != null) {
                    sb.append("雷达评分: ").append(e.getRadarJson()).append("\n");
                }
                if (e.getSkillGapsJson() != null) {
                    sb.append("技能短板: ").append(e.getSkillGapsJson()).append("\n");
                }
                if (e.getActionItemsJson() != null) {
                    sb.append("改进建议: ").append(e.getActionItemsJson()).append("\n");
                }
            }
        }

        List<JobApplication> applications = applicationMapper.selectList(
                Wrappers.lambdaQuery(JobApplication.class)
                        .eq(JobApplication::getUserId, userId)
                        .eq(JobApplication::getStatus, "active")
                        .orderByDesc(JobApplication::getCreateTime)
                        .last("LIMIT 5"));

        if (!applications.isEmpty()) {
            sb.append("\n## 投递中的职位\n");
            for (JobApplication app : applications) {
                sb.append("- ").append(app.getJobTitle()).append(" @ ").append(app.getCompanyName()).append("\n");
            }
        }

        Resume latestResume = resumeMapper.selectOne(
                Wrappers.lambdaQuery(Resume.class)
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getIsDelete, 0)
                        .orderByDesc(Resume::getCreateTime)
                        .last("LIMIT 1"));

        if (latestResume != null) {
            sb.append("\n## 最新简历: ").append(latestResume.getTitle()).append("\n");
        }

        if (request.getTargetPosition() != null && !request.getTargetPosition().isBlank()) {
            sb.append("\n## 目标职位: ").append(request.getTargetPosition()).append("\n");
        }

        sb.append("\n请为该用户生成一份 ").append(request.getTargetDays() != null ? Math.max(1, Math.min(request.getTargetDays(), 14)) : 7)
                .append(" 天的八股文训练计划。");

        return sb.toString();
    }
}
