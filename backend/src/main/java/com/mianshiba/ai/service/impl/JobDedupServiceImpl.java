package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.service.JobDedupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobDedupServiceImpl implements JobDedupService {

    private static final Set<String> TRACKING_PARAMS = Set.of("utm_source", "utm_medium", "utm_campaign",
            "utm_term", "utm_content", "ref", "spm", "track");

    private final JobMapper jobMapper;

    @Override
    public String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim().toLowerCase();
        try {
            URI uri = new URI(trimmed);
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return buildNormalizedUrl(uri, null);
            }
            String filteredQuery = filterTrackingParams(query);
            return buildNormalizedUrl(uri, filteredQuery);
        } catch (URISyntaxException e) {
            int queryIdx = trimmed.indexOf('?');
            if (queryIdx < 0) return trimmed;
            String base = trimmed.substring(0, queryIdx);
            String query = trimmed.substring(queryIdx + 1);
            String filtered = filterTrackingParams(query);
            return filtered.isEmpty() ? base : base + "?" + filtered;
        }
    }

    private String filterTrackingParams(String query) {
        if (query == null || query.isEmpty()) return "";
        String[] params = query.split("&");
        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            String key = param.split("=", 2)[0].toLowerCase().trim();
            if (TRACKING_PARAMS.contains(key)) continue;
            if (!sb.isEmpty()) sb.append("&");
            sb.append(param);
        }
        return sb.toString();
    }

    private String buildNormalizedUrl(URI uri, String query) {
        StringBuilder sb = new StringBuilder();
        String scheme = uri.getScheme() != null ? uri.getScheme() : "";
        sb.append(scheme).append("://");
        String host = uri.getHost() != null ? uri.getHost() : "";
        sb.append(host);
        int port = uri.getPort();
        if (port > 0 && port != 80 && port != 443) {
            sb.append(":").append(port);
        }
        String path = uri.getPath() != null ? uri.getPath() : "";
        sb.append(path);
        if (query != null && !query.isEmpty()) {
            sb.append("?").append(query);
        }
        return sb.toString();
    }

    @Override
    public String buildFingerprint(Job job) {
        String company = job.getCompanyName() != null ? job.getCompanyName().trim().toLowerCase() : "";
        String title = job.getTitle() != null ? job.getTitle().trim().toLowerCase() : "";
        String city = job.getCity() != null ? job.getCity().trim().toLowerCase() : "";
        return company + "|" + title + "|" + city;
    }

    @Override
    public Job findDuplicate(String normalizedUrl, Job parsedJob) {
        String fingerprint = buildFingerprint(parsedJob);

        Job byFingerprint = jobMapper.selectOne(
                Wrappers.lambdaQuery(Job.class)
                        .eq(Job::getCompanyName, parsedJob.getCompanyName() != null ? parsedJob.getCompanyName().trim() : null)
                        .eq(Job::getTitle, parsedJob.getTitle() != null ? parsedJob.getTitle().trim() : null)
                        .eq(Job::getCity, parsedJob.getCity() != null ? parsedJob.getCity().trim() : null)
                        .last("LIMIT 1"));
        if (byFingerprint != null) return byFingerprint;

        if (normalizedUrl != null && !normalizedUrl.isEmpty()) {
            return jobMapper.selectOne(
                    Wrappers.lambdaQuery(Job.class)
                            .eq(Job::getSourceUrl, normalizedUrl)
                            .last("LIMIT 1"));
        }
        return null;
    }
}
