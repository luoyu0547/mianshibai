package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobRecommendService;
import org.springframework.stereotype.Service;

@Service
public class JobRecommendServiceImpl implements JobRecommendService {

    @Override
    public int calculateTotalScore(int matchScore, int growthScore, int techGrowthScore, int salaryCityScore, int experienceFitScore) {
        double score = matchScore * 0.4
                + growthScore * 0.25
                + techGrowthScore * 0.15
                + salaryCityScore * 0.1
                + experienceFitScore * 0.1;
        return (int) Math.round(score);
    }

    @Override
    public String recommend(int matchScore, int growthScore, int techGrowthScore) {
        if (matchScore < 45) {
            return "not_recommended";
        }
        if (matchScore >= 75 && growthScore >= 65) {
            return "recommended";
        }
        if (matchScore >= 55 && growthScore >= 80 && techGrowthScore >= 75) {
            return "stretch";
        }
        return "cautious";
    }
}
