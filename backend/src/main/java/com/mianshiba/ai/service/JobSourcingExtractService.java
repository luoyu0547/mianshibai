package com.mianshiba.ai.service;

public interface JobSourcingExtractService {
    ExtractedJobCard extract(JobPageFetchService.FetchedPage page);

    record ExtractedJobCard(String title, String companyName, String city, String salaryRange,
                            String experienceRequirement, String educationRequirement,
                            String jobDescription, String jobRequirement, String techStackJson,
                            String summary, String tagsJson, int confidenceScore) {
    }
}
