package com.mianshiba.ai.model.dto.jobsourcing;

/**
 * 提取后的职位卡片
 * 从页面内容中解析出的结构化职位信息。
 */
public record ExtractedJobCard(
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
        String salaryRange,
        /**
         * 经验要求
         */
        String experienceRequirement,
        /**
         * 学历要求
         */
        String educationRequirement,
        /**
         * 职位描述
         */
        String jobDescription,
        /**
         * 岗位要求
         */
        String jobRequirement,
        /**
         * 技术栈 JSON
         */
        String techStackJson,
        /**
         * 职位摘要
         */
        String summary,
        /**
         * 标签 JSON
         */
        String tagsJson,
        /**
         * 提取置信度（0-100）
         */
        int confidenceScore
) {
}
