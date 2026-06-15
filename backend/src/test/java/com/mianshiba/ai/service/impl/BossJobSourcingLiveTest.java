package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.mapper.PlatformAuthSessionMapper;
import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.model.enums.PlatformAuthStatus;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Boss 直聘职位采集实时测试
 * 在设置 RUN_LIVE_BOSS_CRAWL=true 时运行，验证 Boss 直聘真实页面采集能力。
 * 当前 discover/fetchDetail 尚未实现真实抓取，测试会自然失败，用于明确后续实现目标。
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_BOSS_CRAWL", matches = "true")
class BossJobSourcingLiveTest {

    /**
     * 平台授权会话 Mapper，模拟数据库中无授权记录
     */
    private final PlatformAuthSessionMapper platformAuthSessionMapper = mock(PlatformAuthSessionMapper.class);

    /**
     * 职位采集配置，使用默认值
     */
    private final JobSourcingProperties properties = new JobSourcingProperties();

    /**
     * 浏览器会话服务，依赖模拟的 Mapper
     */
    private final BrowserSessionService browserSessionService = new PlaywrightBrowserSessionServiceImpl(
            platformAuthSessionMapper, properties);

    /**
     * Boss 直聘平台适配器，直接构造以聚焦适配器行为
     */
    private final JobPlatformAdapter adapter = new BossJobPlatformAdapter(browserSessionService);

    /**
     * 在真实 Boss 直聘列表页上发现职位，验证至少返回一条有效职位条目。
     */
    @Test
    void discoverRealBossJobs_shouldReturnAtLeastOneEntry() {
        // 1. 检查当前授权状态
        BrowserSessionService.AuthCheckResult auth = adapter.checkAuth();
        assertThat(auth.status())
                .as("Boss 直聘未授权，请先完成授权流程")
                .isEqualTo(PlatformAuthStatus.AUTHORIZED.getValue());

        // 2. 构造真实的职位发现请求
        JobDiscoveryRequest request = new JobDiscoveryRequest(
                "boss",
                "Java",
                "北京",
                "1-3年",
                1,
                10
        );

        // 3. 调用发现接口
        List<JobListEntry> entries = adapter.discover(request);

        // 4. 验证至少发现一个职位
        assertThat(entries)
                .as("Boss 直聘列表页应至少返回一个职位条目")
                .isNotEmpty();

        // 5. 验证第一条目关键字段非空
        JobListEntry first = entries.get(0);
        assertThat(first.title()).as("职位标题不应为空").isNotBlank();
        assertThat(first.companyName()).as("公司名称不应为空").isNotBlank();
        assertThat(first.city()).as("工作城市不应为空").isNotBlank();
        assertThat(first.sourceUrl()).as("职位源链接不应为空").isNotBlank();
    }

    /**
     * 在真实 Boss 直聘详情页上抓取并兜底解析，验证页面与卡片关键字段非空。
     */
    @Test
    void fetchRealBossJobDetail_shouldExtractCard() {
        // 1. 检查当前授权状态
        BrowserSessionService.AuthCheckResult auth = adapter.checkAuth();
        assertThat(auth.status())
                .as("Boss 直聘未授权，请先完成授权流程")
                .isEqualTo(PlatformAuthStatus.AUTHORIZED.getValue());

        // 2. 使用真实的 Boss 直聘职位详情页 URL
        String realUrl = "https://www.zhipin.com/job_detail/1234567890123456789.html";

        // 3. 抓取详情页
        FetchedJobPage page = adapter.fetchDetail(realUrl);

        // 4. 验证页面关键内容非空
        assertThat(page).as("抓取页面不应为空").isNotNull();
        assertThat(page.title()).as("页面标题不应为空").isNotBlank();
        assertThat(page.content()).as("页面正文不应为空").isNotBlank();
        assertThat(page.sourceUrl()).as("页面源 URL 不应为空").isNotBlank();

        // 5. 兜底解析职位卡片
        ExtractedJobCard card = adapter.fallbackExtract(page);

        // 6. 验证卡片关键字段非空
        assertThat(card).as("解析卡片不应为空").isNotNull();
        assertThat(card.title()).as("职位标题不应为空").isNotBlank();
        assertThat(card.companyName()).as("公司名称不应为空").isNotBlank();
        assertThat(card.city()).as("工作城市不应为空").isNotBlank();
    }
}
