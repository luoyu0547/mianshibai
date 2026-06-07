package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.AlgorithmRecommendationMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.entity.AlgorithmRecommendation;
import com.mianshiba.ai.model.entity.TrainingAnswer;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingPlan;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.model.vo.training.AlgorithmRecommendationVO;
import com.mianshiba.ai.model.vo.training.TrainingAnswerReviewVO;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import com.mianshiba.ai.service.TrainingService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    private final JwtUtils jwtUtils;
    private final TrainingPlanMapper planMapper;
    private final TrainingQuestionMapper questionMapper;
    private final TrainingAnswerMapper answerMapper;
    private final TrainingAnswerReviewMapper reviewMapper;
    private final AlgorithmRecommendationMapper algorithmMapper;

    @Override
    public TrainingPlanVO generatePlan(String authorizationHeader, TrainingPlanGenerateRequest request) {
        throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI generation not yet configured");
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
        TrainingQuestion question = getOwnedQuestion(authorizationHeader, id);

        // 2. 更新状态为 mastered
        question.setStatus("mastered");
        questionMapper.updateById(question);

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
    public TrainingAnswerVO submitAnswer(String authorizationHeader, Long questionId, TrainingAnswerSubmitRequest request) {
        throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI generation not yet configured");
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
}
