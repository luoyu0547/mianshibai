package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.InterviewTurnReviewMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurnReview;
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
        InterviewReportEnhancement task = enhancementMapper.selectById(enhancementId);
        if (task == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_ENHANCE_ERROR);
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
}
