package com.mianshiba.ai.service;

import com.mianshiba.ai.service.impl.JobRecommendServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobRecommendServiceImplTest {

    private final JobRecommendService service = new JobRecommendServiceImpl();

    @Test
    void calculateTotalScoreUsesOpportunityCostWeights() {
        int score = service.calculateTotalScore(80, 90, 70, 60, 50);

        assertThat(score).isEqualTo(76);
    }

    @Test
    void recommendReturnsRecommendedForHighScoreAndGoodMatch() {
        String recommendation = service.recommend(82, 78, 70);

        assertThat(recommendation).isEqualTo("recommended");
    }

    @Test
    void recommendReturnsStretchForHighGrowthButMediumMatch() {
        String recommendation = service.recommend(62, 88, 84);

        assertThat(recommendation).isEqualTo("stretch");
    }

    @Test
    void recommendReturnsNotRecommendedForLowMatch() {
        String recommendation = service.recommend(35, 80, 70);

        assertThat(recommendation).isEqualTo("not_recommended");
    }
}
