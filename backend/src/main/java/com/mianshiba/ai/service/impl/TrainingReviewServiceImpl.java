package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.dto.training.TrainingMistakeQueryRequest;
import com.mianshiba.ai.model.entity.TrainingAnswer;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingMastery;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.model.vo.training.TrainingMasterySummaryVO;
import com.mianshiba.ai.model.vo.training.TrainingMasteryVO;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;
import com.mianshiba.ai.service.TrainingReviewService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingReviewServiceImpl implements TrainingReviewService {

    private static final String TARGET_TYPE_TOPIC = "topic";
    private static final String TARGET_TYPE_SKILL_TAG = "skill_tag";
    private static final Set<String> WEAK_LEVELS = Set.of("weak", "basic");

    private final JwtUtils jwtUtils;
    private final TrainingQuestionMapper questionMapper;
    private final TrainingAnswerReviewMapper reviewMapper;
    private final TrainingMasteryMapper masteryMapper;
    private final TrainingAnswerMapper answerMapper;

    @Override
    public List<TrainingMistakeVO> listMistakes(String authorizationHeader, TrainingMistakeQueryRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        LambdaQueryWrapper<TrainingQuestion> qWrapper = Wrappers.lambdaQuery(TrainingQuestion.class)
                .eq(TrainingQuestion::getUserId, userId)
                .in(TrainingQuestion::getStatus, "reviewed", "mastered", "answered");
        if (request != null && StringUtils.hasText(request.getTopic())) {
            qWrapper.eq(TrainingQuestion::getTopic, request.getTopic());
        }
        List<TrainingQuestion> questions = questionMapper.selectList(qWrapper);

        List<TrainingMistakeVO> mistakes = new ArrayList<>();
        for (TrainingQuestion question : questions) {
            if (request == null || !Boolean.TRUE.equals(request.getIncludeMastered())) {
                if ("mastered".equals(question.getStatus())) {
                    continue;
                }
            }

            TrainingAnswerReview latestReview = reviewMapper.selectOne(
                    Wrappers.lambdaQuery(TrainingAnswerReview.class)
                            .eq(TrainingAnswerReview::getQuestionId, question.getId())
                            .eq(TrainingAnswerReview::getUserId, userId)
                            .orderByDesc(TrainingAnswerReview::getCreateTime)
                            .last("LIMIT 1"));

            if (latestReview == null) {
                continue;
            }

            boolean isWeak = WEAK_LEVELS.contains(latestReview.getMasteryLevel());
            boolean isLowScore = latestReview.getTotalScore() != null && latestReview.getTotalScore() < 70;
            if (!isWeak && !isLowScore) {
                continue;
            }

            if (request != null && StringUtils.hasText(request.getMasteryLevel())
                    && !request.getMasteryLevel().equals(latestReview.getMasteryLevel())) {
                continue;
            }
            if (request != null && request.getScoreMax() != null
                    && latestReview.getTotalScore() != null
                    && latestReview.getTotalScore() > request.getScoreMax()) {
                continue;
            }

            TrainingAnswer latestAnswer = answerMapper.selectOne(
                    Wrappers.lambdaQuery(TrainingAnswer.class)
                            .eq(TrainingAnswer::getQuestionId, question.getId())
                            .eq(TrainingAnswer::getUserId, userId)
                            .orderByDesc(TrainingAnswer::getCreateTime)
                            .last("LIMIT 1"));

            TrainingMistakeVO vo = new TrainingMistakeVO();
            vo.setQuestionId(question.getId());
            vo.setPlanId(question.getPlanId());
            vo.setTitle(question.getTitle());
            vo.setContent(question.getContent());
            vo.setTopic(question.getTopic());
            vo.setSkillTags(question.getSkillTags() != null ? question.getSkillTags() : Collections.emptyList());
            vo.setDifficulty(question.getDifficulty());
            vo.setStatus(question.getStatus());
            vo.setLatestScore(latestReview.getTotalScore());
            vo.setMasteryLevel(latestReview.getMasteryLevel());
            vo.setMistakes(latestReview.getMistakesJson() != null ? latestReview.getMistakesJson() : Collections.emptyList());
            vo.setMissingPoints(latestReview.getMissingPointsJson() != null ? latestReview.getMissingPointsJson() : Collections.emptyList());
            vo.setSuggestions(latestReview.getSuggestionsJson() != null ? latestReview.getSuggestionsJson() : Collections.emptyList());
            vo.setRecommendedAnswer(latestReview.getRecommendedAnswer());
            vo.setLastAnsweredAt(latestAnswer != null ? latestAnswer.getCreateTime() : null);
            mistakes.add(vo);
        }

        mistakes.sort(Comparator
                .<TrainingMistakeVO, Integer>comparing(m -> "weak".equals(m.getMasteryLevel()) ? 0 : 1)
                .thenComparing(m -> m.getLatestScore() != null ? m.getLatestScore() : 100));

        if (mistakes.size() > 100) {
            mistakes = mistakes.subList(0, 100);
        }
        return mistakes;
    }

    @Override
    public List<TrainingMasteryVO> listTopicMastery(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<TrainingMastery> masteries = masteryMapper.selectList(
                Wrappers.lambdaQuery(TrainingMastery.class)
                        .eq(TrainingMastery::getUserId, userId)
                        .eq(TrainingMastery::getTargetType, TARGET_TYPE_TOPIC)
                        .orderByAsc(TrainingMastery::getMasteryLevel)
                        .orderByAsc(TrainingMastery::getAverageScore));
        return masteries.stream().map(this::toMasteryVO).collect(Collectors.toList());
    }

    @Override
    public List<TrainingMasteryVO> listSkillTagMastery(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<TrainingMastery> masteries = masteryMapper.selectList(
                Wrappers.lambdaQuery(TrainingMastery.class)
                        .eq(TrainingMastery::getUserId, userId)
                        .eq(TrainingMastery::getTargetType, TARGET_TYPE_SKILL_TAG)
                        .orderByAsc(TrainingMastery::getMasteryLevel)
                        .orderByAsc(TrainingMastery::getAverageScore));
        return masteries.stream().map(this::toMasteryVO).collect(Collectors.toList());
    }

    @Override
    public TrainingMasterySummaryVO getMasterySummary(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<TrainingMastery> topicMasteries = masteryMapper.selectList(
                Wrappers.lambdaQuery(TrainingMastery.class)
                        .eq(TrainingMastery::getUserId, userId)
                        .eq(TrainingMastery::getTargetType, TARGET_TYPE_TOPIC));
        TrainingMasterySummaryVO summary = new TrainingMasterySummaryVO();
        summary.setWeak(topicMasteries.stream().filter(m -> "weak".equals(m.getMasteryLevel())).count());
        summary.setBasic(topicMasteries.stream().filter(m -> "basic".equals(m.getMasteryLevel())).count());
        summary.setGood(topicMasteries.stream().filter(m -> "good".equals(m.getMasteryLevel())).count());
        summary.setMastered(topicMasteries.stream().filter(m -> "mastered".equals(m.getMasteryLevel())).count());
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rebuildMastery(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        masteryMapper.delete(Wrappers.lambdaQuery(TrainingMastery.class).eq(TrainingMastery::getUserId, userId));
        List<TrainingQuestion> questions = questionMapper.selectList(
                Wrappers.lambdaQuery(TrainingQuestion.class)
                        .eq(TrainingQuestion::getUserId, userId)
                        .in(TrainingQuestion::getStatus, "reviewed", "mastered"));
        for (TrainingQuestion q : questions) {
            refreshMasteryForQuestion(userId, q.getId());
        }
        return true;
    }

    @Override
    public void refreshMasteryForQuestion(Long userId, Long questionId) {
        TrainingQuestion question = questionMapper.selectById(questionId);
        if (question == null || !question.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TRAINING_QUESTION_NOT_FOUND_ERROR);
        }

        List<TrainingAnswerReview> reviews = reviewMapper.selectList(
                Wrappers.lambdaQuery(TrainingAnswerReview.class)
                        .eq(TrainingAnswerReview::getUserId, userId)
                        .eq(TrainingAnswerReview::getQuestionId, questionId));

        upsertMastery(userId, TARGET_TYPE_TOPIC, question.getTopic(), reviews);

        List<String> skillTags = question.getSkillTags();
        if (skillTags != null) {
            for (String tag : skillTags) {
                upsertMastery(userId, TARGET_TYPE_SKILL_TAG, tag, reviews);
            }
        }
    }

    private void upsertMastery(Long userId, String targetType, String targetName, List<TrainingAnswerReview> reviews) {
        int practiceCount = reviews.size();
        int questionCount = 1;
        int weakCount = (int) reviews.stream().filter(r -> WEAK_LEVELS.contains(r.getMasteryLevel())).count();
        int masteredCount = (int) reviews.stream().filter(r -> "mastered".equals(r.getMasteryLevel())).count();
        BigDecimal averageScore = reviews.stream()
                .map(r -> r.getTotalScore() != null ? BigDecimal.valueOf(r.getTotalScore()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (practiceCount > 0) {
            averageScore = averageScore.divide(BigDecimal.valueOf(practiceCount), 2, RoundingMode.HALF_UP);
        }
        String masteryLevel = calculateMasteryLevel(averageScore, weakCount, practiceCount);
        LocalDateTime lastPracticedAt = reviews.stream()
                .map(TrainingAnswerReview::getCreateTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        TrainingMastery existing = masteryMapper.selectOne(
                Wrappers.lambdaQuery(TrainingMastery.class)
                        .eq(TrainingMastery::getUserId, userId)
                        .eq(TrainingMastery::getTargetType, targetType)
                        .eq(TrainingMastery::getTargetName, targetName));

        if (existing != null) {
            existing.setPracticeCount(practiceCount);
            existing.setQuestionCount(questionCount);
            existing.setAverageScore(averageScore);
            existing.setWeakCount(weakCount);
            existing.setMasteredCount(masteredCount);
            existing.setMasteryLevel(masteryLevel);
            existing.setLastPracticedAt(lastPracticedAt);
            masteryMapper.updateById(existing);
        } else {
            TrainingMastery m = new TrainingMastery();
            m.setUserId(userId);
            m.setTargetType(targetType);
            m.setTargetName(targetName);
            m.setPracticeCount(practiceCount);
            m.setQuestionCount(questionCount);
            m.setAverageScore(averageScore);
            m.setWeakCount(weakCount);
            m.setMasteredCount(masteredCount);
            m.setMasteryLevel(masteryLevel);
            m.setLastPracticedAt(lastPracticedAt);
            masteryMapper.insert(m);
        }
    }

    private String calculateMasteryLevel(BigDecimal averageScore, int weakCount, int practiceCount) {
        if (practiceCount == 0) {
            return "basic";
        }
        double avg = averageScore.doubleValue();
        double weakRatio = (double) weakCount / practiceCount;
        if (avg < 60 || weakRatio >= 0.5) {
            return "weak";
        }
        if (avg < 75) {
            return "basic";
        }
        if (avg < 90) {
            return "good";
        }
        return "mastered";
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        return jwtUtils.parseToken(token).userId();
    }

    private TrainingMasteryVO toMasteryVO(TrainingMastery m) {
        TrainingMasteryVO vo = new TrainingMasteryVO();
        vo.setId(m.getId());
        vo.setTargetType(m.getTargetType());
        vo.setTargetName(m.getTargetName());
        vo.setPracticeCount(m.getPracticeCount());
        vo.setQuestionCount(m.getQuestionCount());
        vo.setAverageScore(m.getAverageScore());
        vo.setWeakCount(m.getWeakCount());
        vo.setMasteredCount(m.getMasteredCount());
        vo.setMasteryLevel(m.getMasteryLevel());
        vo.setLastPracticedAt(m.getLastPracticedAt());
        return vo;
    }
}
