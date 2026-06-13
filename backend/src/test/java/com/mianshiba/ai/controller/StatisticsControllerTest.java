package com.mianshiba.ai.controller;

import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;
import com.mianshiba.ai.service.AdminService;
import com.mianshiba.ai.service.ReviewAnalyticsService;
import com.mianshiba.ai.service.StatisticsService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;
    @MockBean
    private AdminService adminService;
    @MockBean
    private ReviewAnalyticsService reviewAnalyticsService;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private UserMapper userMapper;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void getHomeStats_shouldReturnStats() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUserStatus(0);
        user.setIsDelete(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(jwtUtils.resolveToken(any())).thenReturn("valid-token");
        JwtUtils.JwtUserClaims claims = new JwtUtils.JwtUserClaims(1L, "test", "user");
        when(jwtUtils.parseToken("valid-token")).thenReturn(claims);

        HomeStatsVO stats = new HomeStatsVO();
        stats.setCompletedInterviews(3);
        stats.setTotalQuestions(15);
        stats.setPracticeDays(5);
        when(statisticsService.getHomeStats(anyLong())).thenReturn(stats);

        mockMvc.perform(get("/api/statistics/home")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.completedInterviews").value(3))
                .andExpect(jsonPath("$.data.totalQuestions").value(15))
                .andExpect(jsonPath("$.data.practiceDays").value(5));
    }
}
