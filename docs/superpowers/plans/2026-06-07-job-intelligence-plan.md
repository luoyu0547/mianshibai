# 职位情报与岗位推荐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建职位链接/官网解析、公司画像、岗位 AI 分析、简历岗位匹配和职位页面联动的第一阶段可演示闭环。

**Architecture:** 后端沿用 `controller -> service -> mapper` 分层，将网页抓取、结构化解析、AI 分析、推荐评分拆成独立 service；前端沿用 Vue 3 + Pinia + API 文件模式，新增 job 类型、API、store 和页面。第一阶段只做用户主动粘贴链接解析，不做后台大规模定时爬取。

**Tech Stack:** Spring Boot 3.5.x、Java 17、MyBatis-Plus、Spring AI `ChatClient`、MySQL JSON、Vue 3、TypeScript、Pinia、Vue Router、Element Plus、axios。

---

## Scope Boundary

本计划实现设计文档第一阶段：

- 招聘平台单职位链接解析。
- 公司官网或官网招聘页解析。
- 公司画像与专精特新/小巨人证据识别。
- 单职位 AI 分析。
- 用户简历与职位匹配评分。
- 职位解析页、职位详情页、公司画像页、收藏职位页。
- 职位详情进入针对岗位优化简历和针对岗位模拟面试的入口。

本计划不实现：

- 自动投递。
- 大规模职位采集调度。
- 招聘平台反爬绕过。
- 官方名单导入后台。第一阶段只预留 `official_list` 证据类型。
- 完整多平台规则解析库。第一阶段以通用网页正文抓取 + AI 结构化提取为主。

## File Structure

### Backend Create

- `backend/src/main/java/com/mianshiba/ai/model/entity/Company.java`：公司画像实体。
- `backend/src/main/java/com/mianshiba/ai/model/entity/CompanyCertification.java`：企业资质证据实体。
- `backend/src/main/java/com/mianshiba/ai/model/entity/Job.java`：职位实体。
- `backend/src/main/java/com/mianshiba/ai/model/entity/JobAnalysis.java`：岗位 AI 分析实体。
- `backend/src/main/java/com/mianshiba/ai/model/entity/JobMatch.java`：用户简历职位匹配实体。
- `backend/src/main/java/com/mianshiba/ai/model/entity/JobFavorite.java`：职位收藏实体。
- `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobImportRequest.java`：链接导入请求。
- `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobMatchRequest.java`：岗位匹配请求。
- `backend/src/main/java/com/mianshiba/ai/model/vo/job/CompanyCertificationVO.java`：资质证据视图。
- `backend/src/main/java/com/mianshiba/ai/model/vo/job/CompanyVO.java`：公司画像视图。
- `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobAnalysisVO.java`：岗位分析视图。
- `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobImportResultVO.java`：导入结果视图。
- `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobMatchVO.java`：匹配结果视图。
- `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobVO.java`：职位详情视图。
- `backend/src/main/java/com/mianshiba/ai/mapper/CompanyMapper.java`：公司 Mapper。
- `backend/src/main/java/com/mianshiba/ai/mapper/CompanyCertificationMapper.java`：资质证据 Mapper。
- `backend/src/main/java/com/mianshiba/ai/mapper/JobMapper.java`：职位 Mapper。
- `backend/src/main/java/com/mianshiba/ai/mapper/JobAnalysisMapper.java`：岗位分析 Mapper。
- `backend/src/main/java/com/mianshiba/ai/mapper/JobMatchMapper.java`：职位匹配 Mapper。
- `backend/src/main/java/com/mianshiba/ai/mapper/JobFavoriteMapper.java`：职位收藏 Mapper。
- `backend/src/main/java/com/mianshiba/ai/service/JobService.java`：职位模块业务接口。
- `backend/src/main/java/com/mianshiba/ai/service/JobCrawlService.java`：网页抓取接口。
- `backend/src/main/java/com/mianshiba/ai/service/JobParseService.java`：职位结构化解析接口。
- `backend/src/main/java/com/mianshiba/ai/service/CompanyProfileService.java`：公司画像接口。
- `backend/src/main/java/com/mianshiba/ai/service/AiJobAnalysisService.java`：AI 岗位分析接口。
- `backend/src/main/java/com/mianshiba/ai/service/JobRecommendService.java`：推荐评分接口。
- `backend/src/main/java/com/mianshiba/ai/service/ResumeJobMatchService.java`：简历职位匹配接口。
- `backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java`：职位模块业务实现。
- `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobCrawlServiceImpl.java`：默认网页抓取实现。
- `backend/src/main/java/com/mianshiba/ai/service/impl/AiJobParseServiceImpl.java`：AI 结构化解析实现。
- `backend/src/main/java/com/mianshiba/ai/service/impl/CompanyProfileServiceImpl.java`：公司画像实现。
- `backend/src/main/java/com/mianshiba/ai/service/impl/AiJobAnalysisServiceImpl.java`：AI 岗位分析实现。
- `backend/src/main/java/com/mianshiba/ai/service/impl/JobRecommendServiceImpl.java`：推荐评分实现。
- `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeJobMatchServiceImpl.java`：简历匹配实现。
- `backend/src/main/java/com/mianshiba/ai/controller/JobController.java`：职位模块 REST API。
- `backend/src/test/java/com/mianshiba/ai/service/JobRecommendServiceImplTest.java`：推荐评分单元测试。
- `backend/src/test/java/com/mianshiba/ai/service/CompanyProfileServiceImplTest.java`：企业资质判断测试。

### Backend Modify

- `backend/src/main/resources/sql/init.sql`：新增职位模块 6 张表。
- `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`：新增职位模块错误码。
- `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`：简历优化接口预留 `jobId` 入参的重载或新 DTO 字段。
- `backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiOptimizeRequest.java`：新增可选 `jobId`。
- `backend/src/main/java/com/mianshiba/ai/model/dto/interview/InterviewCreateRequest.java`：新增可选 `jobId`。
- `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java`：创建面试和生成问题时读取职位分析。

### Frontend Create

- `frontend/src/types/job.ts`：职位模块类型定义。
- `frontend/src/api/job.ts`：职位模块 API。
- `frontend/src/stores/job.ts`：职位 Pinia store。
- `frontend/src/views/job/JobImportPage.vue`：职位/官网链接解析页。
- `frontend/src/views/job/JobDetailPage.vue`：职位详情页。
- `frontend/src/views/job/CompanyDetailPage.vue`：公司画像页。
- `frontend/src/views/job/JobFavoritePage.vue`：收藏职位页。

### Frontend Modify

- `frontend/src/router/index.ts`：新增 job 路由。
- `frontend/src/layouts/MainLayout.vue`：新增职位导航入口。
- `frontend/src/api/resume.ts`：简历优化 API 支持传入 `jobId`。
- `frontend/src/types/resume.ts`：`AiOptimizeRequest` 增加可选 `jobId`。
- `frontend/src/api/interview.ts`：创建面试 API 支持传入 `jobId`。
- `frontend/src/types/interview.ts`：`InterviewCreateRequest` 增加可选 `jobId`。

## Task 1: Backend Schema And Error Codes

**Files:**

- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`

- [ ] **Step 1: Add job tables to `init.sql`**

Append the six SQL tables from `docs/superpowers/specs/2026-06-07-job-intelligence-design.md` sections 6.1 through 6.6 after `interview_report`.

- [ ] **Step 2: Add job error codes**

Modify `ErrorCode.java` to include these constants without changing existing numeric values:

```java
JOB_CRAWL_ERROR(50010, "职位页面抓取失败"),
JOB_PARSE_ERROR(50011, "职位信息解析失败"),
JOB_NOT_FOUND_ERROR(40410, "职位不存在"),
COMPANY_NOT_FOUND_ERROR(40411, "公司不存在"),
JOB_MATCH_ERROR(50012, "职位匹配分析失败");
```

- [ ] **Step 3: Verify backend compiles**

Run in `backend/`:

```powershell
.\mvnw.cmd test -DskipTests
```

Expected: Maven compilation succeeds with exit code 0.

## Task 2: Backend Entity And Mapper Layer

**Files:**

- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/Company.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CompanyCertification.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/Job.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobAnalysis.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobMatch.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobFavorite.java`
- Create: mapper files under `backend/src/main/java/com/mianshiba/ai/mapper/`

- [ ] **Step 1: Create entities**

Use existing entity style: Lombok `@Data`, MyBatis-Plus `@TableName`, `@TableId(type = IdType.AUTO)`, `@TableLogic` for `isDelete`, and Java `LocalDateTime`.

Example for `Job.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyId;

    private String title;

    private String companyName;

    private String sourcePlatform;

    private String sourceUrl;

    private String city;

    private String salaryRange;

    private String experienceRequirement;

    private String educationRequirement;

    private String jobDescription;

    private String jobRequirement;

    private String techStack;

    private String rawContent;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
```

JSON columns should use `String` in entities for consistency with existing resume `sectionData` handling.

- [ ] **Step 2: Create mapper interfaces**

Each mapper extends `BaseMapper<Entity>` and has no custom methods in first phase.

Example:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.Job;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
```

- [ ] **Step 3: Verify backend compiles**

Run in `backend/`:

```powershell
.\mvnw.cmd test -DskipTests
```

Expected: Maven compilation succeeds with exit code 0.

## Task 3: Backend DTO And VO Layer

**Files:**

- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobImportRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobMatchRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/CompanyCertificationVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/CompanyVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobAnalysisVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobImportResultVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobMatchVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobVO.java`

- [ ] **Step 1: Create request DTOs**

`JobImportRequest.java`:

```java
package com.mianshiba.ai.model.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class JobImportRequest {

    @NotBlank(message = "链接不能为空")
    private String url;

    @NotBlank(message = "导入类型不能为空")
    @Pattern(regexp = "job|company_website|company_career_page", message = "导入类型不合法")
    private String importType;
}
```

`JobMatchRequest.java`:

```java
package com.mianshiba.ai.model.dto.job;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobMatchRequest {

    @NotNull(message = "简历 id 不能为空")
    private Long resumeId;
}
```

- [ ] **Step 2: Create VO classes**

Use Lombok `@Data`. `JobVO` should include nested `CompanyVO company` and `JobAnalysisVO analysis`. `JobImportResultVO` should include `String resultType`, `Long jobId`, `Long companyId`, `JobVO job`, `CompanyVO company`.

- [ ] **Step 3: Verify backend compiles**

Run in `backend/`:

```powershell
.\mvnw.cmd test -DskipTests
```

Expected: Maven compilation succeeds with exit code 0.

## Task 4: Recommendation Scoring Unit Tests

**Files:**

- Create: `backend/src/test/java/com/mianshiba/ai/service/JobRecommendServiceImplTest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobRecommendService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobRecommendServiceImpl.java`

- [ ] **Step 1: Write failing tests**

Create tests covering weighted score and recommendation labels:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.service.impl.JobRecommendServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobRecommendServiceImplTest {

    private final JobRecommendService service = new JobRecommendServiceImpl();

    @Test
    void calculateTotalScoreUsesOpportunityCostWeights() {
        int score = service.calculateTotalScore(80, 90, 70, 60, 50);

        assertThat(score).isEqualTo(77);
    }

    @Test
    void recommendReturnsRecommendedForHighScoreAndGoodMatch() {
        String recommendation = service.recommend(82, 78, 70);

        assertThat(recommendation).isEqualTo("recommended");
    }

    @Test
    void recommendReturnsStretchForHighGrowthButMediumMatch() {
        String recommendation = service.recommend(62, 88, 84);

        assertThat(recommendation).isEqualTo("stretch");
    }

    @Test
    void recommendReturnsNotRecommendedForLowMatch() {
        String recommendation = service.recommend(35, 80, 70);

        assertThat(recommendation).isEqualTo("not_recommended");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=JobRecommendServiceImplTest
```

Expected: FAIL because `JobRecommendService` and implementation do not exist.

- [ ] **Step 3: Implement scoring service**

`JobRecommendService.java`:

```java
package com.mianshiba.ai.service;

public interface JobRecommendService {

    int calculateTotalScore(int matchScore, int growthScore, int techGrowthScore, int salaryCityScore, int experienceFitScore);

    String recommend(int matchScore, int growthScore, int techGrowthScore);
}
```

`JobRecommendServiceImpl.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobRecommendService;
import org.springframework.stereotype.Service;

@Service
public class JobRecommendServiceImpl implements JobRecommendService {

    @Override
    public int calculateTotalScore(int matchScore, int growthScore, int techGrowthScore, int salaryCityScore, int experienceFitScore) {
        double score = matchScore * 0.4
                + growthScore * 0.25
                + techGrowthScore * 0.15
                + salaryCityScore * 0.1
                + experienceFitScore * 0.1;
        return (int) Math.round(score);
    }

    @Override
    public String recommend(int matchScore, int growthScore, int techGrowthScore) {
        if (matchScore < 45) {
            return "not_recommended";
        }
        if (matchScore >= 75 && growthScore >= 65) {
            return "recommended";
        }
        if (matchScore >= 55 && growthScore >= 80 && techGrowthScore >= 75) {
            return "stretch";
        }
        return "cautious";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=JobRecommendServiceImplTest
```

Expected: PASS.

## Task 5: Company Certification Rules Tests

**Files:**

- Create: `backend/src/test/java/com/mianshiba/ai/service/CompanyProfileServiceImplTest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/CompanyProfileService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/CompanyProfileServiceImpl.java`

- [ ] **Step 1: Write failing tests**

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.service.impl.CompanyProfileServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyProfileServiceImplTest {

    private final CompanyProfileService service = new CompanyProfileServiceImpl(null, null);

    @Test
    void officialListEvidenceIsConfirmed() {
        String status = service.resolveCertificationStatus("official_list", "入选专精特新小巨人企业名单");

        assertThat(status).isEqualTo("confirmed");
    }

    @Test
    void WebsiteExplicitEvidenceIsConfirmed() {
        String status = service.resolveCertificationStatus("website", "公司已获评国家级专精特新小巨人企业");

        assertThat(status).isEqualTo("confirmed");
    }

    @Test
    void NewsEvidenceIsSuspected() {
        String status = service.resolveCertificationStatus("news", "据报道该公司为专精特新企业");

        assertThat(status).isEqualTo("suspected");
    }

    @Test
    void EmptyEvidenceIsUnknown() {
        String status = service.resolveCertificationStatus("ai_inferred", "");

        assertThat(status).isEqualTo("unknown");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=CompanyProfileServiceImplTest
```

Expected: FAIL because `CompanyProfileService` does not exist.

- [ ] **Step 3: Implement status resolver**

`CompanyProfileService.java`:

```java
package com.mianshiba.ai.service;

public interface CompanyProfileService {

    String resolveCertificationStatus(String evidenceSource, String evidenceText);
}
```

`CompanyProfileServiceImpl.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.CompanyCertificationMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyProfileServiceImpl implements CompanyProfileService {

    private final CompanyMapper companyMapper;
    private final CompanyCertificationMapper companyCertificationMapper;

    @Override
    public String resolveCertificationStatus(String evidenceSource, String evidenceText) {
        if (StringUtils.isBlank(evidenceText)) {
            return "unknown";
        }
        if ("official_list".equals(evidenceSource)) {
            return "confirmed";
        }
        if ("website".equals(evidenceSource)
                && (evidenceText.contains("专精特新") || evidenceText.contains("小巨人"))) {
            return "confirmed";
        }
        if ("news".equals(evidenceSource) || "ai_inferred".equals(evidenceSource)) {
            return "suspected";
        }
        return "unknown";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=CompanyProfileServiceImplTest
```

Expected: PASS.

## Task 6: Crawl And AI Parse Services

**Files:**

- Create: `backend/src/main/java/com/mianshiba/ai/service/JobCrawlService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobParseService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/AiJobAnalysisService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobCrawlServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AiJobParseServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AiJobAnalysisServiceImpl.java`

- [ ] **Step 1: Define crawl result as nested record in `JobCrawlService`**

```java
package com.mianshiba.ai.service;

public interface JobCrawlService {

    CrawlResult crawl(String url);

    record CrawlResult(String url, String finalUrl, String title, String content, String html, String sourcePlatform) {
    }
}
```

- [ ] **Step 2: Implement basic HTTP crawler**

Use Spring `RestClient` or `RestTemplate` available through Spring Web. Strip scripts/styles with regex in first phase and cap content length to 12000 characters before AI parsing.

```java
String text = html
        .replaceAll("(?is)<script.*?</script>", " ")
        .replaceAll("(?is)<style.*?</style>", " ")
        .replaceAll("(?is)<[^>]+>", " ")
        .replaceAll("\\s+", " ")
        .trim();
if (text.length() > 12000) {
    text = text.substring(0, 12000);
}
```

- [ ] **Step 3: Define parse service output**

`JobParseService` should expose:

```java
Job parseJob(JobCrawlService.CrawlResult crawlResult);
Company parseCompany(JobCrawlService.CrawlResult crawlResult);
```

- [ ] **Step 4: Implement AI parse prompt**

`AiJobParseServiceImpl` should call `ChatClient` with a system prompt that requires JSON only. If parsing JSON fails, throw `BusinessException(ErrorCode.JOB_PARSE_ERROR)`.

- [ ] **Step 5: Verify backend compiles**

Run in `backend/`:

```powershell
.\mvnw.cmd test -DskipTests
```

Expected: Maven compilation succeeds with exit code 0.

## Task 7: Job Service And Controller

**Files:**

- Create: `backend/src/main/java/com/mianshiba/ai/service/JobService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/JobController.java`

- [ ] **Step 1: Define `JobService` interface**

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.job.JobImportRequest;
import com.mianshiba.ai.model.dto.job.JobMatchRequest;
import com.mianshiba.ai.model.vo.job.CompanyVO;
import com.mianshiba.ai.model.vo.job.JobImportResultVO;
import com.mianshiba.ai.model.vo.job.JobMatchVO;
import com.mianshiba.ai.model.vo.job.JobVO;

import java.util.List;

public interface JobService {

    JobImportResultVO importUrl(String authorizationHeader, JobImportRequest request);

    JobVO getJob(String authorizationHeader, Long jobId);

    CompanyVO getCompany(String authorizationHeader, Long companyId);

    JobMatchVO matchJob(String authorizationHeader, Long jobId, JobMatchRequest request);

    void favoriteJob(String authorizationHeader, Long jobId);

    void unfavoriteJob(String authorizationHeader, Long jobId);

    List<JobVO> listFavorites(String authorizationHeader);
}
```

- [ ] **Step 2: Implement `JobController`**

Use existing controller pattern with `@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)` and `ResultUtils.success(...)`.

Endpoints:

```text
POST /api/job/import-url
GET /api/job/{jobId}
GET /api/job/company/{companyId}
POST /api/job/{jobId}/match
POST /api/job/{jobId}/favorite
DELETE /api/job/{jobId}/favorite
GET /api/job/favorites
```

- [ ] **Step 3: Implement `JobServiceImpl.importUrl`**

Flow:

```text
1. validate token through existing UserService/JwtUtils pattern used by ResumeServiceImpl
2. call JobCrawlService.crawl(url)
3. if importType is job, parse Job and Company, save both, save JobAnalysis
4. if importType is company_website or company_career_page, parse Company, save Company
5. return JobImportResultVO with resultType job or company
```

- [ ] **Step 4: Implement `matchJob`**

Flow:

```text
1. load job and resume owned by current user
2. call ResumeJobMatchService.match(userId, resumeId, jobId)
3. persist JobMatch
4. return JobMatchVO
```

- [ ] **Step 5: Verify backend compiles**

Run in `backend/`:

```powershell
.\mvnw.cmd test -DskipTests
```

Expected: Maven compilation succeeds with exit code 0.

## Task 8: Resume And Interview Job Linkage

**Files:**

- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiOptimizeRequest.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/interview/InterviewCreateRequest.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java`

- [ ] **Step 1: Add optional job id fields**

Add this field to both request DTOs:

```java
private Long jobId;
```

- [ ] **Step 2: Use job context in resume optimization**

When `AiOptimizeRequest.jobId` is not null, load `Job` and `JobAnalysis`, then append JD context to the existing prompt. If not found, throw `BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR)`.

- [ ] **Step 3: Use job context in interview creation**

When `InterviewCreateRequest.jobId` is not null, load `Job` and `JobAnalysis`, then include job title, JD summary, core skills, hidden requirements and interview focus in the AI question prompt.

- [ ] **Step 4: Verify backend tests**

Run in `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: all backend tests pass.

## Task 9: Frontend Types, API, Store, And Routes

**Files:**

- Create: `frontend/src/types/job.ts`
- Create: `frontend/src/api/job.ts`
- Create: `frontend/src/stores/job.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Create `job.ts` types**

Include interfaces matching backend VO names: `CompanyCertificationVO`, `CompanyVO`, `JobAnalysisVO`, `JobVO`, `JobImportRequest`, `JobImportResultVO`, `JobMatchRequest`, `JobMatchVO`.

- [ ] **Step 2: Create `api/job.ts`**

```ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { CompanyVO, JobImportRequest, JobImportResultVO, JobMatchRequest, JobMatchVO, JobVO } from '@/types/job'

export function importJobUrl(data: JobImportRequest) {
  return request.post<BaseResponse<JobImportResultVO>>('/api/job/import-url', data)
}

export function getJobDetail(jobId: number) {
  return request.get<BaseResponse<JobVO>>(`/api/job/${jobId}`)
}

export function getCompanyDetail(companyId: number) {
  return request.get<BaseResponse<CompanyVO>>(`/api/job/company/${companyId}`)
}

export function matchJob(jobId: number, data: JobMatchRequest) {
  return request.post<BaseResponse<JobMatchVO>>(`/api/job/${jobId}/match`, data)
}

export function favoriteJob(jobId: number) {
  return request.post<BaseResponse<boolean>>(`/api/job/${jobId}/favorite`)
}

export function unfavoriteJob(jobId: number) {
  return request.delete<BaseResponse<boolean>>(`/api/job/${jobId}/favorite`)
}

export function listFavoriteJobs() {
  return request.get<BaseResponse<JobVO[]>>('/api/job/favorites')
}
```

- [ ] **Step 3: Create Pinia store**

Follow existing `resume.ts` store style: `jobList`, `currentJob`, `currentCompany`, `currentMatch`, `loading`, actions for import, fetch, match, favorite and list favorites.

- [ ] **Step 4: Add routes**

Add authenticated routes:

```ts
{
  path: '/job/import',
  name: 'JobImport',
  component: () => import('@/views/job/JobImportPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/job/favorites',
  name: 'JobFavorites',
  component: () => import('@/views/job/JobFavoritePage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/job/:id',
  name: 'JobDetail',
  component: () => import('@/views/job/JobDetailPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/company/:id',
  name: 'CompanyDetail',
  component: () => import('@/views/job/CompanyDetailPage.vue'),
  meta: { requiresAuth: true },
}
```

- [ ] **Step 5: Verify frontend type-check**

Run in `frontend/`:

```powershell
npm run type-check
```

Expected: type-check passes.

## Task 10: Frontend Job Pages

**Files:**

- Create: `frontend/src/views/job/JobImportPage.vue`
- Create: `frontend/src/views/job/JobDetailPage.vue`
- Create: `frontend/src/views/job/CompanyDetailPage.vue`
- Create: `frontend/src/views/job/JobFavoritePage.vue`

- [ ] **Step 1: Implement import page**

Use `MainLayout`, `NbCard`, `NbButton`, Element Plus input/select. Form fields:

```ts
const form = reactive({
  url: '',
  importType: 'job' as 'job' | 'company_website' | 'company_career_page',
})
```

Submit flow:

```text
1. call jobStore.importUrl(form)
2. if resultType is job and jobId exists, router.push(`/job/${jobId}`)
3. if resultType is company and companyId exists, router.push(`/company/${companyId}`)
4. show Element Plus error message when API fails
```

- [ ] **Step 2: Implement job detail page**

Sections:

```text
1. job header: title, companyName, city, salaryRange
2. recommendation panel: totalScore and recommendation label after match
3. tech stack tags
4. job description and requirements
5. AI analysis: core skills, hidden requirements, risks, interview focus
6. action buttons: favorite, match resume, optimize resume, start interview
```

For match resume, show a select populated by `resumeStore.fetchResumeList()`.

- [ ] **Step 3: Implement company detail page**

Sections:

```text
1. company name, industry, city, website
2. certification badges: confirmed/suspected/unknown
3. evidence list with evidenceText and evidenceUrl
4. mainBusiness and techDirection
```

- [ ] **Step 4: Implement favorites page**

Use card grid. Each card links to `/job/:id`, shows recommendation label when available, city, salary and company certification badge.

- [ ] **Step 5: Verify frontend build**

Run in `frontend/`:

```powershell
npm run build
```

Expected: type-check and Vite build pass.

## Task 11: End-To-End Verification

**Files:**

- No new files.

- [ ] **Step 1: Backend full test**

Run in `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: all backend tests pass.

- [ ] **Step 2: Backend package without tests**

Run in `backend/`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: package succeeds.

- [ ] **Step 3: Frontend build**

Run in `frontend/`:

```powershell
npm run build
```

Expected: type-check and build pass.

- [ ] **Step 4: Manual smoke test**

Run backend and frontend with the existing project commands. Verify these flows manually:

```text
1. Login
2. Open /job/import
3. Paste a normal company career page URL
4. Import result routes to company detail or job detail
5. Open job detail
6. Select a resume and run match
7. Confirm recommendation label, gaps, resume suggestions and interview focus display
8. Click favorite and verify /job/favorites contains the job
9. Click start interview and verify /interview/new receives job context through route/query or selected state
```

Expected: all listed flows are usable without console errors.

## Commit Guidance

Only commit if the user explicitly asks for commits. If commits are requested, use small commits after verified tasks:

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java
git commit -m "feat: add job intelligence schema"
```

Then repeat for backend service layer, frontend job pages, and cross-module linkage.

## Self-Review Checklist

- Spec coverage: tasks cover schema, crawl, AI parse, company certification, recommendation scoring, job import, job detail, favorites, resume linkage and interview linkage.
- Scope control: this plan excludes automatic delivery, anti-crawler bypass, scheduled batch crawling and official-list admin import.
- Type consistency: backend request/VO names match frontend type names and API response names.
- Verification: each implementation phase includes Maven or npm verification commands.
