package com.mianshiba.ai.service.tool;

import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import com.mianshiba.ai.service.JobPlatformAdapterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JobSourcingTools 单元测试
 */
@ExtendWith(MockitoExtension.class)
class JobSourcingToolsTest {

    @Mock
    private JobPlatformAdapterRegistry adapterRegistry;

    @Mock
    private JobPlatformAdapter adapter;

    @InjectMocks
    private JobSourcingTools jobSourcingTools;

    @Test
    void checkPlatformAuthReturnsAdapterResult() {
        // 1. 准备 adapter 返回的授权检查结果
        BrowserSessionService.AuthCheckResult expected =
                new BrowserSessionService.AuthCheckResult("boss", "valid", "session ok");
        when(adapterRegistry.getAdapter("boss")).thenReturn(adapter);
        when(adapter.checkAuth()).thenReturn(expected);

        // 2. 调用工具方法
        BrowserSessionService.AuthCheckResult result = jobSourcingTools.checkPlatformAuth("boss");

        // 3. 验证返回结果并确认委托给 registry 与 adapter
        assertThat(result).isEqualTo(expected);
        verify(adapterRegistry).getAdapter("boss");
        verify(adapter).checkAuth();
    }

    @Test
    void discoverJobsReturnsAdapterList() {
        // 1. 准备 adapter 返回的职位列表
        List<JobListEntry> expected = List.of(
                new JobListEntry(
                        "boss",
                        "https://www.zhipin.com/job_detail/1.html",
                        "Java 开发",
                        "某科技公司",
                        "深圳",
                        "20-40K"
                )
        );
        when(adapterRegistry.getAdapter("boss")).thenReturn(adapter);
        when(adapter.discover(any(JobDiscoveryRequest.class))).thenReturn(expected);

        // 2. 调用工具方法
        List<JobListEntry> result = jobSourcingTools.discoverJobs("boss", "Java", "深圳", "3-5年", 2, 5);

        // 3. 验证返回结果与请求参数
        assertThat(result).isEqualTo(expected);
        verify(adapterRegistry).getAdapter("boss");
        verify(adapter).discover(argThat(req ->
                req.platform().equals("boss")
                        && req.keywords().equals("Java")
                        && req.cities().equals("深圳")
                        && req.experienceLevels().equals("3-5年")
                        && req.maxPages() == 2
                        && req.targetCount() == 5
        ));
    }

    @Test
    void fetchJobDetailReturnsAdapterPage() {
        // 1. 准备 adapter 返回的详情页
        String url = "https://www.zhipin.com/job_detail/1.html";
        FetchedJobPage expected = new FetchedJobPage(
                url,
                url,
                "Java 开发",
                "岗位要求",
                "<html></html>",
                "boss",
                true
        );
        when(adapterRegistry.getAdapter("boss")).thenReturn(adapter);
        when(adapter.fetchDetail(url)).thenReturn(expected);

        // 2. 调用工具方法
        FetchedJobPage result = jobSourcingTools.fetchJobDetail("boss", url);

        // 3. 验证返回结果
        assertThat(result).isEqualTo(expected);
        verify(adapterRegistry).getAdapter("boss");
        verify(adapter).fetchDetail(url);
    }
}
