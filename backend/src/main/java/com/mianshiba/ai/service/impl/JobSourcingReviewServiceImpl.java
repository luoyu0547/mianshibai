package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlItemReviewRequest;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.service.JobSourcingReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JobSourcingReviewServiceImpl implements JobSourcingReviewService {

    private final JobCrawlItemMapper itemMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long itemId, AdminJobCrawlItemReviewRequest request) {
        JobCrawlItem item = getExistingItem(itemId);

        String jsonStr = request.getEditedExtractedJson() != null
                ? request.getEditedExtractedJson()
                : item.getExtractedJson() != null ? item.getExtractedJson().toString() : "{}";
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.JOB_PARSE_ERROR, "解析采集数据失败");
        }

        Job job = new Job();
        job.setTitle(getText(root, "title"));
        job.setCompanyName(getText(root, "companyName"));
        job.setCity(getText(root, "city"));
        job.setSalaryRange(getText(root, "salaryRange"));
        job.setExperienceRequirement(getText(root, "experienceRequirement"));
        job.setEducationRequirement(getText(root, "educationRequirement"));
        job.setJobDescription(getText(root, "jobDescription"));
        job.setJobRequirement(getText(root, "jobRequirement"));
        if (root.has("techStack")) {
            job.setTechStack(root.get("techStack").toString());
        }
        job.setSourcePlatform(item.getSourcePlatform());
        job.setSourceUrl(item.getSourceUrl());
        job.setRawContent(item.getRawContent());
        job.setStatus("active");
        job.setApplicationStatus("favorite");

        jobMapper.insert(job);

        String companyName = job.getCompanyName();
        if (StringUtils.hasText(companyName)) {
            Company existing = companyMapper.selectOne(
                    Wrappers.lambdaQuery(Company.class)
                            .eq(Company::getName, companyName)
                            .last("LIMIT 1"));
            if (existing != null) {
                job.setCompanyId(existing.getId());
                jobMapper.updateById(job);
            } else {
                Company company = new Company();
                company.setName(companyName);
                company.setSourceUrl(item.getSourceUrl());
                companyMapper.insert(company);
                job.setCompanyId(company.getId());
                jobMapper.updateById(job);
            }
        }

        item.setJobId(job.getId());
        item.setReviewStatus("approved");
        if (StringUtils.hasText(request.getReviewNote())) {
            item.setReviewNote(request.getReviewNote());
        }
        itemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long itemId, AdminJobCrawlItemReviewRequest request) {
        JobCrawlItem item = getExistingItem(itemId);
        item.setReviewStatus("rejected");
        if (StringUtils.hasText(request.getReviewNote())) {
            item.setReviewNote(request.getReviewNote());
        }
        itemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDuplicate(Long itemId, AdminJobCrawlItemReviewRequest request) {
        JobCrawlItem item = getExistingItem(itemId);
        if (request.getDuplicateOfJobId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复标记必须指定关联职位ID");
        }
        item.setReviewStatus("duplicate");
        item.setDuplicateOfJobId(request.getDuplicateOfJobId());
        if (StringUtils.hasText(request.getReviewNote())) {
            item.setReviewNote(request.getReviewNote());
        }
        itemMapper.updateById(item);
    }

    private JobCrawlItem getExistingItem(Long itemId) {
        JobCrawlItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "采集项不存在");
        }
        return item;
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
