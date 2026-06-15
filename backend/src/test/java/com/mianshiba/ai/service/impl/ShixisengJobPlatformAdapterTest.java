package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.service.BrowserSessionService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ShixisengJobPlatformAdapter 单元测试
 */
class ShixisengJobPlatformAdapterTest {

    private final ShixisengJobPlatformAdapter adapter = new ShixisengJobPlatformAdapter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 平台标识应为 shixiseng
     */
    @Test
    void platform_shouldReturnShixiseng() {
        assertThat(adapter.platform()).isEqualTo("shixiseng");
    }

    /**
     * 不需要登录授权
     */
    @Test
    void requiresAuth_shouldReturnFalse() {
        assertThat(adapter.requiresAuth()).isFalse();
    }

    /**
     * 授权检查应为公开访问
     */
    @Test
    void checkAuth_shouldReturnAuthorized() {
        BrowserSessionService.AuthCheckResult result = adapter.checkAuth();

        assertThat(result.platform()).isEqualTo("shixiseng");
        assertThat(result.status()).isEqualTo("authorized");
        assertThat(result.message()).isEqualTo("公开访问");
    }

    /**
     * 真实详情页抓取应抛出业务异常
     */
    @Test
    void fetchDetail_shouldThrowBusinessException() {
        assertThatThrownBy(() -> adapter.fetchDetail("https://www.shixiseng.com/intern/example"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.JOB_CRAWL_ERROR.getCode());
    }

    /**
     * 从 fixture HTML 抽取职位字段
     */
    @Test
    void fallbackExtract_shouldExtractFieldsFromFixtureHtml() throws IOException {
        // 1. 读取 fixture HTML 文件
        String html;
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("job-sourcing/shixiseng-detail.html")) {
            assertThat(inputStream).isNotNull();
            html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 2. 构建抓取的页面信息
        FetchedJobPage page = new FetchedJobPage(
                "https://www.shixiseng.com/intern/example",
                "https://www.shixiseng.com/intern/example",
                "后端开发实习生-样例网络",
                null,
                html,
                "shixiseng",
                false
        );

        // 3. 执行兜底解析
        ExtractedJobCard card = adapter.fallbackExtract(page);

        // 4. 验证基础字段抽取正确
        assertThat(card.title()).isEqualTo("后端开发实习生");
        assertThat(card.companyName()).isEqualTo("样例网络");
        assertThat(card.city()).isEqualTo("北京");
        assertThat(card.salaryRange()).isEqualTo("200-300/天");
        assertThat(card.educationRequirement()).isEqualTo("本科");

        // 5. 验证标签 JSON 包含实习信息
        JsonNode tags = objectMapper.readTree(card.tagsJson());
        List<String> tagList = new ArrayList<>();
        tags.forEach(node -> tagList.add(node.asText()));
        assertThat(tagList).contains("每周 4 天", "实习 3 个月");

        // 6. 验证技术栈 JSON 包含期望关键词
        JsonNode techStack = objectMapper.readTree(card.techStackJson());
        List<String> techList = new ArrayList<>();
        techStack.forEach(node -> techList.add(node.asText()));
        assertThat(techList).contains("Java", "Spring Boot", "MySQL");
    }
}
