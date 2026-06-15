package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DefaultJobSourceDiscoveryServiceImpl implements JobSourceDiscoveryService {

    private static final Pattern SEARCH_ENGINE_URL_PATTERN = Pattern.compile(
            "(?i)^https?://([^/]+\\.)?(bing|google|baidu|sogou|so|sm)\\.[^/]+/(search|s|baidu|web|results?)([/?#].*)?$");

    @Override
    public List<CandidateUrl> discover(JobCrawlTask task) {
        Set<CandidateUrl> results = new LinkedHashSet<>();
        if (StringUtils.hasText(task.getSourceUrl())) {
            String sourceUrl = task.getSourceUrl().trim();
            if (isSearchEngineResultPage(sourceUrl)) {
                log.warn("忽略搜索引擎结果页职位来源: {}", sourceUrl);
            } else {
                String sourceType = StringUtils.hasText(task.getSourceType()) ? task.getSourceType() : "direct";
                results.add(new CandidateUrl(sourceUrl, sourceType, detectPlatform(sourceUrl), "configured source"));
            }
        }
        log.debug("发现 {} 个候选 URL", results.size());
        return new ArrayList<>(results);
    }

    private boolean isSearchEngineResultPage(String url) {
        return SEARCH_ENGINE_URL_PATTERN.matcher(url).matches();
    }

    private String detectPlatform(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("zhipin") || lower.contains("boss")) return "boss";
        if (lower.contains("lagou")) return "lagou";
        if (lower.contains("liepin")) return "liepin";
        return "unknown";
    }
}
