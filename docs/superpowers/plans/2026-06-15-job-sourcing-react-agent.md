# Job Sourcing ReAct Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild job sourcing as a controlled ReAct-style agent with platform tools, Playwright browser profiles, and verified Boss/Zhipin and Shixiseng crawling.

**Architecture:** Keep existing `job_crawl_task`, `job_crawl_run`, and `job_crawl_item` tables as the admin-facing workflow. Add platform authorization sessions, browser-backed platform adapters, Spring AI tools, and a new agent service that controls discovery, detail fetching, extraction, quality scoring, persistence, and trace limits.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis-Plus, Spring AI `ChatClient` tools, Microsoft Playwright Java, JUnit 5, Mockito, AssertJ.

---

## File Structure

**Create:**
- `backend/src/main/java/com/mianshiba/ai/model/entity/PlatformAuthSession.java`: platform authorization session entity.
- `backend/src/main/java/com/mianshiba/ai/mapper/PlatformAuthSessionMapper.java`: MyBatis mapper.
- `backend/src/main/java/com/mianshiba/ai/model/enums/PlatformAuthStatus.java`: `not_authorized`, `authorized`, `expired`, `auth_required`, `error`.
- `backend/src/main/java/com/mianshiba/ai/model/enums/JobSourcingRunStatus.java`: `running`, `success`, `partial_success`, `failed`, `auth_required`.
- `backend/src/main/java/com/mianshiba/ai/model/dto/jobsourcing/*.java`: `JobDiscoveryRequest`, `JobListEntry`, `FetchedJobPage`, `ExtractedJobCard`.
- `backend/src/main/java/com/mianshiba/ai/service/BrowserSessionService.java`: browser profile authorization operations.
- `backend/src/main/java/com/mianshiba/ai/service/JobPlatformAdapter.java`: platform adapter contract.
- `backend/src/main/java/com/mianshiba/ai/service/JobPlatformAdapterRegistry.java`: adapter lookup.
- `backend/src/main/java/com/mianshiba/ai/service/JobSourcingReActAgentService.java`: new agent entry point.
- `backend/src/main/java/com/mianshiba/ai/service/impl/PlaywrightBrowserSessionServiceImpl.java`: browser profile session service.
- `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobPlatformAdapterRegistry.java`: adapter registry implementation.
- `backend/src/main/java/com/mianshiba/ai/service/impl/BossJobPlatformAdapter.java`: Boss/Zhipin adapter.
- `backend/src/main/java/com/mianshiba/ai/service/impl/ShixisengJobPlatformAdapter.java`: Shixiseng adapter.
- `backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingReActAgentServiceImpl.java`: agent orchestration.
- `backend/src/main/java/com/mianshiba/ai/service/tool/JobSourcingTools.java`: Spring AI tool object.
- `backend/src/test/resources/job-sourcing/*.html`: fixture pages.

**Modify:**
- `backend/pom.xml`: add Playwright dependency.
- `backend/src/main/resources/application.yml`: add browser profile config.
- `backend/src/main/resources/sql/init.sql`: add `platform_auth_session`; document `auth_required` run status.
- `backend/src/main/java/com/mianshiba/ai/config/JobSourcingProperties.java`: add browser settings.
- `backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java`: add platform auth endpoints.
- `backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java`: add platform auth service methods.
- `backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java`: delegate auth methods to browser session service.
- `backend/src/main/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImpl.java`: delegate sourcing runs to `JobSourcingReActAgentService`.

---

### Task 1: Add Auth Session Persistence

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/PlatformAuthSession.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/PlatformAuthSessionMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/enums/PlatformAuthStatus.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/enums/JobSourcingRunStatus.java`
- Modify: `backend/src/main/resources/sql/init.sql`
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingAgentTest.java`

- [ ] **Step 1: Write failing SQL assertions**

Add to `InitSqlJobSourcingAgentTest`:

```java
@Test
void initSql_shouldCreatePlatformAuthSessionTable() throws IOException {
    String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS platform_auth_session");
    assertThat(sql).contains("platform VARCHAR(64) NOT NULL");
    assertThat(sql).contains("status VARCHAR(32) NOT NULL DEFAULT 'not_authorized'");
    assertThat(sql).contains("profile_path VARCHAR(1024) NOT NULL DEFAULT ''");
    assertThat(sql).contains("UNIQUE KEY uk_platform (platform)");
}

@Test
void initSql_shouldDocumentAuthRequiredRunStatus() throws IOException {
    String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

    assertThat(sql).contains("running/success/partial_success/failed/auth_required");
}
```

- [ ] **Step 2: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=InitSqlJobSourcingAgentTest"`

Expected: FAIL because `platform_auth_session` is missing.

- [ ] **Step 3: Add SQL table and status comment**

Add after `job_crawl_item` in `init.sql`:

```sql
CREATE TABLE IF NOT EXISTS platform_auth_session (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台授权会话 id',
  platform VARCHAR(64) NOT NULL COMMENT '平台：boss/shixiseng',
  status VARCHAR(32) NOT NULL DEFAULT 'not_authorized' COMMENT 'not_authorized/authorized/expired/auth_required/error',
  profile_path VARCHAR(1024) NOT NULL DEFAULT '' COMMENT 'Playwright 持久化 profile 路径',
  last_verified_at DATETIME DEFAULT NULL COMMENT '最后验证时间',
  expires_hint_at DATETIME DEFAULT NULL COMMENT '预计过期时间',
  error_message VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '授权错误信息',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台授权会话表';
```

Change `job_crawl_run.status` comment to include `auth_required`.

- [ ] **Step 4: Add entity, mapper, enums**

Create entity:

```java
@Data
@TableName("platform_auth_session")
public class PlatformAuthSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String platform;
    private String status;
    private String profilePath;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime expiresHintAt;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Create mapper:

```java
@Mapper
public interface PlatformAuthSessionMapper extends BaseMapper<PlatformAuthSession> {
}
```

Create enums with `.getValue()` returning the lowercase string values from this task.

- [ ] **Step 5: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=InitSqlJobSourcingAgentTest"`

Expected: PASS.

---

### Task 2: Add Browser Session Service

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/mianshiba/ai/config/JobSourcingProperties.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/BrowserSessionService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/PlaywrightBrowserSessionServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/PlaywrightBrowserSessionServiceImplTest.java`

- [ ] **Step 1: Write failing service test**

Create a test asserting `startAuth("boss")` creates or updates a session with profile path ending in `backend/browser-profiles/boss` and status `auth_required`.

- [ ] **Step 2: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=PlaywrightBrowserSessionServiceImplTest"`

Expected: compilation FAIL because the service does not exist.

- [ ] **Step 3: Add dependency and properties**

Add Playwright dependency:

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.49.0</version>
</dependency>
```

Add config:

```yaml
app:
  job-sourcing:
    browser-profile-root: ${JOB_SOURCING_BROWSER_PROFILE_ROOT:backend/browser-profiles}
    browser-headless: ${JOB_SOURCING_BROWSER_HEADLESS:false}
    browser-timeout-millis: ${JOB_SOURCING_BROWSER_TIMEOUT_MILLIS:30000}
```

Add fields to `JobSourcingProperties`:

```java
private String browserProfileRoot = "backend/browser-profiles";
private boolean browserHeadless = false;
private int browserTimeoutMillis = 30000;
```

- [ ] **Step 4: Add service contract and implementation**

Service contract:

```java
public interface BrowserSessionService {
    AuthStartResult startAuth(String platform);
    AuthCheckResult checkAuth(String platform);
    record AuthStartResult(String platform, String profilePath) {}
    record AuthCheckResult(String platform, String status, String message) {}
}
```

Implementation must normalize platform to lowercase, persist the profile path, and return existing status from `platform_auth_session` for `checkAuth`.

- [ ] **Step 5: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=PlaywrightBrowserSessionServiceImplTest"`

Expected: PASS.

---

### Task 3: Add Platform Adapter Contracts and Registry

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/jobsourcing/JobDiscoveryRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/jobsourcing/JobListEntry.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/jobsourcing/FetchedJobPage.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/jobsourcing/ExtractedJobCard.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobPlatformAdapter.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobPlatformAdapterRegistry.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DefaultJobPlatformAdapterRegistry.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/DefaultJobPlatformAdapterRegistryTest.java`

- [ ] **Step 1: Write failing registry test**

Test that `getAdapter("BOSS")` returns the `boss` adapter and `getAdapter("unknown")` throws `IllegalArgumentException`.

- [ ] **Step 2: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=DefaultJobPlatformAdapterRegistryTest"`

Expected: compilation FAIL.

- [ ] **Step 3: Add DTO records**

Use records with these signatures:

```java
public record JobDiscoveryRequest(String platform, String keywords, String cities, String experienceLevels, int maxPages, int targetCount) {}
public record JobListEntry(String platform, String sourceUrl, String title, String companyName, String city, String salaryRange) {}
public record FetchedJobPage(String sourceUrl, String finalUrl, String title, String content, String html, String sourcePlatform, boolean requiresAuth) {}
public record ExtractedJobCard(String title, String companyName, String city, String salaryRange, String experienceRequirement, String educationRequirement, String jobDescription, String jobRequirement, String techStackJson, String summary, String tagsJson, int confidenceScore) {}
```

- [ ] **Step 4: Add adapter and registry**

Add `JobPlatformAdapter` with `platform`, `requiresAuth`, `checkAuth`, `discover`, `fetchDetail`, and `fallbackExtract` methods. Add registry implementation backed by a lowercase map.

- [ ] **Step 5: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=DefaultJobPlatformAdapterRegistryTest"`

Expected: PASS.

---

### Task 4: Add Boss Adapter with Fixture Extraction

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/BossJobPlatformAdapter.java`
- Create: `backend/src/test/resources/job-sourcing/boss-detail.html`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/BossJobPlatformAdapterTest.java`

- [ ] **Step 1: Create fixture**

Use a Boss-like HTML fixture containing title `Java 后端开发工程师`, company `示例科技`, city `深圳`, salary `20-35K`, experience `3-5年`, degree `本科`, and skills `Java`, `Spring Boot`, `MySQL`, `Redis`.

- [ ] **Step 2: Write failing tests**

Test `fallbackExtract` extracts those fields and `isAuthWall` detects text containing `登录`, `验证码`, or `安全验证`.

- [ ] **Step 3: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=BossJobPlatformAdapterTest"`

Expected: compilation FAIL.

- [ ] **Step 4: Implement adapter**

Implement `platform() = "boss"`, `requiresAuth() = true`, `checkAuth()` delegated to `BrowserSessionService`, auth-wall detection, and fixture-driven `fallbackExtract`.

- [ ] **Step 5: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=BossJobPlatformAdapterTest"`

Expected: PASS.

---

### Task 5: Add Shixiseng Adapter with Fixture Extraction

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ShixisengJobPlatformAdapter.java`
- Create: `backend/src/test/resources/job-sourcing/shixiseng-detail.html`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/ShixisengJobPlatformAdapterTest.java`

- [ ] **Step 1: Create fixture**

Use a Shixiseng-like HTML fixture containing title `后端开发实习生`, company `样例网络`, city `北京`, salary `200-300/天`, degree `本科`, weekly days `每周 4 天`, duration `实习 3 个月`, and skills `Java`, `Spring Boot`, `MySQL`.

- [ ] **Step 2: Write failing tests**

Test `fallbackExtract` extracts the internship fields and stores weekly days/duration in tags.

- [ ] **Step 3: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=ShixisengJobPlatformAdapterTest"`

Expected: compilation FAIL.

- [ ] **Step 4: Implement adapter**

Implement `platform() = "shixiseng"`, `requiresAuth() = false`, public-page fallback extraction, and auth-wall detection for pages that later require login.

- [ ] **Step 5: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=ShixisengJobPlatformAdapterTest"`

Expected: PASS.

---

### Task 6: Add Spring AI Job Sourcing Tools

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/tool/JobSourcingTools.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/tool/JobSourcingToolsTest.java`

- [ ] **Step 1: Write failing tool test**

Mock `JobPlatformAdapterRegistry`, call `discoverJobs("boss", "Java", "深圳", "3-5年", 2, 5)`, and assert it delegates to the Boss adapter.

- [ ] **Step 2: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=JobSourcingToolsTest"`

Expected: compilation FAIL.

- [ ] **Step 3: Implement tools**

Create `JobSourcingTools` with `@Tool` methods: `checkPlatformAuth`, `discoverJobs`, and `fetchJobDetail`. Each method must use `JobPlatformAdapterRegistry` and never issue arbitrary HTTP requests.

- [ ] **Step 4: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=JobSourcingToolsTest"`

Expected: PASS.

---

### Task 7: Implement ReAct Agent Service

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/JobSourcingReActAgentService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/JobSourcingReActAgentServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobSourcingReActAgentServiceImplTest.java`

- [ ] **Step 1: Write failing auth-required test**

Mock a Boss task and adapter returning `auth_required`. Assert run status `auth_required` and verify `JobCrawlItemMapper.insert` is never called.

- [ ] **Step 2: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=JobSourcingReActAgentServiceImplTest"`

Expected: compilation FAIL.

- [ ] **Step 3: Implement auth gate**

Create run, resolve task, resolve adapter, call `checkAuth`, and stop with `auth_required` before discovery if a required platform is not authorized.

- [ ] **Step 4: Add successful path test**

Mock adapter discovery returning one entry, detail fetch returning one page, fallback extraction returning one card. Assert one item becomes `extracted`, review status is `pending_review`, and run status is `success`.

- [ ] **Step 5: Implement successful path**

Loop through discovered entries up to `targetCount`, call `fetchDetail`, `fallbackExtract`, quality scoring, and save `job_crawl_item` with extracted JSON. Enforce `maxPagesPerSource` and `maxThirdPartyCalls` from `JobSourcingProperties`.

- [ ] **Step 6: Run green tests**

Run: `.\mvnw.cmd test -pl . "-Dtest=JobSourcingReActAgentServiceImplTest"`

Expected: PASS.

- [ ] **Step 7: Delegate batch crawl service**

Inject `JobSourcingReActAgentService` into `JobBatchCrawlServiceImpl` and use it for job sourcing tasks.

- [ ] **Step 8: Run regression tests**

Run: `.\mvnw.cmd test -pl . "-Dtest=JobSourcingReActAgentServiceImplTest,JobBatchCrawlServiceImplTest"`

Expected: PASS.

---

### Task 8: Add Admin Platform Auth Endpoints

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java`
- Test: existing admin job crawl controller/service tests or new `AdminJobCrawlControllerTest.java`

- [ ] **Step 1: Write failing endpoint tests**

Test `POST /api/admin/job-crawl/platforms/boss/auth/start` and `POST /api/admin/job-crawl/platforms/boss/auth/check` return `code=0` and platform fields.

- [ ] **Step 2: Run red test**

Run: `.\mvnw.cmd test -pl . "-Dtest=AdminJobCrawlControllerTest"`

Expected: FAIL with 404.

- [ ] **Step 3: Add service methods and controller endpoints**

Add `startPlatformAuth` and `checkPlatformAuth` to `AdminJobCrawlService`, delegate to `BrowserSessionService`, and expose the two controller routes.

- [ ] **Step 4: Run green test**

Run: `.\mvnw.cmd test -pl . "-Dtest=AdminJobCrawlControllerTest"`

Expected: PASS.

---

### Task 9: Add Live Tests

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/BossJobSourcingLiveTest.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ShixisengJobSourcingLiveTest.java`

- [ ] **Step 1: Add Boss live test**

Guard with `@EnabledIfEnvironmentVariable(named = "RUN_LIVE_BOSS_CRAWL", matches = "true")`. Use the authorized Boss profile. Assert at least one real job has nonblank title, company, city, sourceUrl, and content.

- [ ] **Step 2: Add Shixiseng live test**

Guard with `@EnabledIfEnvironmentVariable(named = "RUN_LIVE_SHIXISENG_CRAWL", matches = "true")`. Assert at least one real internship has nonblank title, company, city, sourceUrl, and content.

- [ ] **Step 3: Run focused unit tests**

Run: `.\mvnw.cmd test -pl . "-Dtest=JobSourcingReActAgentServiceImplTest,BossJobPlatformAdapterTest,ShixisengJobPlatformAdapterTest,PlaywrightBrowserSessionServiceImplTest,JobSourcingToolsTest,DefaultJobPlatformAdapterRegistryTest"`

Expected: PASS.

- [ ] **Step 4: Run Boss live test after authorization**

Run: `$env:RUN_LIVE_BOSS_CRAWL='true'; .\mvnw.cmd test -pl . "-Dtest=BossJobSourcingLiveTest"`

Expected: PASS. If authorization is missing or expired, re-authorize and rerun; do not claim Boss support until this passes.

- [ ] **Step 5: Run Shixiseng live test**

Run: `$env:RUN_LIVE_SHIXISENG_CRAWL='true'; .\mvnw.cmd test -pl . "-Dtest=ShixisengJobSourcingLiveTest"`

Expected: PASS. Do not claim Shixiseng support until this passes.

---

## Final Verification

- [ ] Run focused backend tests:

```powershell
.\mvnw.cmd test -pl . "-Dtest=JobSourcingReActAgentServiceImplTest,BossJobPlatformAdapterTest,ShixisengJobPlatformAdapterTest,PlaywrightBrowserSessionServiceImplTest,JobSourcingToolsTest,DefaultJobPlatformAdapterRegistryTest"
```

- [ ] Run Boss live verification:

```powershell
$env:RUN_LIVE_BOSS_CRAWL='true'; .\mvnw.cmd test -pl . "-Dtest=BossJobSourcingLiveTest"
```

- [ ] Run Shixiseng live verification:

```powershell
$env:RUN_LIVE_SHIXISENG_CRAWL='true'; .\mvnw.cmd test -pl . "-Dtest=ShixisengJobSourcingLiveTest"
```

- [ ] Only claim a platform works after its live verification command passes in the current session.

## Self-Review Notes

- Spec coverage: persistence, auth profile, platform tools, adapters, agent orchestration, admin auth endpoints, and live tests are covered.
- Incomplete-marker scan: none are present.
- Type consistency: DTO names and service names are introduced before later tasks use them.
- Scope check: implementation is large but sequenced into independently testable backend tasks.
