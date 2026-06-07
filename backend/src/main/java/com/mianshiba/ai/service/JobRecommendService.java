package com.mianshiba.ai.service;

public interface JobRecommendService {

    int calculateTotalScore(int matchScore, int growthScore, int techGrowthScore, int salaryCityScore, int experienceFitScore);

    String recommend(int matchScore, int growthScore, int techGrowthScore);
}
