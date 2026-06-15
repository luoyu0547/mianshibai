package com.mianshiba.ai.controller;

import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.service.AdminJobCrawlService;
import com.mianshiba.ai.service.BrowserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminJobCrawlController.class)
class AdminJobCrawlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminJobCrawlService adminJobCrawlService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public FileUploadProperties fileUploadProperties() {
            FileUploadProperties properties = new FileUploadProperties();
            properties.setUploadDir("uploads");
            properties.setPublicPrefix("/uploads");
            return properties;
        }
    }

    @Test
    void startPlatformAuth_returnsAuthStartResult() throws Exception {
        BrowserSessionService.AuthStartResult result =
                new BrowserSessionService.AuthStartResult("boss", "/tmp/profile/boss");
        when(adminJobCrawlService.startPlatformAuth(eq("Bearer token"), eq("boss"))).thenReturn(result);

        mockMvc.perform(post("/api/admin/job-crawl/platforms/boss/auth/start")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.platform").value("boss"))
                .andExpect(jsonPath("$.data.profilePath").value("/tmp/profile/boss"));
    }

    @Test
    void checkPlatformAuth_returnsAuthCheckResult() throws Exception {
        BrowserSessionService.AuthCheckResult result =
                new BrowserSessionService.AuthCheckResult("boss", "authenticated", "已授权");
        when(adminJobCrawlService.checkPlatformAuth(eq("Bearer token"), eq("boss"))).thenReturn(result);

        mockMvc.perform(post("/api/admin/job-crawl/platforms/boss/auth/check")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.platform").value("boss"))
                .andExpect(jsonPath("$.data.status").value("authenticated"))
                .andExpect(jsonPath("$.data.message").value("已授权"));
    }
}
