package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.statistics.ReviewAnalyticsVO;

public interface ReviewAnalyticsService {
    ReviewAnalyticsVO getReviewAnalytics(Long userId);
}
