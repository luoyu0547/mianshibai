package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobPlatformAdapter;
import com.mianshiba.ai.service.JobPlatformAdapterRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认职位平台适配器注册表
 * 基于 Spring 注入的所有 {@link JobPlatformAdapter} 实现构建平台到适配器的映射。
 */
@Service
public class DefaultJobPlatformAdapterRegistry implements JobPlatformAdapterRegistry {

    /**
     * 平台标识（小写）到适配器的映射
     */
    private final Map<String, JobPlatformAdapter> adapterMap;

    /**
     * 构造注册表
     *
     * @param adapters 所有可用的平台适配器
     */
    public DefaultJobPlatformAdapterRegistry(List<JobPlatformAdapter> adapters) {
        // 1. 将适配器列表转换为以平台小写名为键的映射
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(
                        adapter -> adapter.platform().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));
    }

    /**
     * 根据平台标识获取适配器（忽略大小写）
     *
     * @param platform 平台标识
     * @return 对应的适配器
     * @throws IllegalArgumentException 当平台不支持时抛出
     */
    @Override
    public JobPlatformAdapter getAdapter(String platform) {
        // 1. 将平台标识转为小写后查找
        JobPlatformAdapter adapter = adapterMap.get(platform.toLowerCase(Locale.ROOT));
        // 2. 未找到时抛出异常
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported job platform: " + platform);
        }
        return adapter;
    }
}
