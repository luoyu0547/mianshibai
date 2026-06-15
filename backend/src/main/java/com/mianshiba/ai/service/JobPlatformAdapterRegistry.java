package com.mianshiba.ai.service;

/**
 * 职位平台适配器注册表
 * 根据平台标识获取对应的适配器实现。
 */
public interface JobPlatformAdapterRegistry {

    /**
     * 根据平台标识获取适配器
     *
     * @param platform 平台标识
     * @return 对应的职位平台适配器
     * @throws IllegalArgumentException 当平台不支持时抛出
     */
    JobPlatformAdapter getAdapter(String platform);
}
