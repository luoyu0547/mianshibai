package com.mianshiba.ai.model.dto.jobsourcing;

/**
 * 职位发现请求
 * 用于向平台适配器发起职位列表检索的参数。
 */
public record JobDiscoveryRequest(
        /**
         * 目标平台标识
         */
        String platform,
        /**
         * 搜索关键词
         */
        String keywords,
        /**
         * 目标城市，多个以分隔符拼接
         */
        String cities,
        /**
         * 经验等级，多个以分隔符拼接
         */
        String experienceLevels,
        /**
         * 最大翻页数
         */
        int maxPages,
        /**
         * 期望获取的职位数量
         */
        int targetCount
) {
}
