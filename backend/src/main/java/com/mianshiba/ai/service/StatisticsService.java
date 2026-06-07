package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.statistics.AnalyticsOverviewVO;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;

public interface StatisticsService {
    HomeStatsVO getHomeStats(Long userId);

    AnalyticsOverviewVO getAnalyticsOverview(Long userId);
}
