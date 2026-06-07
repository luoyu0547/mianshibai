package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.dashboard.DashboardVO;

public interface DashboardService {
    DashboardVO getDashboard(String authorizationHeader);
}
