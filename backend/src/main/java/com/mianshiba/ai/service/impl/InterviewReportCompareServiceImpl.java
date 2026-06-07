package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.interview.InterviewReportCompareVO;
import com.mianshiba.ai.model.vo.interview.InterviewScoreDeltaVO;
import com.mianshiba.ai.service.InterviewReportCompareService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewReportCompareServiceImpl implements InterviewReportCompareService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewReportMapper reportMapper;
    private final InterviewReportEnhancementMapper enhancementMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    public InterviewReportCompareVO compare(String authorizationHeader, Long baseSessionId, Long targetSessionId) {
        Long userId = resolveUserId(authorizationHeader);

        InterviewSession baseSession = sessionMapper.selectById(baseSessionId);
        InterviewSession targetSession = sessionMapper.selectById(targetSessionId);
        if (baseSession == null || targetSession == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND_ERROR);
        }
        if (!userId.equals(baseSession.getUserId()) || !userId.equals(targetSession.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        InterviewReport baseReport = reportMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReport.class)
                        .eq(InterviewReport::getSessionId, baseSessionId));
        InterviewReport targetReport = reportMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReport.class)
                        .eq(InterviewReport::getSessionId, targetSessionId));
        if (baseReport == null || targetReport == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND_ERROR);
        }

        List<InterviewScoreDeltaVO> dimensions = List.of(
                buildDelta("accuracy", "技术准确性", baseReport.getAccuracyScore(), targetReport.getAccuracyScore()),
                buildDelta("clarity", "表达清晰度", baseReport.getClarityScore(), targetReport.getClarityScore()),
                buildDelta("depth", "项目深度", baseReport.getDepthScore(), targetReport.getDepthScore()),
                buildDelta("matching", "岗位匹配度", baseReport.getMatchingScore(), targetReport.getMatchingScore())
        );

        List<String> newSkillGaps = Collections.emptyList();
        List<String> resolvedSkillGaps = Collections.emptyList();

        InterviewReportEnhancement baseEnhancement = enhancementMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getSessionId, baseSessionId));
        InterviewReportEnhancement targetEnhancement = enhancementMapper.selectOne(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getSessionId, targetSessionId));

        if (baseEnhancement != null && targetEnhancement != null
                && "completed".equals(baseEnhancement.getStatus())
                && "completed".equals(targetEnhancement.getStatus())) {
            Set<String> baseNames = extractSkillGapNames(baseEnhancement);
            Set<String> targetNames = extractSkillGapNames(targetEnhancement);

            newSkillGaps = targetNames.stream()
                    .filter(name -> !baseNames.contains(name))
                    .collect(Collectors.toList());
            resolvedSkillGaps = baseNames.stream()
                    .filter(name -> !targetNames.contains(name))
                    .collect(Collectors.toList());
        }

        List<String> summary = buildSummary(baseReport, targetReport, dimensions);

        InterviewReportCompareVO vo = new InterviewReportCompareVO();
        vo.setBaseSessionId(baseSessionId);
        vo.setTargetSessionId(targetSessionId);
        vo.setBaseTotalScore(baseReport.getTotalScore());
        vo.setTargetTotalScore(targetReport.getTotalScore());
        vo.setTotalDelta(targetReport.getTotalScore() - baseReport.getTotalScore());
        vo.setDimensions(dimensions);
        vo.setNewSkillGaps(newSkillGaps);
        vo.setResolvedSkillGaps(resolvedSkillGaps);
        vo.setSummary(summary);
        return vo;
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

    private InterviewScoreDeltaVO buildDelta(String key, String label, Integer baseScore, Integer targetScore) {
        InterviewScoreDeltaVO delta = new InterviewScoreDeltaVO();
        delta.setKey(key);
        delta.setLabel(label);
        delta.setBaseScore(baseScore);
        delta.setTargetScore(targetScore);
        delta.setDelta(targetScore - baseScore);
        return delta;
    }

    private Set<String> extractSkillGapNames(InterviewReportEnhancement enhancement) {
        if (enhancement.getSkillGapsJson() == null) {
            return Collections.emptySet();
        }
        return enhancement.getSkillGapsJson().stream()
                .map(map -> map.get("name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<String> buildSummary(InterviewReport baseReport, InterviewReport targetReport, List<InterviewScoreDeltaVO> dimensions) {
        List<String> summary = new ArrayList<>();

        int totalDelta = targetReport.getTotalScore() - baseReport.getTotalScore();
        if (totalDelta > 0) {
            summary.add("总分提升了 " + totalDelta + " 分，从 " + baseReport.getTotalScore() + " 上升到 " + targetReport.getTotalScore());
        } else if (totalDelta < 0) {
            summary.add("总分下降了 " + Math.abs(totalDelta) + " 分，从 " + baseReport.getTotalScore() + " 降至 " + targetReport.getTotalScore());
        } else {
            summary.add("总分保持不变，为 " + baseReport.getTotalScore() + " 分");
        }

        InterviewScoreDeltaVO strongest = dimensions.stream()
                .max((a, b) -> Integer.compare(a.getDelta(), b.getDelta()))
                .orElse(null);
        if (strongest != null && strongest.getDelta() > 0) {
            summary.add("提升最大的维度是「" + strongest.getLabel() + "」，提高了 " + strongest.getDelta() + " 分");
        }

        InterviewScoreDeltaVO weakest = dimensions.stream()
                .min((a, b) -> Integer.compare(a.getDelta(), b.getDelta()))
                .orElse(null);
        if (weakest != null && weakest.getDelta() < 0) {
            summary.add("下降最大的维度是「" + weakest.getLabel() + "」，降低了 " + Math.abs(weakest.getDelta()) + " 分");
        }

        return summary;
    }
}
