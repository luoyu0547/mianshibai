package com.mianshiba.ai.service.impl;

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

/**
 * 实习僧职位采集实时测试
 * 在设置 RUN_LIVE_SHIXISENG_CRAWL=true 时运行，验证实习僧真实页面采集能力。
 * 当前 discover/fetchDetail 尚未实现真实抓取，测试会自然失败，用于明确后续实现目标。
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_SHIXISENG_CRAWL", matches = "true")
class ShixisengJobSourcingLiveTest {

    /**
     * 实习僧平台适配器，公开访问无需额外授权
     */
    private final JobPlatformAdapter adapter = new ShixisengJobPlatformAdapter();

    /**
     * 在真实实习僧列表页上发现实习岗位，验证至少返回一条有效实习条目。
     */
    @Test
    void discoverRealShixisengInternships_shouldReturnAtLeastOneEntry() {
        // 1. 检查当前授权状态（实习僧为公开访问，应返回已授权）
        BrowserSessionService.AuthCheckResult auth = adapter.checkAuth();
        assertThat(auth.status())
                .as("实习僧为公开平台，应返回已授权状态")
                .isEqualTo(PlatformAuthStatus.AUTHORIZED.getValue());

        // 2. 构造真实的实习发现请求
        JobDiscoveryRequest request = new JobDiscoveryRequest(
                "shixiseng",
                "Java",
                "北京",
                null,
                1,
                10
        );

        // 3. 调用发现接口
        List<JobListEntry> entries = adapter.discover(request);

        // 4. 验证至少发现一个实习岗位
        assertThat(entries)
                .as("实习僧列表页应至少返回一个实习岗位条目")
                .isNotEmpty();

        // 5. 验证第一条目关键字段非空
        JobListEntry first = entries.get(0);
        assertThat(first.title()).as("实习标题不应为空").isNotBlank();
        assertThat(first.companyName()).as("公司名称不应为空").isNotBlank();
        assertThat(first.city()).as("工作城市不应为空").isNotBlank();
        assertThat(first.sourceUrl()).as("实习源链接不应为空").isNotBlank();
    }

    /**
     * 在真实实习僧详情页上抓取并兜底解析，验证页面与卡片关键字段非空。
     */
    @Test
    void fetchRealShixisengInternshipDetail_shouldExtractCard() {
        // 1. 检查当前授权状态（实习僧为公开访问，应返回已授权）
        BrowserSessionService.AuthCheckResult auth = adapter.checkAuth();
        assertThat(auth.status())
                .as("实习僧为公开平台，应返回已授权状态")
                .isEqualTo(PlatformAuthStatus.AUTHORIZED.getValue());

        // 2. 使用真实的实习僧详情页 URL
        String realUrl = "https://www.shixiseng.com/intern/abc123";

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
        assertThat(card.title()).as("实习标题不应为空").isNotBlank();
        assertThat(card.companyName()).as("公司名称不应为空").isNotBlank();
        assertThat(card.city()).as("工作城市不应为空").isNotBlank();
    }
}
