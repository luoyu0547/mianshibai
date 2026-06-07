package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.Job;

public interface JobParseService {

    Job parseJob(JobCrawlService.CrawlResult crawlResult);

    Company parseCompany(JobCrawlService.CrawlResult crawlResult);
}
