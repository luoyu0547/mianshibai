package com.mianshiba.ai.service.tool;

import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import com.mianshiba.ai.service.JobPlatformAdapterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

/**
 * 职位采集 Spring AI 工具
 * 为 AI Agent 提供统一的职位平台操作入口，包括授权检查、职位发现与详情抓取。
 */
@RequiredArgsConstructor
public class JobSourcingTools {

    private final JobPlatformAdapterRegistry adapterRegistry;

    /**
     * 检查目标平台是否拥有有效的授权浏览器会话。
     *
     * @param platform 平台标识
     * @return 授权检查结果
     */
    @Tool(description = "Check whether a job platform has a valid authorized browser session. Use before crawling platforms that require login.")
    public BrowserSessionService.AuthCheckResult checkPlatformAuth(String platform) {
        // 1. 根据平台标识获取对应适配器
        JobPlatformAdapter adapter = adapterRegistry.getAdapter(platform);
        // 2. 委托适配器检查当前授权状态
        return adapter.checkAuth();
    }

    /**
     * 根据关键词、城市与经验条件从指定平台发现职位列表。
     *
     * @param platform         平台标识
     * @param keywords         搜索关键词
     * @param cities           目标城市
     * @param experienceLevels 经验等级
     * @param maxPages         最大翻页数
     * @param targetCount      期望获取的职位数量
     * @return 职位列表条目
     */
    @Tool(description = "Discover job detail URLs from a supported platform using keywords, cities, and experience filters.")
    public List<JobListEntry> discoverJobs(String platform, String keywords, String cities, String experienceLevels, int maxPages, int targetCount) {
        // 1. 根据平台标识获取对应适配器
        JobPlatformAdapter adapter = adapterRegistry.getAdapter(platform);
        // 2. 构造职位发现请求参数
        JobDiscoveryRequest request = new JobDiscoveryRequest(platform, keywords, cities, experienceLevels, maxPages, targetCount);
        // 3. 委托适配器发现职位列表
        return adapter.discover(request);
    }

    /**
     * 从指定平台抓取单个职位详情页。
     *
     * @param platform 平台标识
     * @param url      职位详情页 URL
     * @return 抓取的职位详情页
     */
    @Tool(description = "Fetch one job detail page from a supported platform. The URL must come from discoverJobs.")
    public FetchedJobPage fetchJobDetail(String platform, String url) {
        // 1. 根据平台标识获取对应适配器
        JobPlatformAdapter adapter = adapterRegistry.getAdapter(platform);
        // 2. 委托适配器抓取职位详情页
        return adapter.fetchDetail(url);
    }
}
