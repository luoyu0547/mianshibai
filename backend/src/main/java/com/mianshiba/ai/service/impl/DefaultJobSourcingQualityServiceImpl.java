package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobSourcingExtractService;
import com.mianshiba.ai.service.JobSourcingQualityService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultJobSourcingQualityServiceImpl implements JobSourcingQualityService {

    @Override
    public QualityResult score(JobSourcingExtractService.ExtractedJobCard card) {
        int score = 20;
        List<String> warnings = new ArrayList<>();
        score += addIfPresent(card.title(), 12, "标题缺失", warnings);
        score += addIfPresent(card.companyName(), 12, "公司缺失", warnings);
        score += addIfPresent(card.city(), 8, "城市缺失", warnings);
        score += addIfPresent(card.salaryRange(), 8, "薪资缺失", warnings);
        score += addIfPresent(card.experienceRequirement(), 8, "经验要求缺失", warnings);
        score += addIfPresent(card.educationRequirement(), 8, "学历缺失", warnings);
        score += addIfPresent(card.jobDescription(), 12, "岗位职责缺失", warnings);
        score += addIfPresent(card.jobRequirement(), 12, "岗位要求缺失", warnings);
        score += card.confidenceScore() >= 80 ? 8 : 0;
        return new QualityResult(Math.min(score, 100), toJsonArray(warnings));
    }

    private int addIfPresent(String value, int points, String warning, List<String> warnings) {
        if (StringUtils.hasText(value)) {
            return points;
        }
        warnings.add(warning);
        return 0;
    }

    private String toJsonArray(List<String> warnings) {
        return "[" + warnings.stream().map(w -> "\"" + w + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]";
    }
}
