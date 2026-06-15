package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DefaultJobPlatformAdapterRegistry 单元测试
 */
class DefaultJobPlatformAdapterRegistryTest {

    /**
     * Boss 直聘平台适配器桩
     */
    static class BossAdapter implements JobPlatformAdapter {
        @Override
        public String platform() {
            return "BOSS";
        }

        @Override
        public boolean requiresAuth() {
            return true;
        }

        @Override
        public BrowserSessionService.AuthCheckResult checkAuth() {
            return new BrowserSessionService.AuthCheckResult("BOSS", "ok", "authenticated");
        }

        @Override
        public List<JobListEntry> discover(JobDiscoveryRequest request) {
            return Collections.emptyList();
        }

        @Override
        public FetchedJobPage fetchDetail(String url) {
            return null;
        }

        @Override
        public ExtractedJobCard fallbackExtract(FetchedJobPage page) {
            return null;
        }
    }

    /**
     * 当传入已注册平台时，应返回对应适配器（忽略大小写）
     */
    @Test
    void shouldReturnAdapterIgnoringCase() {
        // 1. 准备 Boss 适配器桩
        BossAdapter bossAdapter = new BossAdapter();
        // 2. 构建注册表
        DefaultJobPlatformAdapterRegistry registry = new DefaultJobPlatformAdapterRegistry(List.of(bossAdapter));
        // 3. 查询 BOSS 平台适配器
        JobPlatformAdapter result = registry.getAdapter("BOSS");
        // 4. 验证返回的是同一个适配器实例
        assertEquals(bossAdapter, result);
    }

    /**
     * 当传入未注册平台时，应抛出 IllegalArgumentException
     */
    @Test
    void shouldThrowWhenPlatformNotSupported() {
        // 1. 构建空注册表
        DefaultJobPlatformAdapterRegistry registry = new DefaultJobPlatformAdapterRegistry(Collections.emptyList());
        // 2. 验证查询未知平台时抛出异常
        assertThrows(IllegalArgumentException.class, () -> registry.getAdapter("unknown"));
    }
}
