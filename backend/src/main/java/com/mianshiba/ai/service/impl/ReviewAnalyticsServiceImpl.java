package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.InterviewReportEnhancementMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.vo.statistics.ReviewAnalyticsVO;
import com.mianshiba.ai.service.ReviewAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAnalyticsServiceImpl implements ReviewAnalyticsService {

    private final InterviewReportEnhancementMapper enhancementMapper;
    private final InterviewReportMapper reportMapper;
    private final InterviewSessionMapper sessionMapper;

    @Override
    public ReviewAnalyticsVO getReviewAnalytics(Long userId) {
        List<InterviewReportEnhancement> enhancements = enhancementMapper.selectList(
                Wrappers.lambdaQuery(InterviewReportEnhancement.class)
                        .eq(InterviewReportEnhancement::getUserId, userId)
                        .eq(InterviewReportEnhancement::getStatus, "completed")
                        .orderByDesc(InterviewReportEnhancement::getUpdateTime)
                        .last("LIMIT 20"));

        ReviewAnalyticsVO vo = new ReviewAnalyticsVO();
        vo.setRadar(buildRadar(enhancements));
        vo.setTopSkillGaps(buildTopSkillGaps(enhancements));
        vo.setRecentScoreTrend(buildRecentScoreTrend(enhancements));
        vo.setLatestActionItems(buildLatestActionItems(enhancements));
        return vo;
    }

    private Map<String, Integer> buildRadar(List<InterviewReportEnhancement> enhancements) {
        if (enhancements.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<Integer>> collect = new HashMap<>();
        for (InterviewReportEnhancement e : enhancements) {
            Map<String, Integer> radar = e.getRadarJson();
            if (radar == null) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : radar.entrySet()) {
                collect.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : collect.entrySet()) {
            double avg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            result.put(entry.getKey(), (int) Math.round(avg));
        }
        return result;
    }

    private List<Map<String, String>> buildTopSkillGaps(List<InterviewReportEnhancement> enhancements) {
        if (enhancements.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> nameCount = new LinkedHashMap<>();
        Map<String, String> nameSeverity = new LinkedHashMap<>();
        for (InterviewReportEnhancement e : enhancements) {
            List<Map<String, String>> gaps = e.getSkillGapsJson();
            if (gaps == null) {
                continue;
            }
            for (Map<String, String> gap : gaps) {
                String name = gap.get("name");
                if (name == null) {
                    continue;
                }
                nameCount.merge(name, 1, Integer::sum);
                nameSeverity.putIfAbsent(name, gap.getOrDefault("severity", ""));
            }
        }
        return nameCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("name", entry.getKey());
                    m.put("severity", nameSeverity.getOrDefault(entry.getKey(), ""));
                    m.put("count", String.valueOf(entry.getValue()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentScoreTrend(List<InterviewReportEnhancement> enhancements) {
        List<Map<String, Object>> trend = new ArrayList<>();
        enhancements.stream().limit(5).forEach(e -> {
            if (e.getReportId() == null) {
                return;
            }
            InterviewReport report = reportMapper.selectById(e.getReportId());
            if (report == null) {
                return;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("sessionId", report.getSessionId());
            entry.put("score", report.getTotalScore());
            InterviewSession session = sessionMapper.selectById(report.getSessionId());
            entry.put("date", session != null && session.getCreateTime() != null
                    ? session.getCreateTime().toString() : "");
            trend.add(entry);
        });
        return trend;
    }

    private List<String> buildLatestActionItems(List<InterviewReportEnhancement> enhancements) {
        if (enhancements.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> items = enhancements.get(0).getActionItemsJson();
        return items != null ? items : Collections.emptyList();
    }
}
