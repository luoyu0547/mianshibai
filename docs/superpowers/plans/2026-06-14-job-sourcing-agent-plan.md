# Job Sourcing Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a hybrid AI job sourcing agent that discovers programmer jobs, extracts reviewable job cards, lets admins approve them, and exposes only approved jobs to users.

**Architecture:** Extend the existing admin job crawl module instead of replacing it. `JobSourcingAgentService` orchestrates discovery, fetch, extraction, quality scoring, deduplication, and pending-review item creation; `JobSourcingReviewService` publishes approved items into the existing `job`, `company`, and `job_analysis` flow.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis-Plus, JUnit 5, Mockito, Spring AI `ChatClient`, Vue 3, TypeScript, Pinia, Element Plus, Vite.

**Repository Rule:** Do not create git commits during execution unless the user explicitly asks for commits. Checkpoint steps use `git diff` to inspect intended files and must not stage changes.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql` adds first-version review and sourcing columns to `job_crawl_item` and `job_crawl_run`.
- Modify: `backend/src/main/resources/application.yml` adds provider configuration placeholders for `app.job-sourcing`.
- Create: `backend/src/main/java/com/mianshiba/ai/config/JobSourcingProperties.java` binds source provider and budget defaults.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlItem.java` adds review card fields.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlRun.java` adds summary and cost fields.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlItemReviewRequest.java` carries approve/reject/duplicate payloads.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlReviewQueryRequest.java` filters the review pool.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlItemVO.java` exposes review card fields.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlRunVO.java` exposes run summary/cost fields.
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingAgentService.java` orchestration interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourceDiscoveryService.java` candidate URL discovery interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobPageFetchService.java` local/third-party fetch interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingExtractService.java` AI extraction interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingQualityService.java` scoring and warnings interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingReviewService.java` approval/rejection publishing interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobSourceDiscoveryServiceImpl.java` deterministic search plan and URL discovery.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobPageFetchServiceImpl.java` local fetch with optional provider fallback.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AiJobSourcingExtractServiceImpl.java` strict JSON extraction.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobSourcingQualityServiceImpl.java` quality and confidence scoring.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingAgentServiceImpl.java` run orchestration.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingReviewServiceImpl.java` publish/reject/duplicate flow.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImpl.java` delegates sourcing tasks to the new agent.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java` adds review pool and review action methods.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java` maps new VOs and delegates review actions.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java` adds review pool, approve, reject, and duplicate endpoints.
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingAgentServiceImplTest.java` covers agent orchestration.
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingReviewServiceImplTest.java` covers approve/reject/duplicate transitions.
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingAgentTest.java` covers schema fields.

### Frontend Files

- Modify: `frontend/src/types/jobCrawl.ts` adds review statuses, review card fields, and request types.
- Modify: `frontend/src/api/jobCrawl.ts` adds review pool and action APIs.
- Modify: `frontend/src/stores/jobCrawl.ts` adds review pool state/actions.
- Create: `frontend/src/views/admin/AdminJobCrawlReviewPage.vue` renders review cards.
- Modify: `frontend/src/router/index.ts` adds `/admin/job-crawl/review` route.
- Modify: `frontend/src/views/admin/AdminJobCrawlDetailPage.vue` links each run to review items.
- Test: `frontend/src/views/admin/__tests__/AdminJobCrawlReviewPage.spec.ts` covers card rendering and actions.

---

### Task 1: Schema And Configuration

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/mianshiba/ai/config/JobSourcingProperties.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlItem.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlRun.java`
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingAgentTest.java`

- [ ] **Step 1: Write the failing SQL coverage test**

Create `backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingAgentTest.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlJobSourcingAgentTest {

    @Test
    void initSql_shouldContainJobSourcingAgentReviewFields() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("review_status VARCHAR(32) NOT NULL DEFAULT 'pending_review'");
        assertThat(sql).contains("quality_score INT NOT NULL DEFAULT 0");
        assertThat(sql).contains("confidence_score INT NOT NULL DEFAULT 0");
        assertThat(sql).contains("duplicate_of_job_id BIGINT DEFAULT NULL");
        assertThat(sql).contains("extracted_json JSON DEFAULT NULL");
        assertThat(sql).contains("tags_json JSON DEFAULT NULL");
        assertThat(sql).contains("cost_json JSON DEFAULT NULL");
        assertThat(sql).contains("summary TEXT DEFAULT NULL");
    }
}
```

- [ ] **Step 2: Run the failing SQL test**

Run: `.\mvnw.cmd test -pl . -Dtest=InitSqlJobSourcingAgentTest`

Expected: FAIL because the new SQL fields are missing.

- [ ] **Step 3: Add SQL fields**

In `backend/src/main/resources/sql/init.sql`, update `job_crawl_run` to include:

```sql
  summary TEXT DEFAULT NULL COMMENT '本次采集总结',
  cost_json JSON DEFAULT NULL COMMENT '调用成本统计',
```

Update `job_crawl_item` to include:

```sql
  source_platform VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源平台或网站类型',
  raw_content MEDIUMTEXT DEFAULT NULL COMMENT '抓取内容快照',
  extracted_json JSON DEFAULT NULL COMMENT 'AI 抽取后的待审核职位卡片',
  summary TEXT DEFAULT NULL COMMENT 'AI 摘要',
  tags_json JSON DEFAULT NULL COMMENT '岗位标签',
  quality_score INT NOT NULL DEFAULT 0 COMMENT '质量分 0-100',
  confidence_score INT NOT NULL DEFAULT 0 COMMENT '抽取置信度 0-100',
  duplicate_of_job_id BIGINT DEFAULT NULL COMMENT '疑似重复的正式职位 id',
  review_status VARCHAR(32) NOT NULL DEFAULT 'pending_review' COMMENT '审核状态：pending_review/approved/rejected/duplicate/failed',
  review_note VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
```

- [ ] **Step 4: Add entity fields**

Update `JobCrawlItem` with:

```java
private String sourcePlatform;
private String rawContent;
private Object extractedJson;
private String summary;
private Object tagsJson;
private Integer qualityScore;
private Integer confidenceScore;
private Long duplicateOfJobId;
private String reviewStatus;
private String reviewNote;
```

Update `JobCrawlRun` with:

```java
private String summary;
private Object costJson;
```

- [ ] **Step 5: Add configuration properties**

Create `backend/src/main/java/com/mianshiba/ai/config/JobSourcingProperties.java`:

```java
package com.mianshiba.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.job-sourcing")
public class JobSourcingProperties {
    private String provider = "local";
    private int targetCount = 20;
    private int maxSearchQueries = 5;
    private int maxPagesPerSource = 5;
    private int maxThirdPartyCalls = 20;
    private String firecrawlApiKey = "";
}
```

Add to `backend/src/main/resources/application.yml`:

```yaml
app:
  job-sourcing:
    provider: ${JOB_SOURCING_PROVIDER:local}
    target-count: ${JOB_SOURCING_TARGET_COUNT:20}
    max-search-queries: ${JOB_SOURCING_MAX_SEARCH_QUERIES:5}
    max-pages-per-source: ${JOB_SOURCING_MAX_PAGES_PER_SOURCE:5}
    max-third-party-calls: ${JOB_SOURCING_MAX_THIRD_PARTY_CALLS:20}
    firecrawl-api-key: ${FIRECRAWL_API_KEY:}
```

- [ ] **Step 6: Verify task passes**

Run: `.\mvnw.cmd test -pl . -Dtest=InitSqlJobSourcingAgentTest`

Expected: PASS.

- [ ] **Step 7: Checkpoint**

Run:

```powershell
git diff -- backend/src/main/resources/sql/init.sql backend/src/main/resources/application.yml backend/src/main/java/com/mianshiba/ai/config/JobSourcingProperties.java backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlItem.java backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlRun.java backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingAgentTest.java
```

---

### Task 2: Agent Contracts And Deterministic Discovery

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingAgentService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourceDiscoveryService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobPageFetchService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingExtractService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingQualityService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingReviewService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobSourceDiscoveryServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/DefaultJobSourceDiscoveryServiceImplTest.java`

- [ ] **Step 1: Write the failing discovery test**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/DefaultJobSourceDiscoveryServiceImplTest.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJobSourceDiscoveryServiceImplTest {

    @Test
    void discover_shouldGenerateProgrammerQueriesAndCandidateUrls() {
        JobSourcingProperties properties = new JobSourcingProperties();
        properties.setMaxSearchQueries(3);
        DefaultJobSourceDiscoveryServiceImpl service = new DefaultJobSourceDiscoveryServiceImpl(properties);

        JobCrawlTask task = new JobCrawlTask();
        task.setKeywords("Java后端,Spring Boot");
        task.setCities("杭州,上海");
        task.setExperienceLevels("3-5年");
        task.setSourceUrl("https://example.com/careers");

        List<JobSourceDiscoveryService.CandidateUrl> urls = service.discover(task);

        assertThat(urls).isNotEmpty();
        assertThat(urls).anyMatch(url -> url.url().equals("https://example.com/careers"));
        assertThat(urls).anyMatch(url -> url.discoveryQuery().contains("Java后端"));
        assertThat(urls).allMatch(url -> url.sourceType() != null && !url.sourceType().isBlank());
    }
}
```

- [ ] **Step 2: Run the failing discovery test**

Run: `.\mvnw.cmd test -pl . -Dtest=DefaultJobSourceDiscoveryServiceImplTest`

Expected: FAIL because contracts and implementation do not exist.

- [ ] **Step 3: Create service contracts**

Create `JobSourceDiscoveryService`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobCrawlTask;

import java.util.List;

public interface JobSourceDiscoveryService {
    List<CandidateUrl> discover(JobCrawlTask task);

    record CandidateUrl(String url, String sourceType, String sourcePlatform, String discoveryQuery) {
    }
}
```

Create `JobPageFetchService`:

```java
package com.mianshiba.ai.service;

public interface JobPageFetchService {
    FetchedPage fetch(String url);

    record FetchedPage(String sourceUrl, String finalUrl, String title, String content, String html,
                       String sourcePlatform, boolean thirdPartyUsed) {
    }
}
```

Create `JobSourcingExtractService`:

```java
package com.mianshiba.ai.service;

public interface JobSourcingExtractService {
    ExtractedJobCard extract(JobPageFetchService.FetchedPage page);

    record ExtractedJobCard(String title, String companyName, String city, String salaryRange,
                            String experienceRequirement, String educationRequirement,
                            String jobDescription, String jobRequirement, String techStackJson,
                            String summary, String tagsJson, int confidenceScore) {
    }
}
```

Create `JobSourcingQualityService`:

```java
package com.mianshiba.ai.service;

public interface JobSourcingQualityService {
    QualityResult score(JobSourcingExtractService.ExtractedJobCard card);

    record QualityResult(int qualityScore, String warningsJson) {
    }
}
```

Create `JobSourcingAgentService`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobCrawlRun;

public interface JobSourcingAgentService {
    JobCrawlRun runTask(Long taskId);
}
```

Create `JobSourcingReviewService`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlItemReviewRequest;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlItemVO;

public interface JobSourcingReviewService {
    AdminJobCrawlItemVO approve(Long itemId, AdminJobCrawlItemReviewRequest request);
    AdminJobCrawlItemVO reject(Long itemId, AdminJobCrawlItemReviewRequest request);
    AdminJobCrawlItemVO markDuplicate(Long itemId, AdminJobCrawlItemReviewRequest request);
}
```

- [ ] **Step 4: Implement deterministic discovery**

Create `DefaultJobSourceDiscoveryServiceImpl`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultJobSourceDiscoveryServiceImpl implements JobSourceDiscoveryService {

    private final JobSourcingProperties properties;

    @Override
    public List<CandidateUrl> discover(JobCrawlTask task) {
        Set<CandidateUrl> results = new LinkedHashSet<>();
        if (StringUtils.hasText(task.getSourceUrl())) {
            results.add(new CandidateUrl(task.getSourceUrl().trim(), task.getSourceType(), detectPlatform(task.getSourceUrl()), "configured source"));
        }
        for (String query : buildQueries(task)) {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            results.add(new CandidateUrl("https://www.google.com/search?q=" + encoded, "public_search", "web", query));
        }
        return new ArrayList<>(results);
    }

    private List<String> buildQueries(JobCrawlTask task) {
        List<String> keywords = split(task.getKeywords());
        List<String> cities = split(task.getCities());
        List<String> experiences = split(task.getExperienceLevels());
        List<String> queries = new ArrayList<>();
        for (String keyword : keywords.isEmpty() ? List.of("程序员") : keywords) {
            for (String city : cities.isEmpty() ? List.of("") : cities) {
                for (String exp : experiences.isEmpty() ? List.of("") : experiences) {
                    String query = String.join(" ", List.of(city, keyword, exp, "招聘", "岗位"))
                            .replaceAll("\\s+", " ").trim();
                    queries.add(query);
                    if (queries.size() >= properties.getMaxSearchQueries()) {
                        return queries;
                    }
                }
            }
        }
        return queries;
    }

    private List<String> split(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return List.of(text.split("[,，\\n]")).stream().map(String::trim).filter(StringUtils::hasText).toList();
    }

    private String detectPlatform(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("zhipin") || lower.contains("boss")) return "boss";
        if (lower.contains("lagou")) return "lagou";
        if (lower.contains("liepin")) return "liepin";
        return "website";
    }
}
```

- [ ] **Step 5: Verify discovery test passes**

Run: `.\mvnw.cmd test -pl . -Dtest=DefaultJobSourceDiscoveryServiceImplTest`

Expected: PASS.

- [ ] **Step 6: Checkpoint**

Run:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/service/JobSourcingAgentService.java backend/src/main/java/com/mianshiba/ai/service/JobSourceDiscoveryService.java backend/src/main/java/com/mianshiba/ai/service/JobPageFetchService.java backend/src/main/java/com/mianshiba/ai/service/JobSourcingExtractService.java backend/src/main/java/com/mianshiba/ai/service/JobSourcingQualityService.java backend/src/main/java/com/mianshiba/ai/service/JobSourcingReviewService.java backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobSourceDiscoveryServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/DefaultJobSourceDiscoveryServiceImplTest.java
```

---

### Task 3: Fetch, Extraction, And Quality Services

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobPageFetchServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AiJobSourcingExtractServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobSourcingQualityServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/DefaultJobSourcingQualityServiceImplTest.java`

- [ ] **Step 1: Write the failing quality test**

Create `DefaultJobSourcingQualityServiceImplTest`:

```java
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
```

- [ ] **Step 2: Run the failing quality test**

Run: `.\mvnw.cmd test -pl . -Dtest=DefaultJobSourcingQualityServiceImplTest`

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement quality scoring**

Create `DefaultJobSourcingQualityServiceImpl`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobSourcingExtractService;
import com.mianshiba.ai.service.JobSourcingQualityService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultJobSourcingQualityServiceImpl implements JobSourcingQualityService {

    @Override
    public QualityResult score(JobSourcingExtractService.ExtractedJobCard card) {
        int score = 20;
        List<String> warnings = new ArrayList<>();
        score += addIfPresent(card.title(), 12, "标题缺失", warnings);
        score += addIfPresent(card.companyName(), 12, "公司缺失", warnings);
        score += addIfPresent(card.city(), 8, "城市缺失", warnings);
        score += addIfPresent(card.salaryRange(), 8, "薪资缺失", warnings);
        score += addIfPresent(card.experienceRequirement(), 8, "经验要求缺失", warnings);
        score += addIfPresent(card.jobDescription(), 12, "岗位职责缺失", warnings);
        score += addIfPresent(card.jobRequirement(), 12, "岗位要求缺失", warnings);
        score += card.confidenceScore() >= 80 ? 8 : 0;
        return new QualityResult(Math.min(score, 100), toJsonArray(warnings));
    }

    private int addIfPresent(String value, int points, String warning, List<String> warnings) {
        if (StringUtils.hasText(value)) {
            return points;
        }
        warnings.add(warning);
        return 0;
    }

    private String toJsonArray(List<String> warnings) {
        return "[" + warnings.stream().map(w -> "\"" + w + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]";
    }
}
```

- [ ] **Step 4: Implement local fetch service**

Create `DefaultJobPageFetchServiceImpl` using the existing crawl logic:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobCrawlService;
import com.mianshiba.ai.service.JobPageFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultJobPageFetchServiceImpl implements JobPageFetchService {

    private final JobCrawlService jobCrawlService;

    @Override
    public FetchedPage fetch(String url) {
        JobCrawlService.CrawlResult result = jobCrawlService.crawl(url);
        return new FetchedPage(result.url(), result.finalUrl(), result.title(), result.content(), result.html(), result.sourcePlatform(), false);
    }
}
```

- [ ] **Step 5: Implement AI extraction service**

Create `AiJobSourcingExtractServiceImpl` with strict JSON parsing. Reuse the extraction pattern from `AiJobParseServiceImpl`, but return `ExtractedJobCard` and include `summary`, `tags`, and `confidenceScore`.

```java
package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.JobPageFetchService;
import com.mianshiba.ai.service.JobSourcingExtractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobSourcingExtractServiceImpl implements JobSourcingExtractService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final String PROMPT = "你是程序员岗位采集审核助手。请从网页内容提取待审核职位卡片，输出严格 JSON："
            + "{\"title\":\"\",\"companyName\":\"\",\"city\":\"\",\"salaryRange\":\"\","
            + "\"experienceRequirement\":\"\",\"educationRequirement\":\"\",\"jobDescription\":\"\","
            + "\"jobRequirement\":\"\",\"techStack\":[],\"summary\":\"\",\"tags\":[],\"confidenceScore\":0}";

    private final ChatClient chatClient;

    @Override
    public ExtractedJobCard extract(JobPageFetchService.FetchedPage page) {
        try {
            String response = chatClient.prompt().system(PROMPT).user(page.content()).call().content();
            JsonNode node = OBJECT_MAPPER.readTree(extractJson(response));
            return new ExtractedJobCard(text(node, "title"), text(node, "companyName"), text(node, "city"),
                    text(node, "salaryRange"), text(node, "experienceRequirement"), text(node, "educationRequirement"),
                    text(node, "jobDescription"), text(node, "jobRequirement"), node.path("techStack").toString(),
                    text(node, "summary"), node.path("tags").toString(), node.path("confidenceScore").asInt(0));
        } catch (Exception e) {
            log.error("职位采集 AI 抽取失败: {}", page.sourceUrl(), e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String extractJson(String text) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : text.trim();
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }
}
```

- [ ] **Step 6: Verify quality service test passes**

Run: `.\mvnw.cmd test -pl . -Dtest=DefaultJobSourcingQualityServiceImplTest`

Expected: PASS.

- [ ] **Step 7: Checkpoint**

Run:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobPageFetchServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/AiJobSourcingExtractServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobSourcingQualityServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/DefaultJobSourcingQualityServiceImplTest.java
```

---

### Task 4: Agent Orchestration And Batch Delegation

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingAgentServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingAgentServiceImplTest.java`

- [ ] **Step 1: Write the failing orchestration test**

Create `JobSourcingAgentServiceImplTest`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobPageFetchService;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import com.mianshiba.ai.service.JobSourcingExtractService;
import com.mianshiba.ai.service.JobSourcingQualityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSourcingAgentServiceImplTest {

    @Mock JobCrawlTaskMapper taskMapper;
    @Mock JobCrawlRunMapper runMapper;
    @Mock JobCrawlItemMapper itemMapper;
    @Mock JobSourceDiscoveryService discoveryService;
    @Mock JobPageFetchService fetchService;
    @Mock JobSourcingExtractService extractService;
    @Mock JobSourcingQualityService qualityService;
    @InjectMocks JobSourcingAgentServiceImpl service;

    @Test
    void runTask_shouldCreatePendingReviewItem() {
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setSourceType("public_feed");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(discoveryService.discover(task)).thenReturn(List.of(new JobSourceDiscoveryService.CandidateUrl("https://example.com/job/1", "public_search", "website", "Java 杭州 招聘")));
        when(fetchService.fetch("https://example.com/job/1")).thenReturn(new JobPageFetchService.FetchedPage("https://example.com/job/1", "https://example.com/job/1", "Java", "content", "<html></html>", "website", false));
        when(extractService.extract(any())).thenReturn(new JobSourcingExtractService.ExtractedJobCard("Java后端", "示例科技", "杭州", "20-30K", "3-5年", "本科", "开发后端", "熟悉 Java", "[\"Java\"]", "后端岗位", "[\"Java后端\"]", 90));
        when(qualityService.score(any())).thenReturn(new JobSourcingQualityService.QualityResult(90, "[]"));

        JobCrawlRun run = service.runTask(1L);

        ArgumentCaptor<JobCrawlItem> itemCaptor = ArgumentCaptor.forClass(JobCrawlItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getReviewStatus()).isEqualTo("pending_review");
        assertThat(itemCaptor.getValue().getQualityScore()).isEqualTo(90);
        assertThat(run.getStatus()).isEqualTo("success");
    }
}
```

- [ ] **Step 2: Run failing orchestration test**

Run: `.\mvnw.cmd test -pl . -Dtest=JobSourcingAgentServiceImplTest`

Expected: FAIL because `JobSourcingAgentServiceImpl` does not exist.

- [ ] **Step 3: Implement orchestration**

Create `JobSourcingAgentServiceImpl` that creates a run, discovers URLs, inserts one item per URL, fetches, extracts, scores, marks `pending_review`, and updates run counters. Keep failures item-scoped.

Required status values:

```java
private static final String RUNNING = "running";
private static final String SUCCESS = "success";
private static final String PARTIAL_SUCCESS = "partial_success";
private static final String FAILED = "failed";
private static final String PENDING_REVIEW = "pending_review";
```

Required item assignments for a successful item:

```java
item.setStatus("extracted");
item.setReviewStatus(PENDING_REVIEW);
item.setSourcePlatform(page.sourcePlatform());
item.setRawTitle(card.title());
item.setRawCompanyName(card.companyName());
item.setRawContent(page.content());
item.setSummary(card.summary());
item.setTagsJson(card.tagsJson());
item.setQualityScore(quality.qualityScore());
item.setConfidenceScore(card.confidenceScore());
item.setExtractedJson(toExtractedJson(card, quality));
```

Implement `toExtractedJson` with `ObjectMapper.writeValueAsString(Map.of(...))` and wrap JSON serialization failures as `BusinessException(ErrorCode.SYSTEM_ERROR)`.

- [ ] **Step 4: Delegate batch crawl service**

Modify `JobBatchCrawlServiceImpl` so `runTask(Long taskId)` delegates to `JobSourcingAgentService.runTask(taskId)`. Keep old logic only if needed behind a private method; first version should prefer the agent path.

- [ ] **Step 5: Verify orchestration test passes**

Run: `.\mvnw.cmd test -pl . -Dtest=JobSourcingAgentServiceImplTest`

Expected: PASS.

- [ ] **Step 6: Run existing admin crawl service tests**

Run: `.\mvnw.cmd test -pl . -Dtest=AdminJobCrawlServiceImplTest`

Expected: PASS.

- [ ] **Step 7: Checkpoint**

Run:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingAgentServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingAgentServiceImplTest.java
```

---

### Task 5: Admin Review Publish API

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlItemReviewRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlReviewQueryRequest.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlItemVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingReviewServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingReviewServiceImplTest.java`

- [ ] **Step 1: Write failing review service test**

Create `JobSourcingReviewServiceImplTest` with three tests: `approve_shouldPublishJobAndMarkApproved`, `reject_shouldNotCreateJob`, and `markDuplicate_shouldLinkExistingJob`.

Core approve assertion:

```java
assertThat(item.getReviewStatus()).isEqualTo("approved");
assertThat(item.getJobId()).isEqualTo(10L);
verify(jobMapper).insert(any(Job.class));
verify(itemMapper).updateById(item);
```

Core reject assertion:

```java
assertThat(item.getReviewStatus()).isEqualTo("rejected");
assertThat(item.getReviewNote()).isEqualTo("公司信息不足");
verify(jobMapper, never()).insert(any(Job.class));
```

Core duplicate assertion:

```java
assertThat(item.getReviewStatus()).isEqualTo("duplicate");
assertThat(item.getDuplicateOfJobId()).isEqualTo(99L);
```

- [ ] **Step 2: Run failing review test**

Run: `.\mvnw.cmd test -pl . -Dtest=JobSourcingReviewServiceImplTest`

Expected: FAIL because review service and DTOs do not exist.

- [ ] **Step 3: Add DTOs**

Create `AdminJobCrawlItemReviewRequest`:

```java
package com.mianshiba.ai.model.dto.admin.jobcrawl;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminJobCrawlItemReviewRequest {
    private Long duplicateOfJobId;
    private String editedExtractedJson;
    @Size(max = 512, message = "审核备注不能超过512个字符")
    private String reviewNote;
}
```

Create `AdminJobCrawlReviewQueryRequest`:

```java
package com.mianshiba.ai.model.dto.admin.jobcrawl;

import lombok.Data;

@Data
public class AdminJobCrawlReviewQueryRequest {
    private String reviewStatus;
    private String keyword;
    private String city;
    private Long taskId;
}
```

- [ ] **Step 4: Implement review service**

Create `JobSourcingReviewServiceImpl`. It should:

- Load item by ID or throw `BusinessException(ErrorCode.NOT_FOUND_ERROR, "采集项不存在")`.
- On approve, parse `editedExtractedJson` if present, otherwise `item.getExtractedJson().toString()`.
- Create `Job` with existing fields and `status="active"`, `applicationStatus="favorite"`, `sourceUrl=item.getSourceUrl()`, `sourcePlatform=item.getSourcePlatform()`.
- Insert `Job`, set `item.reviewStatus="approved"`, `item.jobId=job.id`, update item.
- On reject, set `reviewStatus="rejected"`, `reviewNote=request.reviewNote`.
- On duplicate, require `duplicateOfJobId`, set `reviewStatus="duplicate"`, `duplicateOfJobId`.

- [ ] **Step 5: Expose service methods and controller endpoints**

Add to `AdminJobCrawlService`:

```java
List<AdminJobCrawlItemVO> listReviewItems(String authorizationHeader, AdminJobCrawlReviewQueryRequest request);
AdminJobCrawlItemVO approveItem(String authorizationHeader, Long itemId, AdminJobCrawlItemReviewRequest request);
AdminJobCrawlItemVO rejectItem(String authorizationHeader, Long itemId, AdminJobCrawlItemReviewRequest request);
AdminJobCrawlItemVO markItemDuplicate(String authorizationHeader, Long itemId, AdminJobCrawlItemReviewRequest request);
```

Add to `AdminJobCrawlController`:

```java
@GetMapping("/review-items")
public BaseResponse<List<AdminJobCrawlItemVO>> listReviewItems(...)

@PostMapping("/items/{itemId}/approve")
public BaseResponse<AdminJobCrawlItemVO> approveItem(...)

@PostMapping("/items/{itemId}/reject")
public BaseResponse<AdminJobCrawlItemVO> rejectItem(...)

@PostMapping("/items/{itemId}/duplicate")
public BaseResponse<AdminJobCrawlItemVO> markItemDuplicate(...)
```

- [ ] **Step 6: Verify review tests**

Run: `.\mvnw.cmd test -pl . -Dtest=JobSourcingReviewServiceImplTest,AdminJobCrawlServiceImplTest`

Expected: PASS.

- [ ] **Step 7: Checkpoint**

Run:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlItemReviewRequest.java backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlReviewQueryRequest.java backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlItemVO.java backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingReviewServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingReviewServiceImplTest.java
```

---

### Task 6: Frontend Review Pool

**Files:**
- Modify: `frontend/src/types/jobCrawl.ts`
- Modify: `frontend/src/api/jobCrawl.ts`
- Modify: `frontend/src/stores/jobCrawl.ts`
- Create: `frontend/src/views/admin/AdminJobCrawlReviewPage.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/admin/AdminJobCrawlDetailPage.vue`
- Test: `frontend/src/views/admin/__tests__/AdminJobCrawlReviewPage.spec.ts`

- [ ] **Step 1: Write failing frontend test**

Create `frontend/src/views/admin/__tests__/AdminJobCrawlReviewPage.spec.ts`:

```ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import AdminJobCrawlReviewPage from '../AdminJobCrawlReviewPage.vue'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

describe('AdminJobCrawlReviewPage', () => {
  it('renders pending review cards', async () => {
    const wrapper = mount(AdminJobCrawlReviewPage, {
      global: {
        plugins: [createTestingPinia({ stubActions: false })],
        stubs: ['AdminLayout', 'NbPageHeader', 'NbCard', 'NbButton', 'NbStatusBadge', 'NbLoadingBlock', 'NbEmptyState'],
      },
    })

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('职位采集审核池')
  })
})
```

- [ ] **Step 2: Run failing frontend test**

Run from `frontend/`: `npm run test:unit -- AdminJobCrawlReviewPage.spec.ts`

Expected: FAIL because page does not exist.

- [ ] **Step 3: Add frontend types and APIs**

In `jobCrawl.ts`, add:

```ts
export type JobCrawlReviewStatus = 'pending_review' | 'approved' | 'rejected' | 'duplicate' | 'failed'

export interface AdminJobCrawlItemReviewRequest {
  duplicateOfJobId?: number
  editedExtractedJson?: string
  reviewNote?: string
}

export interface AdminJobCrawlReviewQueryRequest {
  reviewStatus?: JobCrawlReviewStatus
  keyword?: string
  city?: string
  taskId?: number
}
```

Extend `AdminJobCrawlItemVO` with:

```ts
taskId: number
sourcePlatform: string
rawContent?: string
extractedJson?: string
summary?: string
tagsJson?: string
qualityScore: number
confidenceScore: number
duplicateOfJobId?: number | null
reviewStatus: JobCrawlReviewStatus
reviewNote?: string
createTime?: string
```

In `api/jobCrawl.ts`, add:

```ts
export function listJobCrawlReviewItems(params?: AdminJobCrawlReviewQueryRequest) {
  return request.get<BaseResponse<AdminJobCrawlItemVO[]>>('/api/admin/job-crawl/review-items', { params })
}

export function approveJobCrawlItem(id: number, data: AdminJobCrawlItemReviewRequest) {
  return request.post<BaseResponse<AdminJobCrawlItemVO>>(`/api/admin/job-crawl/items/${id}/approve`, data)
}

export function rejectJobCrawlItem(id: number, data: AdminJobCrawlItemReviewRequest) {
  return request.post<BaseResponse<AdminJobCrawlItemVO>>(`/api/admin/job-crawl/items/${id}/reject`, data)
}

export function markJobCrawlItemDuplicate(id: number, data: AdminJobCrawlItemReviewRequest) {
  return request.post<BaseResponse<AdminJobCrawlItemVO>>(`/api/admin/job-crawl/items/${id}/duplicate`, data)
}
```

- [ ] **Step 4: Build review page**

Create `AdminJobCrawlReviewPage.vue` with a page header titled `职位采集审核池`, filters for status/keyword/city, and cards showing title, company, salary, city, summary, tags, quality score, confidence score, source URL, and actions `通过`, `拒绝`, `标记重复`.

Use these action handlers:

```ts
async function handleApprove(item: AdminJobCrawlItemVO) {
  await approveJobCrawlItem(item.id, {})
  ElMessage.success('职位已发布')
  await loadItems()
}

async function handleReject(item: AdminJobCrawlItemVO) {
  await rejectJobCrawlItem(item.id, { reviewNote: '管理员拒绝' })
  ElMessage.success('已拒绝')
  await loadItems()
}

async function handleDuplicate(item: AdminJobCrawlItemVO) {
  if (!item.duplicateOfJobId) {
    ElMessage.warning('该卡片没有可关联的重复职位')
    return
  }
  await markJobCrawlItemDuplicate(item.id, { duplicateOfJobId: item.duplicateOfJobId })
  ElMessage.success('已标记重复')
  await loadItems()
}
```

- [ ] **Step 5: Add route and detail link**

Add route in `frontend/src/router/index.ts`:

```ts
{
  path: '/admin/job-crawl/review',
  name: 'AdminJobCrawlReview',
  component: () => import('@/views/admin/AdminJobCrawlReviewPage.vue'),
  meta: { requiresAuth: true, requiresAdmin: true },
}
```

Add a button in `AdminJobCrawlDetailPage.vue` actions:

```vue
<NbButton variant="primary" @click="router.push('/admin/job-crawl/review')">审核池</NbButton>
```

- [ ] **Step 6: Verify frontend**

Run from `frontend/`:

```powershell
npm run test:unit -- AdminJobCrawlReviewPage.spec.ts
npm run type-check
```

Expected: PASS.

- [ ] **Step 7: Checkpoint**

Run:

```powershell
git diff -- frontend/src/types/jobCrawl.ts frontend/src/api/jobCrawl.ts frontend/src/stores/jobCrawl.ts frontend/src/views/admin/AdminJobCrawlReviewPage.vue frontend/src/router/index.ts frontend/src/views/admin/AdminJobCrawlDetailPage.vue frontend/src/views/admin/__tests__/AdminJobCrawlReviewPage.spec.ts
```

---

### Task 7: User Visibility Boundary

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobServiceImplTest.java`

- [ ] **Step 1: Write failing visibility test**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/JobServiceImplTest.java` or extend the existing file. The test targets a new package-visible helper `filterUserVisibleJobs(List<Job> jobs)` so the visibility rule is independently testable without a database.

Test body:

```java
@Test
void listJobs_shouldExcludeInactiveJobsFromUserFacingPool() {
    Job active = new Job();
    active.setId(1L);
    active.setStatus("active");
    active.setTitle("Java后端");

    Job pending = new Job();
    pending.setId(2L);
    pending.setStatus("pending_review");
    pending.setTitle("未审核岗位");

    List<Job> result = service.filterUserVisibleJobs(List.of(active, pending));

    assertThat(result).extracting(Job::getId).containsExactly(1L);
}
```

- [ ] **Step 2: Run failing visibility test**

Run: `.\mvnw.cmd test -pl . -Dtest=JobServiceImplTest`

Expected: FAIL until `filterUserVisibleJobs(List<Job> jobs)` exists.

- [ ] **Step 3: Implement active-only filtering**

Update user-facing queries in `JobServiceImpl` so `listJobs`, `getJob`, recommendation-facing methods, and favorite list paths only expose jobs where `job.status` is `active`. Do not filter admin review APIs.

Add this package-visible helper to `JobServiceImpl`:

```java
List<Job> filterUserVisibleJobs(List<Job> jobs) {
    return jobs.stream()
            .filter(job -> "active".equals(job.getStatus()))
            .toList();
}
```

For wrapper-based query paths, add:

```java
.eq(Job::getStatus, "active")
```

For stream paths, add:

```java
.filter(job -> "active".equals(job.getStatus()))
```

- [ ] **Step 4: Verify visibility test**

Run: `.\mvnw.cmd test -pl . -Dtest=JobServiceImplTest`

Expected: PASS.

- [ ] **Step 5: Checkpoint**

Run:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/JobServiceImplTest.java
```

---

### Task 8: Final Verification

**Files:**
- Modify only files required by failures found in this task.

- [ ] **Step 1: Run backend focused tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -pl . -Dtest=InitSqlJobSourcingAgentTest,DefaultJobSourceDiscoveryServiceImplTest,DefaultJobSourcingQualityServiceImplTest,JobSourcingAgentServiceImplTest,JobSourcingReviewServiceImplTest,AdminJobCrawlServiceImplTest
```

Expected: PASS.

- [ ] **Step 2: Run backend full test suite**

Run from `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 3: Run frontend verification**

Run from `frontend/`:

```powershell
npm run type-check
npm run test:unit
```

Expected: PASS.

- [ ] **Step 4: Build frontend**

Run from `frontend/`:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 5: Inspect git diff**

Run:

```powershell
git status --short
```

Expected: only intended job sourcing agent files are changed after the final task.

- [ ] **Step 6: Commit verification fixes if needed**

If Step 1-4 required additional fixes, commit them:

```powershell
git diff -- <fixed-files>
```

If no additional fixes were needed, do not create an empty commit.
