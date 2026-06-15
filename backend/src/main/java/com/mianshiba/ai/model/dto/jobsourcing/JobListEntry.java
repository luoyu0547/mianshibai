package com.mianshiba.ai.model.dto.jobsourcing;

/**
 * 职位列表条目
 * 职位发现阶段返回的精简职位信息。
 */
public record JobListEntry(
        /**
         * 来源平台标识
         */
        String platform,
        /**
         * 职位详情页源地址
         */
        String sourceUrl,
        /**
         * 职位标题
         */
        String title,
        /**
         * 公司名称
         */
        String companyName,
        /**
         * 工作城市
         */
        String city,
        /**
         * 薪资范围
         */
        String salaryRange
) {
}
