package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import com.mianshiba.ai.service.JobPlatformAdapterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSourcingReActAgentServiceImplTest {

    @Mock
    private JobCrawlTaskMapper taskMapper;
    @Mock
    private JobCrawlRunMapper runMapper;
    @Mock
    private JobCrawlItemMapper itemMapper;
    @Mock
    private JobPlatformAdapterRegistry adapterRegistry;
    @Mock
    private JobSourcingProperties properties;
    private JobSourcingReActAgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        agentService = new JobSourcingReActAgentServiceImpl(
                taskMapper, runMapper, itemMapper, adapterRegistry, properties, new ObjectMapper());
    }

    @Test
    void runTask_shouldReturnAuthRequiredAndNotInsertItemWhenAuthMissing() {
        // 1. 构造 Boss 平台采集任务
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setSourceType("boss");
        when(taskMapper.selectById(1L)).thenReturn(task);

        // 2. 模拟 Boss 适配器需要授权且未授权
        JobPlatformAdapter adapter = org.mockito.Mockito.mock(JobPlatformAdapter.class);
        when(adapterRegistry.getAdapter("boss")).thenReturn(adapter);
        when(adapter.requiresAuth()).thenReturn(true);
        when(adapter.checkAuth()).thenReturn(
                new BrowserSessionService.AuthCheckResult("boss", "auth_required", "未授权"));

        // 3. 执行任务
        JobCrawlRun run = agentService.runTask(1L);

        // 4. 验证状态为 auth_required 且没有产生任何 item
        assertThat(run.getStatus()).isEqualTo("auth_required");
        verify(itemMapper, never()).insert(any(JobCrawlItem.class));
    }

    @Test
    void runTask_shouldSaveExtractedItemWhenAuthorized() {
        // 1. 构造实习僧平台采集任务
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setSourceType("shixiseng");
        when(taskMapper.selectById(1L)).thenReturn(task);

        // 2. 模拟已授权的实习僧适配器
        JobPlatformAdapter adapter = org.mockito.Mockito.mock(JobPlatformAdapter.class);
        when(adapterRegistry.getAdapter("shixiseng")).thenReturn(adapter);
        when(adapter.requiresAuth()).thenReturn(false);

        // 3. 模拟发现一条职位
        JobListEntry entry = new JobListEntry(
                "shixiseng", "https://www.shixiseng.com/job/abc", "Java 实习", "Test Company", "北京", "200-300/天");
        when(adapter.discover(any(JobDiscoveryRequest.class))).thenReturn(List.of(entry));

        // 4. 模拟详情抓取与兜底解析
        FetchedJobPage page = new FetchedJobPage(
                entry.sourceUrl(), entry.sourceUrl(), "Java 实习", "content", "<html></html>", "shixiseng", false);
        ExtractedJobCard card = new ExtractedJobCard(
                "Java 实习", "Test Company", "北京", "200-300/天",
                null, "本科", "desc", null, "[\"Java\"]", null, "[]", 80);
        when(adapter.fetchDetail(entry.sourceUrl())).thenReturn(page);
        when(adapter.fallbackExtract(page)).thenReturn(card);

        // 5. 设置运行记录 ID
        when(runMapper.insert(any(JobCrawlRun.class))).thenAnswer(inv -> {
            JobCrawlRun run = inv.getArgument(0);
            run.setId(100L);
            return 1;
        });

        // 6. 执行任务
        JobCrawlRun run = agentService.runTask(1L);

        // 7. 验证任务成功且 item 被正确保存
        assertThat(run.getStatus()).isEqualTo("success");
        verify(itemMapper).insert(any(JobCrawlItem.class));
        verify(itemMapper).updateById(argThat((JobCrawlItem item) -> "extracted".equals(item.getStatus())));
    }

    @Test
    void runTask_shouldThrowWhenTaskNotFound() {
        when(taskMapper.selectById(anyLong())).thenReturn(null);

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                com.mianshiba.ai.exception.BusinessException.class,
                () -> agentService.runTask(1L))
        ).isNotNull();
    }
}
