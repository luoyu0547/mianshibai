package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobPageFetchService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiJobSourcingExtractServiceImplTest {

    @Test
    void extract_shouldFallbackToRulesWhenAiClientUnavailable() {
        AiJobSourcingExtractServiceImpl service = new AiJobSourcingExtractServiceImpl(null);
        JobPageFetchService.FetchedPage page = new JobPageFetchService.FetchedPage(
                "https://boards.greenhouse.io/clickhouse/jobs/6009092004",
                "https://boards.greenhouse.io/clickhouse/jobs/6009092004",
                "Job Application for Senior Software Engineer (Backend) - AI/ML at ClickHouse",
                "Senior Software Engineer (Backend) - AI/ML United States (remote) About ClickHouse "
                        + "What you will do Feature Development Design and implement AI-powered features API Architecture "
                        + "What you will bring along 5+ years of software engineering experience Backend development experience in TypeScript or Python "
                        + "The typical starting salary for this role in the US is $141,000 - $195,000 USD",
                "<html></html>",
                "greenhouse",
                false);

        var card = service.extract(page);

        assertThat(card.title()).isEqualTo("Senior Software Engineer (Backend) - AI/ML");
        assertThat(card.companyName()).isEqualTo("ClickHouse");
        assertThat(card.city()).isEqualTo("United States (remote)");
        assertThat(card.salaryRange()).isEqualTo("$141,000 - $195,000 USD");
        assertThat(card.experienceRequirement()).contains("5+");
        assertThat(card.jobDescription()).contains("Feature Development");
        assertThat(card.jobRequirement()).contains("Backend development experience");
        assertThat(card.techStackJson()).contains("TypeScript", "Python");
        assertThat(card.confidenceScore()).isGreaterThanOrEqualTo(60);
    }
}
