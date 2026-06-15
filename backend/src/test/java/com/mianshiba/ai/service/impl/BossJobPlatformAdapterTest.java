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
import static org.mockito.Mockito.mock;

/**
 * BossJobPlatformAdapter 单元测试
 */
class BossJobPlatformAdapterTest {

    private final BrowserSessionService browserSessionService = mock(BrowserSessionService.class);
    private final BossJobPlatformAdapter adapter = new BossJobPlatformAdapter(browserSessionService);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从 fixture HTML 抽取职位字段
     */
    @Test
    void fallbackExtract_shouldExtractFieldsFromFixtureHtml() throws IOException {
        // 1. 读取 fixture HTML 文件
        String html;
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("job-sourcing/boss-detail.html")) {
            assertThat(inputStream).isNotNull();
            html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 2. 构建抓取的页面信息
        FetchedJobPage page = new FetchedJobPage(
                "https://www.zhipin.com/job_detail/example.html",
                "https://www.zhipin.com/job_detail/example.html",
                "Java 后端开发工程师-示例科技",
                null,
                html,
                "boss",
                true
        );

        // 3. 执行兜底解析
        ExtractedJobCard card = adapter.fallbackExtract(page);

        // 4. 验证基础字段抽取正确
        assertThat(card.title()).isEqualTo("Java 后端开发工程师");
        assertThat(card.companyName()).isEqualTo("示例科技");
        assertThat(card.city()).isEqualTo("深圳");
        assertThat(card.salaryRange()).isEqualTo("20-35K");
        assertThat(card.experienceRequirement()).isEqualTo("3-5年");
        assertThat(card.educationRequirement()).isEqualTo("本科");

        // 5. 验证技术栈 JSON 包含期望关键词
        JsonNode techStack = objectMapper.readTree(card.techStackJson());
        List<String> tags = new ArrayList<>();
        techStack.forEach(node -> tags.add(node.asText()));
        assertThat(tags).contains("Java", "Spring Boot", "MySQL", "Redis");
    }

    /**
     * 对包含授权拦截关键词的 HTML 应判定为授权墙
     */
    @Test
    void isAuthWall_shouldReturnTrueForAuthKeywords() {
        // 1. 准备包含登录、验证码、安全验证的 HTML
        String authWallHtml = "<html><body>请先登录，需要验证码，正在进行安全验证</body></html>";

        // 2. 验证判定为授权墙
        assertThat(adapter.isAuthWall(authWallHtml)).isTrue();
    }

    /**
     * 对正常职位详情 HTML 不应判定为授权墙
     */
    @Test
    void isAuthWall_shouldReturnFalseForNormalPage() {
        // 1. 准备正常职位详情 HTML
        String normalHtml = "<html><body><h1>Java 后端开发工程师</h1><div>示例科技</div></body></html>";

        // 2. 验证不判定为授权墙
        assertThat(adapter.isAuthWall(normalHtml)).isFalse();
    }

    /**
     * 当页面为授权墙时，兜底解析应抛出业务异常
     */
    @Test
    void fallbackExtract_shouldThrowWhenAuthWall() {
        // 1. 构建授权墙页面
        FetchedJobPage page = new FetchedJobPage(
                "https://www.zhipin.com/job_detail/example.html",
                "https://www.zhipin.com/job_detail/example.html",
                "",
                null,
                "<html><body>需要登录才能查看</body></html>",
                "boss",
                true
        );

        // 2. 验证抛出职位抓取异常，且错误码为 JOB_CRAWL_ERROR
        assertThatThrownBy(() -> adapter.fallbackExtract(page))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.JOB_CRAWL_ERROR.getCode());
    }
}
