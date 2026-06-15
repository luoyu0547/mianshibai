package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.JobCrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DefaultJobCrawlServiceImpl implements JobCrawlService {

    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

    private final RestClient restClient;

    public DefaultJobCrawlServiceImpl() {
        this(RestClient.create());
    }

    public DefaultJobCrawlServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CrawlResult crawl(String url) {
        String html;
        String finalUrl = url;
        try {
            html = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("抓取页面失败: {}", url, e);
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
        }

        if (html == null) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
        }

        String title = extractTitle(html);
        String text = extractText(html);
        String sourcePlatform = detectPlatform(url);

        return new CrawlResult(url, finalUrl, title, text, html, sourcePlatform);
    }

    private String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String extractText(String html) {
        String text = html
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() > 12000) {
            text = text.substring(0, 12000);
        }
        return text;
    }

    private String detectPlatform(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("zhipin") || lower.contains("boss")) {
            return "boss";
        }
        if (lower.contains("lagou")) {
            return "lagou";
        }
        if (lower.contains("liepin")) {
            return "liepin";
        }
        return "unknown";
    }
}
