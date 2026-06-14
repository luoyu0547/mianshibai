package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.Job;

public interface JobDedupService {

    String normalizeUrl(String url);

    String buildFingerprint(Job job);

    Job findDuplicate(String normalizedUrl, Job parsedJob);
}
