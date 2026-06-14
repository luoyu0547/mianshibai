# Job Sourcing Recommendation Application Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved job sourcing, AI recommendation, and application Kanban workflow so admins can curate public job sources and users can receive matched jobs and convert them into application plans.

**Architecture:** Add crawl task/run/item and recommendation persistence first, then implement admin crawl orchestration, recommendation APIs, and finally the Vue pages. Existing `JobCrawlService`, `JobParseService`, `AiJobAnalysisService`, `ResumeJobMatchService`, and `ApplicationService` remain the core integration points; new services wrap them instead of replacing them.

**Tech Stack:** Spring Boot 3.5.x, Java 17, Maven, MyBatis-Plus, MySQL, Vue 3, TypeScript, Vite, Pinia, Vue Router, Element Plus.

---

## Execution Notes

The current branch has one known backend test failure before this plan starts:

```text
./mvnw.cmd test
Failures: InterviewReportEnhancementServiceImplTest.runTask_shouldThrowIfNotFound
Expected BusinessException, but current runTask returns when task is absent.
```

Do not use the full backend test suite as the only signal until this existing failure is fixed or intentionally accepted. Each task below names targeted tests that should pass for its own scope.

## File Structure Map

### Backend Persistence

- Create `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlTask.java`: admin crawl task entity.
- Create `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlRun.java`: crawl run entity.
- Create `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlItem.java`: per-URL crawl item entity.
- Create `backend/src/main/java/com/mianshiba/ai/model/entity/JobRecommendation.java`: user recommendation entity.
- Create mapper interfaces under `backend/src/main/java/com/mianshiba/ai/mapper/` for the four new entities.
- Modify `backend/src/main/resources/sql/init.sql`: add tables and columns.

### Backend DTO/VO

- Create `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlTaskCreateRequest.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlTaskUpdateRequest.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/AdminJobCrawlTaskQueryRequest.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobRecommendationRefineRequest.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlTaskVO.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlRunVO.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl/AdminJobCrawlItemVO.java`.
- Create `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobRecommendationVO.java`.

### Backend Services

- Create `backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java` and `impl/AdminJobCrawlServiceImpl.java`.
- Create `backend/src/main/java/com/mianshiba/ai/service/JobBatchCrawlService.java` and `impl/JobBatchCrawlServiceImpl.java`.
- Create `backend/src/main/java/com/mianshiba/ai/service/JobDedupService.java` and `impl/JobDedupServiceImpl.java`.
- Create `backend/src/main/java/com/mianshiba/ai/service/JobRecommendationService.java` and `impl/JobRecommendationServiceImpl.java`.
- Create `backend/src/main/java/com/mianshiba/ai/scheduler/JobCrawlScheduler.java`.
- Modify `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java`: support recommendation-created applications.
- Modify `backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java`: list visible job library instead of favorites-only list.

### Backend Controllers

- Create `backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java`.
- Modify `backend/src/main/java/com/mianshiba/ai/controller/JobController.java`: add recommendation endpoints.

### Frontend API/Types/Stores

- Create `frontend/src/types/jobCrawl.ts`.
- Modify `frontend/src/types/job.ts`: add recommendation types and list filters.
- Modify `frontend/src/types/application.ts`: add `recommendationId`.
- Create `frontend/src/api/jobCrawl.ts`.
- Modify `frontend/src/api/job.ts`: add recommendation endpoints.
- Create `frontend/src/stores/jobCrawl.ts`.
- Modify `frontend/src/stores/job.ts`: add recommendation state/actions.
- Modify `frontend/src/stores/application.ts`: support recommendation-created flow.

### Frontend Pages

- Create `frontend/src/views/admin/AdminJobCrawlListPage.vue`.
- Create `frontend/src/views/admin/AdminJobCrawlEditPage.vue`.
- Create `frontend/src/views/admin/AdminJobCrawlDetailPage.vue`.
- Create `frontend/src/views/job/JobRecommendationPage.vue`.
- Modify `frontend/src/views/job/JobImportPage.vue`: rename copy to manual supplement.
- Modify `frontend/src/views/job/JobListPage.vue`: show job library filters.
- Modify `frontend/src/views/application/ApplicationListPage.vue`: convert main view to Kanban.
- Modify `frontend/src/router/index.ts`: add routes.
- Modify `frontend/src/layouts/MainLayout.vue` and `frontend/src/layouts/AdminLayout.vue`: update navigation.

---

### Task 0: Fix Or Quarantine Existing Backend Test Failure

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImplTest.java`

- [ ] **Step 1: Reproduce the existing failure**

Run:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportEnhancementServiceImplTest#runTask_shouldThrowIfNotFound
```

Expected: FAIL with `Expecting code to raise a throwable`.

- [ ] **Step 2: Decide the intended behavior**

Use this behavior to align the current test and implementation: `runTask` is worker-facing and should skip missing/completed tasks instead of throwing. This matches the implementation and avoids poison queue retries for already-deleted tasks.

- [ ] **Step 3: Update the test name and assertion**

Replace the failing test with:

```java
@Test
void runTask_shouldSkipIfNotFound() {
    when(enhancementMapper.selectById(999L)).thenReturn(null);

    enhancementService.runTask(999L);

    verify(enhancementMapper).selectById(999L);
    verify(enhancementMapper, org.mockito.Mockito.never())
            .updateById(ArgumentMatchers.any(InterviewReportEnhancement.class));
}
```

- [ ] **Step 4: Run the targeted test**

Run:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportEnhancementServiceImplTest#runTask_shouldSkipIfNotFound
```

Expected: PASS.

- [ ] **Step 5: Run backend tests**

Run:

```powershell
.\mvnw.cmd test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImplTest.java
git commit -m "test: align enhancement worker missing task behavior"
```

---

### Task 1: Add Crawl And Recommendation Schema

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlTask.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlRun.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobCrawlItem.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobRecommendation.java`
- Create: mapper interfaces for each entity
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingTest.java`

- [ ] **Step 1: Write SQL structure test**

Create `InitSqlJobSourcingTest.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlJobSourcingTest {

    @Test
    void initSqlContainsJobSourcingTablesAndColumns() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_crawl_task");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_crawl_run");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_crawl_item");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_recommendation");
        assertThat(sql).contains("crawl_task_id BIGINT");
        assertThat(sql).contains("recommendation_id BIGINT");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\mvnw.cmd test -Dtest=InitSqlJobSourcingTest
```

Expected: FAIL because new tables are absent.

- [ ] **Step 3: Add SQL tables and columns**

Append to `init.sql` after existing job/application tables:

```sql
ALTER TABLE job
  ADD COLUMN crawl_task_id BIGINT DEFAULT NULL COMMENT '来源采集任务 id',
  ADD COLUMN crawl_run_id BIGINT DEFAULT NULL COMMENT '来源采集运行 id',
  ADD COLUMN normalized_fingerprint VARCHAR(255) NOT NULL DEFAULT '' COMMENT '去重指纹',
  ADD COLUMN last_seen_at DATETIME DEFAULT NULL COMMENT '最近采集到时间',
  ADD COLUMN expire_checked_at DATETIME DEFAULT NULL COMMENT '最近过期检查时间',
  ADD COLUMN quality_score INT NOT NULL DEFAULT 0 COMMENT '职位质量分';

ALTER TABLE job_application
  ADD COLUMN recommendation_id BIGINT DEFAULT NULL COMMENT '来源推荐 id';

CREATE TABLE IF NOT EXISTS job_crawl_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '采集任务 id',
  name VARCHAR(128) NOT NULL COMMENT '任务名称',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  source_url VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '来源 URL',
  config_json JSON DEFAULT NULL COMMENT '任务扩展配置',
  keywords VARCHAR(512) NOT NULL DEFAULT '' COMMENT '关键词条件',
  cities VARCHAR(512) NOT NULL DEFAULT '' COMMENT '城市条件',
  experience_levels VARCHAR(512) NOT NULL DEFAULT '' COMMENT '经验条件',
  schedule_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '调度类型',
  cron_expression VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'cron 表达式',
  status VARCHAR(32) NOT NULL DEFAULT 'disabled' COMMENT 'enabled/disabled',
  last_run_at DATETIME DEFAULT NULL COMMENT '上次运行时间',
  next_run_at DATETIME DEFAULT NULL COMMENT '下次运行时间',
  created_by BIGINT NOT NULL COMMENT '创建管理员 id',
  remark VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_status_next_run (status, next_run_at),
  KEY idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位采集任务表';
```

Add analogous `job_crawl_run`, `job_crawl_item`, and `job_recommendation` tables using the fields from the approved spec.

- [ ] **Step 4: Create entities**

Use this pattern for `JobCrawlTask`; create the other three entities with matching fields:

```java
@Data
@TableName("job_crawl_task")
public class JobCrawlTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String sourceType;
    private String sourceUrl;
    private Object configJson;
    private String keywords;
    private String cities;
    private String experienceLevels;
    private String scheduleType;
    private String cronExpression;
    private String status;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Long createdBy;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDelete;
}
```

- [ ] **Step 5: Create mapper interfaces**

```java
@Mapper
public interface JobCrawlTaskMapper extends BaseMapper<JobCrawlTask> {
}
```

Repeat for `JobCrawlRunMapper`, `JobCrawlItemMapper`, and `JobRecommendationMapper`.

- [ ] **Step 6: Run SQL and context tests**

Run:

```powershell
.\mvnw.cmd test -Dtest=InitSqlJobSourcingTest,MianshibaAiBackendApplicationTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/model/entity backend/src/main/java/com/mianshiba/ai/mapper backend/src/test/java/com/mianshiba/ai/sql/InitSqlJobSourcingTest.java
git commit -m "feat: add job sourcing persistence"
```

---

### Task 2: Implement Admin Crawl Task CRUD

**Files:**
- Create DTO/VO files under `backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl/` and `model/vo/admin/jobcrawl/`
- Create: `backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImplTest.java`

- [ ] **Step 1: Write service tests**

Test cases:

```java
@Test
void createTask_shouldRequireAdminAndPersistDisabledTask() { }

@Test
void enableTask_shouldSetEnabledAndNextRunAt() { }

@Test
void disableTask_shouldSetDisabled() { }

@Test
void createTask_shouldRejectInvalidSourceType() { }
```

Use `jwtUtils.resolveToken`, `jwtUtils.parseToken`, and `userMapper.selectById` mocks matching `AdminServiceImplTest`.

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
.\mvnw.cmd test -Dtest=AdminJobCrawlServiceImplTest
```

Expected: compilation failure because service classes do not exist.

- [ ] **Step 3: Implement DTOs and VO**

`AdminJobCrawlTaskCreateRequest` fields:

```java
@NotBlank
private String name;
@NotBlank
private String sourceType;
private String sourceUrl;
private String configJson;
private String keywords;
private String cities;
private String experienceLevels;
private String scheduleType;
private String cronExpression;
private String remark;
```

`AdminJobCrawlTaskVO` fields mirror `JobCrawlTask` plus run count fields: `latestStatus`, `successCount`, `failedCount`.

- [ ] **Step 4: Implement service validation**

Allowed source types:

```java
private static final Set<String> SOURCE_TYPES = Set.of(
        "company_career_page", "public_feed", "manual_url_list", "platform_entry_url");
```

Allowed schedule types:

```java
private static final Set<String> SCHEDULE_TYPES = Set.of("manual", "daily", "weekly", "cron");
```

Reject invalid values with `new BusinessException(ErrorCode.PARAMS_ERROR)`.

- [ ] **Step 5: Implement admin identity helper**

```java
private Long resolveAdminId(String authorizationHeader) {
    String token = jwtUtils.resolveToken(authorizationHeader);
    JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
    User user = userMapper.selectById(claims.userId());
    if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
    if (!"admin".equals(user.getUserRole())) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }
    return user.getId();
}
```

- [ ] **Step 6: Add controller endpoints**

Use `/api/admin/job-crawl` prefix and methods from the spec:

```java
@PostMapping("/tasks")
public BaseResponse<AdminJobCrawlTaskVO> createTask(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @Valid @RequestBody AdminJobCrawlTaskCreateRequest request) {
    return ResultUtils.success(adminJobCrawlService.createTask(authorizationHeader, request));
}
```

- [ ] **Step 7: Run service and controller tests**

Run:

```powershell
.\mvnw.cmd test -Dtest=AdminJobCrawlServiceImplTest,AdminJobCrawlControllerTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/admin/jobcrawl backend/src/main/java/com/mianshiba/ai/model/vo/admin/jobcrawl backend/src/main/java/com/mianshiba/ai/service/AdminJobCrawlService.java backend/src/main/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/AdminJobCrawlController.java backend/src/test/java/com/mianshiba/ai/service/impl/AdminJobCrawlServiceImplTest.java
git commit -m "feat: add admin job crawl task management"
```

---

### Task 3: Implement Batch Crawl, Dedup, Runs, And Scheduler

**Files:**
- Create: `JobBatchCrawlService` and implementation
- Create: `JobDedupService` and implementation
- Create: `backend/src/main/java/com/mianshiba/ai/scheduler/JobCrawlScheduler.java`
- Modify: `AdminJobCrawlServiceImpl` to call batch run service
- Test: `JobDedupServiceImplTest`, `JobBatchCrawlServiceImplTest`

- [ ] **Step 1: Write dedup tests**

```java
@Test
void findDuplicate_shouldMatchByNormalizedUrl() { }

@Test
void buildFingerprint_shouldUseCompanyTitleAndCity() { }
```

Expected fingerprint format:

```text
normalizedCompany|normalizedTitle|normalizedCity
```

- [ ] **Step 2: Implement `JobDedupService`**

Interface:

```java
public interface JobDedupService {
    String normalizeUrl(String url);
    String buildFingerprint(Job job);
    Job findDuplicate(String normalizedUrl, Job parsedJob);
}
```

Implementation should lowercase, trim query tracking params, and compare `sourceUrl`, `normalizedFingerprint`, and company/title/city.

- [ ] **Step 3: Write batch crawl tests**

```java
@Test
void runTask_shouldContinueWhenOneItemFails() { }

@Test
void runTask_shouldInsertJobForSuccessfulNewItem() { }

@Test
void runTask_shouldMarkDuplicateWithoutInsertingJob() { }
```

- [ ] **Step 4: Implement `JobBatchCrawlService.runTask(Long taskId)`**

Required flow:

```text
load task -> create JobCrawlRun(running) -> expand urls -> for each url:
  create JobCrawlItem
  crawl url
  parse job
  dedup
  insert or update job
  analyze new job
  update item status
finish run with success/partial_success/failed counts
```

- [ ] **Step 5: Implement URL expansion**

For first implementation:

```java
private List<String> resolveUrls(JobCrawlTask task) {
    if ("manual_url_list".equals(task.getSourceType())) {
        return Arrays.stream(nullToEmpty(task.getConfigJson()).split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
    return List.of(task.getSourceUrl());
}
```

- [ ] **Step 6: Implement scheduler**

```java
@Scheduled(fixedDelay = 60000)
public void runDueTasks() {
    List<JobCrawlTask> tasks = taskMapper.selectList(Wrappers.lambdaQuery(JobCrawlTask.class)
            .eq(JobCrawlTask::getStatus, "enabled")
            .le(JobCrawlTask::getNextRunAt, LocalDateTime.now()));
    for (JobCrawlTask task : tasks) {
        jobBatchCrawlService.runTask(task.getId());
    }
}
```

- [ ] **Step 7: Run targeted tests**

Run:

```powershell
.\mvnw.cmd test -Dtest=JobDedupServiceImplTest,JobBatchCrawlServiceImplTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/JobBatchCrawlService.java backend/src/main/java/com/mianshiba/ai/service/JobDedupService.java backend/src/main/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/JobDedupServiceImpl.java backend/src/main/java/com/mianshiba/ai/scheduler/JobCrawlScheduler.java backend/src/test/java/com/mianshiba/ai/service/impl/JobDedupServiceImplTest.java backend/src/test/java/com/mianshiba/ai/service/impl/JobBatchCrawlServiceImplTest.java
git commit -m "feat: add scheduled job batch crawling"
```

---

### Task 4: Implement Recommendation APIs And Apply-To-Application Flow

**Files:**
- Create: `JobRecommendationService` and implementation
- Create: `JobRecommendationRefineRequest.java`
- Create: `JobRecommendationVO.java`
- Modify: `JobController.java`
- Modify: `ApplicationServiceImpl.java`
- Test: `JobRecommendationServiceImplTest`, `JobControllerTest`

- [ ] **Step 1: Write recommendation service tests**

Test cases:

```java
@Test
void listRecommendations_shouldExcludeDismissedAndApplied() { }

@Test
void refine_shouldCallResumeJobMatchAndPersistMatchId() { }

@Test
void dismiss_shouldMarkDismissedForCurrentUserOnly() { }

@Test
void apply_shouldCreatePendingSubmitApplicationAndDefaultTodos() { }

@Test
void apply_shouldReturnExistingApplicationWhenDuplicateJobExists() { }
```

- [ ] **Step 2: Implement service interface**

```java
public interface JobRecommendationService {
    List<JobRecommendationVO> listRecommendations(String authorizationHeader);
    List<JobRecommendationVO> refine(String authorizationHeader, JobRecommendationRefineRequest request);
    void dismiss(String authorizationHeader, Long recommendationId);
    JobApplicationVO apply(String authorizationHeader, Long recommendationId);
}
```

- [ ] **Step 3: Implement rough score generation helper**

Use deterministic scoring first:

```java
private int calculateRoughScore(Job job) {
    int score = 50;
    if (StringUtils.hasText(job.getSalaryRange())) score += 10;
    if (StringUtils.hasText(job.getTechStack())) score += 15;
    if (StringUtils.hasText(job.getJobRequirement())) score += 15;
    if (StringUtils.hasText(job.getCity())) score += 10;
    return Math.min(score, 100);
}
```

- [ ] **Step 4: Implement apply flow**

When applying, create `ApplicationCreateRequest` equivalent values:

```java
app.setUserId(userId);
app.setJobId(job.getId());
app.setRecommendationId(recommendation.getId());
app.setCompanyName(job.getCompanyName());
app.setJobTitle(job.getTitle());
app.setSource(job.getSourcePlatform());
app.setStatus("pending_submit");
app.setSalaryRange(job.getSalaryRange());
app.setLocation(job.getCity());
```

Create three todos: `根据 JD 优化简历`, `准备核心技能题`, `投递后 3 天跟进`.

- [ ] **Step 5: Add controller endpoints**

Add to `JobController`:

```java
@GetMapping("/recommendations")
public BaseResponse<List<JobRecommendationVO>> listRecommendations(...) { }

@PostMapping("/recommendations/refine")
public BaseResponse<List<JobRecommendationVO>> refineRecommendations(...) { }

@PutMapping("/recommendations/{id}/dismiss")
public BaseResponse<Void> dismissRecommendation(...) { }

@PostMapping("/recommendations/{id}/apply")
public BaseResponse<JobApplicationVO> applyRecommendation(...) { }
```

- [ ] **Step 6: Run targeted tests**

Run:

```powershell
.\mvnw.cmd test -Dtest=JobRecommendationServiceImplTest,JobControllerTest,ApplicationServiceImplTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/JobRecommendationService.java backend/src/main/java/com/mianshiba/ai/service/impl/JobRecommendationServiceImpl.java backend/src/main/java/com/mianshiba/ai/model/dto/job/JobRecommendationRefineRequest.java backend/src/main/java/com/mianshiba/ai/model/vo/job/JobRecommendationVO.java backend/src/main/java/com/mianshiba/ai/controller/JobController.java backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/JobRecommendationServiceImplTest.java
git commit -m "feat: add job recommendation workflow"
```

---

### Task 5: Build Admin Crawl Frontend

**Files:**
- Create: `frontend/src/types/jobCrawl.ts`
- Create: `frontend/src/api/jobCrawl.ts`
- Create: `frontend/src/stores/jobCrawl.ts`
- Create: admin crawl pages
- Modify: `frontend/src/router/index.ts`, `frontend/src/layouts/AdminLayout.vue`

- [ ] **Step 1: Add frontend types**

```ts
export type JobCrawlSourceType = 'company_career_page' | 'public_feed' | 'manual_url_list' | 'platform_entry_url'
export type JobCrawlScheduleType = 'manual' | 'daily' | 'weekly' | 'cron'
export type JobCrawlTaskStatus = 'enabled' | 'disabled'

export interface AdminJobCrawlTaskVO {
  id: number
  name: string
  sourceType: JobCrawlSourceType
  sourceUrl: string
  keywords: string
  cities: string
  scheduleType: JobCrawlScheduleType
  status: JobCrawlTaskStatus
  lastRunAt?: string | null
  nextRunAt?: string | null
  successCount?: number
  failedCount?: number
}
```

- [ ] **Step 2: Add API client**

```ts
export function listJobCrawlTasks(params?: AdminJobCrawlTaskQueryRequest) {
  return request.get<BaseResponse<AdminJobCrawlTaskVO[]>>('/api/admin/job-crawl/tasks', { params })
}

export function runJobCrawlTask(id: number) {
  return request.post<BaseResponse<AdminJobCrawlRunVO>>(`/api/admin/job-crawl/tasks/${id}/run`)
}
```

- [ ] **Step 3: Add Pinia store**

```ts
export const useJobCrawlStore = defineStore('jobCrawl', () => {
  const tasks = ref<AdminJobCrawlTaskVO[]>([])
  const loading = ref(false)
  async function fetchTasks() {
    loading.value = true
    try {
      const res = await listJobCrawlTasks()
      tasks.value = res.data
    } finally {
      loading.value = false
    }
  }
  return { tasks, loading, fetchTasks }
})
```

- [ ] **Step 4: Create list page**

Implement `AdminJobCrawlListPage.vue` with `NbPageHeader`, `NbCard`, `NbStatusBadge`, task table, enable/disable buttons, and run button.

- [ ] **Step 5: Create edit page**

Implement source-type conditional fields: URL for all except manual list can be optional; textarea for `manual_url_list`; cron expression only for `cron` schedule.

- [ ] **Step 6: Add routes and navigation**

Add routes:

```ts
{ path: '/admin/job-crawl', name: 'AdminJobCrawl', component: () => import('@/views/admin/AdminJobCrawlListPage.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
```

- [ ] **Step 7: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/types/jobCrawl.ts frontend/src/api/jobCrawl.ts frontend/src/stores/jobCrawl.ts frontend/src/views/admin/AdminJobCrawlListPage.vue frontend/src/views/admin/AdminJobCrawlEditPage.vue frontend/src/views/admin/AdminJobCrawlDetailPage.vue frontend/src/router/index.ts frontend/src/layouts/AdminLayout.vue
git commit -m "feat: add admin job crawl frontend"
```

---

### Task 6: Build User Recommendation Page And Job Library Filters

**Files:**
- Modify: `frontend/src/types/job.ts`
- Modify: `frontend/src/api/job.ts`
- Modify: `frontend/src/stores/job.ts`
- Create: `frontend/src/views/job/JobRecommendationPage.vue`
- Modify: `frontend/src/views/job/JobListPage.vue`
- Modify: `frontend/src/views/job/JobImportPage.vue`
- Modify: `frontend/src/router/index.ts`, `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Add recommendation types**

```ts
export interface JobRecommendationVO {
  id: number
  job: JobVO
  stage: 'rough' | 'refined'
  roughScore: number
  matchResult?: JobMatchVO | null
  recommendation: string
  reason: string
  riskPoints: string[]
  actionSuggestions: string[]
  dismissed: boolean
  applied: boolean
}
```

- [ ] **Step 2: Add API functions**

```ts
export function listJobRecommendations() {
  return request.get<BaseResponse<JobRecommendationVO[]>>('/api/job/recommendations')
}

export function applyJobRecommendation(id: number) {
  return request.post<BaseResponse<JobApplicationVO>>(`/api/job/recommendations/${id}/apply`)
}
```

- [ ] **Step 3: Add store actions**

```ts
const recommendations = ref<JobRecommendationVO[]>([])
async function fetchRecommendations() {
  const res = await listJobRecommendations()
  recommendations.value = res.data
}
```

- [ ] **Step 4: Create recommendation page**

Card content must include title, company, city, salary, score, reason, risk points, action suggestions, and buttons: `加入投递计划`, `查看详情`, `不感兴趣`.

- [ ] **Step 5: Update job import copy**

Change title from `导入职位` to `手动补充职位`. Change description to `当系统推荐之外还有目标职位时，可手动粘贴链接补充。`

- [ ] **Step 6: Update navigation**

Main nav `职位情报` should point to `/job/recommendations`.

- [ ] **Step 7: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/types/job.ts frontend/src/api/job.ts frontend/src/stores/job.ts frontend/src/views/job/JobRecommendationPage.vue frontend/src/views/job/JobListPage.vue frontend/src/views/job/JobImportPage.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue
git commit -m "feat: add user job recommendations"
```

---

### Task 7: Convert Application List To Kanban

**Files:**
- Modify: `frontend/src/views/application/ApplicationListPage.vue`
- Modify: `frontend/src/types/application.ts`
- Modify: `frontend/src/stores/application.ts`
- Optional create: `frontend/src/components/application/ApplicationKanbanColumn.vue`
- Optional create: `frontend/src/components/application/ApplicationKanbanCard.vue`

- [ ] **Step 1: Define Kanban groups**

```ts
const columns = [
  { key: 'pending_submit', title: '待投递', statuses: ['pending_submit'] },
  { key: 'submitted', title: '已投递', statuses: ['submitted'] },
  { key: 'contacting', title: '沟通中', statuses: ['hr_contact'] },
  { key: 'interviewing', title: '笔试/面试', statuses: ['written_test', 'first_interview', 'second_interview', 'final_interview'] },
  { key: 'offer', title: 'Offer', statuses: ['offer'] },
  { key: 'closed', title: '已关闭', statuses: ['rejected', 'withdrawn'] },
] as const
```

- [ ] **Step 2: Render grouped cards**

Use computed grouping:

```ts
function appsForColumn(statuses: readonly ApplicationStatus[]) {
  return applicationStore.applications.filter((item) => statuses.includes(item.status))
}
```

- [ ] **Step 3: Add quick advance action**

Use mapping:

```ts
const nextStatusMap: Partial<Record<ApplicationStatus, ApplicationStatus>> = {
  pending_submit: 'submitted',
  submitted: 'hr_contact',
  hr_contact: 'written_test',
  written_test: 'first_interview',
  first_interview: 'second_interview',
  second_interview: 'final_interview',
  final_interview: 'offer',
}
```

- [ ] **Step 4: Preserve filters and empty state**

Keep keyword/status filters at the top. If no applications exist, keep current `NbEmptyState` action to `/applications/new`.

- [ ] **Step 5: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/views/application/ApplicationListPage.vue frontend/src/types/application.ts frontend/src/stores/application.ts frontend/src/components/application
git commit -m "feat: convert applications to kanban"
```

---

### Task 8: Final Integration Verification

**Files:**
- Modify docs only if implementation changes the plan/spec.

- [ ] **Step 1: Run backend targeted suites**

Run:

```powershell
.\mvnw.cmd test -Dtest=AdminJobCrawlServiceImplTest,JobDedupServiceImplTest,JobBatchCrawlServiceImplTest,JobRecommendationServiceImplTest,ApplicationServiceImplTest
```

Expected: PASS.

- [ ] **Step 2: Run full backend tests**

Run:

```powershell
.\mvnw.cmd test
```

Expected: PASS after Task 0 resolves the existing enhancement test mismatch.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 4: Manual smoke path**

Verify manually:

```text
admin login -> /admin/job-crawl -> create manual_url_list task -> run task -> inspect run
user login -> /job/recommendations -> refine -> apply recommendation -> /applications Kanban shows pending card
```

- [ ] **Step 5: Commit verification docs if changed**

```powershell
git status --short
git add docs/superpowers/plans/2026-06-14-job-sourcing-recommendation-application-redesign-plan.md
git commit -m "docs: add job sourcing implementation plan"
```

## Self-Review

- Spec coverage: tasks cover schema, admin crawl, batch crawl, dedup, scheduler, recommendation APIs, apply-to-application, recommendation page, job library copy, Kanban, and verification.
- Scope choice: implementation remains phased and avoids automatic application submission, anti-bot bypass, notification systems, and distributed queues.
- Type consistency: source types, schedule types, application statuses, and recommendation endpoint names match the approved design.
- Known gap: complete `public_feed` parsing can be added after `manual_url_list` and `company_career_page`; Task 3 keeps URL expansion minimal for first implementation.
