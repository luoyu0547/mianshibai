package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobSourcingExtractService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJobSourcingQualityServiceImplTest {

    @Test
    void score_shouldRewardCompleteProgrammerJobAndWarnMissingSalary() {
        DefaultJobSourcingQualityServiceImpl service = new DefaultJobSourcingQualityServiceImpl();
        JobSourcingExtractService.ExtractedJobCard card = new JobSourcingExtractService.ExtractedJobCard(
                "Java后端开发工程师", "杭州示例科技有限公司", "杭州", "", "3-5年", "本科",
                "负责 Spring Boot 后端服务开发", "熟悉 Java、MySQL、Redis", "[\"Java\",\"Spring Boot\",\"MySQL\"]",
                "Java 后端岗位，技术栈清晰。", "[\"Java后端\",\"3-5年\"]", 85);

        var result = service.score(card);

        assertThat(result.qualityScore()).isGreaterThanOrEqualTo(70);
        assertThat(result.warningsJson()).contains("薪资缺失");
    }
}
