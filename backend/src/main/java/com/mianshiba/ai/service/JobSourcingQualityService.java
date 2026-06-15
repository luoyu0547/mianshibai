package com.mianshiba.ai.service;

public interface JobSourcingQualityService {
    QualityResult score(JobSourcingExtractService.ExtractedJobCard card);

    record QualityResult(int qualityScore, String warningsJson) {
    }
}
