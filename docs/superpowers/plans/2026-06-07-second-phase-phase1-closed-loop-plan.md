# Second Phase Phase 1 Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Phase 1 closed-loop flow: import resume, analyze job fit, optimize resume for a target job, run job-targeted timed interviews, and show a basic analytics dashboard.

**Architecture:** Reuse the existing Spring Boot layered backend and Vue 3 frontend patterns. Add small, focused service methods around the existing `resume`, `job`, `interview`, and `statistics` modules instead of introducing a new workflow engine. Persist only data needed for user-visible features and derive analytics from existing tables where possible.

**Tech Stack:** Java 17, Spring Boot 3.5.x, MyBatis-Plus, Spring AI ChatClient, MySQL JSON columns, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, Vite.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql` — add fields for resume import source, job status, job keywords, interview timing, and skill gap summary.
- Modify: `backend/src/main/java/com/mianshiba/ai/common/ErrorCode.java` — add upload/parse/analytics errors.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumeImportRequest.java` — request for uploaded resume text fallback.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumeWholeOptimizeRequest.java` — request for whole-resume optimization.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeImportPreviewVO.java` — AI parsed resume preview.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeWholeOptimizeVO.java` — whole-resume optimization suggestions.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java` — add import and whole optimize methods.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java` — implement import and whole optimize prompts.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java` — add resume import and whole optimize endpoints.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiImportServiceImplTest.java` — service tests for parsing and whole optimize.
- Create: `backend/src/test/java/com/mianshiba/ai/controller/ResumeAiImportControllerTest.java` — endpoint tests.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobListQueryRequest.java` — job list filters.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobKeywordVO.java` — JD keyword extraction output.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobGapAnalysisVO.java` — resume/job gap output.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobQuestionPredictionVO.java` — predicted interview questions.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/JobService.java` — add list, keyword, gap, predicted question methods.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java` — implement job list and orchestration.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/JobController.java` — add job list, keyword, gap, predicted questions endpoints.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/JobPhase1ServiceImplTest.java` — service tests.
- Create: `backend/src/test/java/com/mianshiba/ai/controller/JobPhase1ControllerTest.java` — endpoint tests.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/interview/InterviewCreateRequest.java` — ensure `jobId`, `interviewType`, `difficulty`, and `durationMinutes` are present.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewSession.java` — persist interview type, difficulty, duration and timing metadata.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewSessionVO.java` — return timing metadata.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java` — generate job-targeted questions and timing fields.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java` — reuse existing endpoints with richer request/response.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewPhase1ServiceImplTest.java` — timing and job-targeted interview tests.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/HomeStatsVO.java` — extend only if the home card needs Phase 1 data.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/AnalyticsOverviewVO.java` — analytics dashboard data.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java` — add analytics overview method.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java` — aggregate resume/job/interview data.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java` — add `/api/statistics/analytics/overview` endpoint.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/StatisticsPhase1ServiceImplTest.java` — analytics aggregation tests.

### Frontend Files

- Modify: `frontend/src/types/resume.ts` — add resume import and whole optimize types.
- Modify: `frontend/src/api/resume.ts` — add import and whole optimize API functions.
- Modify: `frontend/src/stores/resume.ts` — add actions for import preview and whole optimize.
- Create: `frontend/src/components/resume/ResumeImportDialog.vue` — upload/paste resume import dialog.
- Create: `frontend/src/components/resume/WholeResumeOptimizeDialog.vue` — whole-resume optimization dialog.
- Modify: `frontend/src/views/resume/ResumeListPage.vue` — add import entry.
- Modify: `frontend/src/views/resume/ResumeEditPage.vue` — add whole optimize entry.
- Modify: `frontend/src/types/job.ts` — add job list, keyword, gap and predicted question types.
- Modify: `frontend/src/api/job.ts` — add list, keywords, gap, questions API functions.
- Modify: `frontend/src/stores/job.ts` — add list, keyword, gap and questions actions.
- Create: `frontend/src/views/job/JobListPage.vue` — job list/search/filter page.
- Create: `frontend/src/views/job/JobQuestionsPage.vue` — predicted questions page.
- Modify: `frontend/src/views/job/JobDetailPage.vue` — add gap analysis and predicted question entry.
- Modify: `frontend/src/router/index.ts` — add `/job/list` and `/job/:id/questions` routes.
- Modify: `frontend/src/types/interview.ts` — add interview type, difficulty and timing fields.
- Modify: `frontend/src/views/interview/InterviewNewPage.vue` — add job, type, duration and difficulty inputs.
- Modify: `frontend/src/views/interview/InterviewRoomPage.vue` — add timer and progress UI.
- Modify: `frontend/src/types/statistics.ts` — extend with analytics overview types.
- Modify: `frontend/src/api/statistics.ts` — add analytics overview API.
- Create: `frontend/src/views/analytics/AnalyticsOverviewPage.vue` — basic dashboard page.
- Modify: `frontend/src/router/index.ts` — add `/analytics` route.

---

## Task 1: Backend Schema And Error Codes

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/common/ErrorCode.java`

- [ ] **Step 1: Add a migration-oriented SQL block to `init.sql`**

Add idempotent columns where MySQL supports them in the project environment. If `ADD COLUMN IF NOT EXISTS` is not supported by the target MySQL version, split these into deployment migrations before production.

```sql
ALTER TABLE resume
    ADD COLUMN import_source varchar(32) DEFAULT NULL COMMENT '导入来源：manual/pdf/docx/text';

ALTER TABLE job
    ADD COLUMN application_status varchar(32) NOT NULL DEFAULT 'favorite' COMMENT '投递状态：favorite/preparing/applied/interviewing/rejected/offer',
    ADD COLUMN keywords_json json DEFAULT NULL COMMENT 'JD关键词分析',
    ADD COLUMN predicted_questions_json json DEFAULT NULL COMMENT '预测面试题';

ALTER TABLE job_match
    ADD COLUMN keyword_coverage int DEFAULT NULL COMMENT '关键词覆盖率 0-100';

ALTER TABLE interview_session
    ADD COLUMN interview_type varchar(32) NOT NULL DEFAULT 'technical' COMMENT 'technical/project/hr/system_design',
    ADD COLUMN difficulty varchar(32) NOT NULL DEFAULT 'medium' COMMENT 'easy/medium/hard',
    ADD COLUMN duration_minutes int DEFAULT NULL COMMENT '计划面试时长',
    ADD COLUMN started_at datetime DEFAULT NULL COMMENT '开始时间',
    ADD COLUMN ended_at datetime DEFAULT NULL COMMENT '结束时间';
```

- [ ] **Step 2: Add new error codes**

In `ErrorCode.java`, add constants using unused code ranges:

```java
RESUME_IMPORT_ERROR(50003, "简历导入解析失败"),
RESUME_OPTIMIZE_ERROR(50004, "整份简历优化失败"),
JOB_ANALYSIS_ERROR(50023, "职位分析失败"),
ANALYTICS_ERROR(50030, "数据分析失败"),
```

- [ ] **Step 3: Run backend tests for compilation**

Run in `backend/`:

```powershell
.\mvnw.cmd test -DskipTests=false
```

Expected: compilation succeeds. Existing unrelated test failures must be documented before continuing.

- [ ] **Step 4: Commit**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/common/ErrorCode.java
git commit -m "feat: add phase one schema fields"
```

## Task 2: Resume Import And Whole Optimize Backend

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumeImportRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumeWholeOptimizeRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeImportPreviewVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeWholeOptimizeVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiImportServiceImplTest.java`
- Test: `backend/src/test/java/com/mianshiba/ai/controller/ResumeAiImportControllerTest.java`

- [ ] **Step 1: Write service tests first**

Test cases:

```java
@Test
void importResumePreview_shouldRejectBlankText() {
    ResumeImportRequest request = new ResumeImportRequest();
    request.setRawText(" ");

    assertThrows(BusinessException.class, () -> resumeAiService.importResumePreview(request, 1L));
}

@Test
void optimizeWholeResume_shouldRejectMissingResumeId() {
    ResumeWholeOptimizeRequest request = new ResumeWholeOptimizeRequest();
    request.setJobId(1L);

    assertThrows(BusinessException.class, () -> resumeAiService.optimizeWholeResume(request, 1L));
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=ResumeAiImportServiceImplTest
```

Expected: fail because DTOs and service methods do not exist.

- [ ] **Step 3: Add DTO and VO classes**

`ResumeImportRequest` fields:

```java
private String fileName;
private String fileType;
private String rawText;
```

`ResumeWholeOptimizeRequest` fields:

```java
private Long resumeId;
private Long jobId;
private String targetPosition;
private String optimizeGoal;
```

`ResumeImportPreviewVO` fields:

```java
private String title;
private String templateType;
private List<SectionVO> sections;
private List<String> warnings;
```

`ResumeWholeOptimizeVO` fields:

```java
private Integer beforeScore;
private Integer estimatedAfterScore;
private List<String> globalSuggestions;
private List<SectionVO> optimizedSections;
```

- [ ] **Step 4: Extend `ResumeAiService`**

Add:

```java
ResumeImportPreviewVO importResumePreview(ResumeImportRequest request, Long loginUserId);

ResumeWholeOptimizeVO optimizeWholeResume(ResumeWholeOptimizeRequest request, Long loginUserId);
```

- [ ] **Step 5: Implement minimal validation and AI prompt orchestration**

In `ResumeAiServiceImpl`, validate user id, non-blank raw text, resume ownership, and optional job ownership before calling `ChatClient`. Reuse existing JSON parsing helpers where available. Throw `BusinessException(ErrorCode.RESUME_IMPORT_ERROR)` for parse failures and `BusinessException(ErrorCode.RESUME_OPTIMIZE_ERROR)` for optimization failures.

- [ ] **Step 6: Add controller endpoints**

Add to `ResumeAiController`:

```java
@PostMapping("/ai/import-preview")
public BaseResponse<ResumeImportPreviewVO> importPreview(@RequestBody ResumeImportRequest request, HttpServletRequest httpRequest) {
    Long userId = resolveUserId(httpRequest);
    return ResultUtils.success(resumeAiService.importResumePreview(request, userId));
}

@PostMapping("/{resumeId}/ai/optimize-whole")
public BaseResponse<ResumeWholeOptimizeVO> optimizeWhole(@PathVariable Long resumeId,
                                                         @RequestBody ResumeWholeOptimizeRequest request,
                                                         HttpServletRequest httpRequest) {
    request.setResumeId(resumeId);
    Long userId = resolveUserId(httpRequest);
    return ResultUtils.success(resumeAiService.optimizeWholeResume(request, userId));
}
```

- [ ] **Step 7: Run tests**

```powershell
.\mvnw.cmd test -Dtest=ResumeAiImportServiceImplTest,ResumeAiImportControllerTest
```

Expected: all new tests pass.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/resume backend/src/main/java/com/mianshiba/ai/model/vo/resume backend/src/main/java/com/mianshiba/ai/service backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java backend/src/test/java/com/mianshiba/ai
git commit -m "feat: add resume import and whole optimization"
```

## Task 3: Job List, Keywords, Gap Analysis, And Predicted Questions Backend

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/job/JobListQueryRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobKeywordVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobGapAnalysisVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/job/JobQuestionPredictionVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/JobService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/JobServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/JobController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/JobPhase1ServiceImplTest.java`
- Test: `backend/src/test/java/com/mianshiba/ai/controller/JobPhase1ControllerTest.java`

- [ ] **Step 1: Write tests for list filtering and ownership**

Test cases:

```java
@Test
void listJobs_shouldOnlyReturnCurrentUserJobs() {
    JobListQueryRequest request = new JobListQueryRequest();
    request.setCity("杭州");

    List<JobVO> jobs = jobService.listJobs(request, 1L);

    assertThat(jobs).allMatch(job -> job.getCity().contains("杭州"));
}

@Test
void analyzeGap_shouldRejectMissingResumeId() {
    assertThrows(BusinessException.class, () -> jobService.analyzeGap(1L, null, 1L));
}
```

- [ ] **Step 2: Run tests and verify they fail**

```powershell
.\mvnw.cmd test -Dtest=JobPhase1ServiceImplTest
```

Expected: fail because methods and types do not exist.

- [ ] **Step 3: Add request and VO classes**

`JobListQueryRequest` fields:

```java
private String keyword;
private String city;
private String techStack;
private String applicationStatus;
```

`JobKeywordVO` fields:

```java
private List<String> hardSkills;
private List<String> softSkills;
private List<String> bonusSkills;
private List<String> hiddenRequirements;
```

`JobGapAnalysisVO` fields:

```java
private Integer keywordCoverage;
private List<String> matchedKeywords;
private List<String> missingKeywords;
private List<String> projectExpressionGaps;
private List<String> optimizeActions;
```

`JobQuestionPredictionVO` fields:

```java
private List<String> technicalQuestions;
private List<String> projectQuestions;
private List<String> systemDesignQuestions;
private List<String> hrQuestions;
```

- [ ] **Step 4: Extend `JobService`**

Add:

```java
List<JobVO> listJobs(JobListQueryRequest request, Long loginUserId);

JobKeywordVO extractKeywords(Long jobId, Long loginUserId);

JobGapAnalysisVO analyzeGap(Long jobId, Long resumeId, Long loginUserId);

JobQuestionPredictionVO predictQuestions(Long jobId, Long loginUserId);
```

- [ ] **Step 5: Implement list query and AI analysis**

Use MyBatis-Plus `LambdaQueryWrapper<Job>` for list filters. Use existing job ownership checks and existing `ChatClient` style from `AiJobAnalysisServiceImpl` for keyword, gap and question extraction.

- [ ] **Step 6: Add controller endpoints**

Add to `JobController`:

```java
@GetMapping("/list")
public BaseResponse<List<JobVO>> listJobs(JobListQueryRequest request, HttpServletRequest httpRequest) {
    Long userId = resolveUserId(httpRequest);
    return ResultUtils.success(jobService.listJobs(request, userId));
}

@PostMapping("/{jobId}/keywords")
public BaseResponse<JobKeywordVO> extractKeywords(@PathVariable Long jobId, HttpServletRequest httpRequest) {
    Long userId = resolveUserId(httpRequest);
    return ResultUtils.success(jobService.extractKeywords(jobId, userId));
}

@PostMapping("/{jobId}/gap")
public BaseResponse<JobGapAnalysisVO> analyzeGap(@PathVariable Long jobId, @RequestParam Long resumeId, HttpServletRequest httpRequest) {
    Long userId = resolveUserId(httpRequest);
    return ResultUtils.success(jobService.analyzeGap(jobId, resumeId, userId));
}

@PostMapping("/{jobId}/questions")
public BaseResponse<JobQuestionPredictionVO> predictQuestions(@PathVariable Long jobId, HttpServletRequest httpRequest) {
    Long userId = resolveUserId(httpRequest);
    return ResultUtils.success(jobService.predictQuestions(jobId, userId));
}
```

- [ ] **Step 7: Run tests**

```powershell
.\mvnw.cmd test -Dtest=JobPhase1ServiceImplTest,JobPhase1ControllerTest
```

Expected: all new tests pass.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/job backend/src/main/java/com/mianshiba/ai/model/vo/job backend/src/main/java/com/mianshiba/ai/service backend/src/main/java/com/mianshiba/ai/controller/JobController.java backend/src/test/java/com/mianshiba/ai
git commit -m "feat: add job search and preparation analysis"
```

## Task 4: Job-Targeted Timed Interview Backend

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/interview/InterviewCreateRequest.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewSession.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewSessionVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewPhase1ServiceImplTest.java`

- [ ] **Step 1: Write interview creation tests**

Test cases:

```java
@Test
void createSession_shouldPersistJobAndTimingOptions() {
    InterviewCreateRequest request = new InterviewCreateRequest();
    request.setResumeId(1L);
    request.setJobId(2L);
    request.setInterviewType("technical");
    request.setDifficulty("medium");
    request.setDurationMinutes(30);

    InterviewSessionVO session = interviewService.createSession(request, 1L);

    assertThat(session.getJobId()).isEqualTo(2L);
    assertThat(session.getInterviewType()).isEqualTo("technical");
    assertThat(session.getDurationMinutes()).isEqualTo(30);
}
```

- [ ] **Step 2: Run tests and verify they fail**

```powershell
.\mvnw.cmd test -Dtest=InterviewPhase1ServiceImplTest
```

Expected: fail because fields do not exist or are not mapped.

- [ ] **Step 3: Add request/entity/VO fields**

Add these fields consistently:

```java
private Long jobId;
private String interviewType;
private String difficulty;
private Integer durationMinutes;
private LocalDateTime startedAt;
private LocalDateTime endedAt;
```

- [ ] **Step 4: Validate interview options**

Allowed values:

```text
interviewType: technical, project, hr, system_design
difficulty: easy, medium, hard
durationMinutes: 10, 20, 30, 45, 60
```

Invalid values throw `BusinessException(ErrorCode.PARAMS_ERROR)`.

- [ ] **Step 5: Update prompt context**

When `jobId` is provided, include job title, company, JD summary, tech stack and job analysis in question generation prompts. When missing, keep current resume-only behavior.

- [ ] **Step 6: Run tests**

```powershell
.\mvnw.cmd test -Dtest=InterviewPhase1ServiceImplTest,InterviewServiceImplTest,InterviewControllerTest
```

Expected: new and existing interview tests pass.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/interview backend/src/main/java/com/mianshiba/ai/model/entity/InterviewSession.java backend/src/main/java/com/mianshiba/ai/model/vo/interview backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java backend/src/test/java/com/mianshiba/ai
git commit -m "feat: add job-targeted timed interviews"
```

## Task 5: Analytics Overview Backend

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/AnalyticsOverviewVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/StatisticsPhase1ServiceImplTest.java`

- [ ] **Step 1: Write aggregation tests**

Test cases:

```java
@Test
void getAnalyticsOverview_shouldReturnZerosForNewUser() {
    AnalyticsOverviewVO overview = statisticsService.getAnalyticsOverview(1L);

    assertThat(overview.getResumeCount()).isZero();
    assertThat(overview.getJobCount()).isZero();
    assertThat(overview.getInterviewCount()).isZero();
}
```

- [ ] **Step 2: Run tests and verify they fail**

```powershell
.\mvnw.cmd test -Dtest=StatisticsPhase1ServiceImplTest
```

Expected: fail because VO and method do not exist.

- [ ] **Step 3: Add `AnalyticsOverviewVO`**

Fields:

```java
private Integer resumeCount;
private Integer jobCount;
private Integer interviewCount;
private Integer averageInterviewScore;
private List<String> topMissingSkills;
private List<String> nextActions;
```

- [ ] **Step 4: Implement aggregation**

Aggregate from existing resume, job, job_match, interview_session and interview_report tables. For Phase 1, return a deterministic `nextActions` list based on missing data:

```text
No resume -> 上传或创建一份简历
No job -> 导入一个目标职位
No interview -> 完成一次岗位定向面试
Has job_match gaps -> 优先补齐最高频技能缺口
```

- [ ] **Step 5: Add endpoint**

Add to `StatisticsController`:

```java
@GetMapping("/analytics/overview")
public BaseResponse<AnalyticsOverviewVO> getAnalyticsOverview(HttpServletRequest request) {
    Long userId = resolveUserId(request);
    return ResultUtils.success(statisticsService.getAnalyticsOverview(userId));
}
```

- [ ] **Step 6: Run tests**

```powershell
.\mvnw.cmd test -Dtest=StatisticsPhase1ServiceImplTest,StatisticsControllerTest
```

Expected: tests pass.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/vo/statistics backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java backend/src/test/java/com/mianshiba/ai
git commit -m "feat: add analytics overview backend"
```

## Task 6: Resume Frontend Enhancements

**Files:**
- Modify: `frontend/src/types/resume.ts`
- Modify: `frontend/src/api/resume.ts`
- Modify: `frontend/src/stores/resume.ts`
- Create: `frontend/src/components/resume/ResumeImportDialog.vue`
- Create: `frontend/src/components/resume/WholeResumeOptimizeDialog.vue`
- Modify: `frontend/src/views/resume/ResumeListPage.vue`
- Modify: `frontend/src/views/resume/ResumeEditPage.vue`

- [ ] **Step 1: Add frontend types**

Add TypeScript interfaces matching backend VO/DTO names:

```ts
export interface ResumeImportRequest {
  fileName?: string
  fileType?: string
  rawText: string
}

export interface ResumeImportPreviewVO {
  title: string
  templateType: string
  sections: SectionVO[]
  warnings: string[]
}

export interface ResumeWholeOptimizeRequest {
  resumeId: number
  jobId?: number
  targetPosition?: string
  optimizeGoal?: string
}

export interface ResumeWholeOptimizeVO {
  beforeScore: number
  estimatedAfterScore: number
  globalSuggestions: string[]
  optimizedSections: SectionVO[]
}
```

- [ ] **Step 2: Add API functions**

```ts
export const importResumePreview = (data: ResumeImportRequest) => {
  return request.post<BaseResponse<ResumeImportPreviewVO>>('/api/resume/ai/import-preview', data)
}

export const optimizeWholeResume = (resumeId: number, data: ResumeWholeOptimizeRequest) => {
  return request.post<BaseResponse<ResumeWholeOptimizeVO>>(`/api/resume/${resumeId}/ai/optimize-whole`, data)
}
```

- [ ] **Step 3: Add import dialog**

The dialog supports paste-first import in Phase 1. File binary parsing can be added after backend parser support is stable. The component emits `created` after the user confirms parsed sections and creates a resume.

- [ ] **Step 4: Add whole optimize dialog**

The dialog shows before/after score, global suggestions, and optimized section preview. It emits `apply` with optimized sections.

- [ ] **Step 5: Wire pages**

Add “导入简历” in `ResumeListPage.vue`. Add “整份优化” in `ResumeEditPage.vue`.

- [ ] **Step 6: Verify frontend**

Run in `frontend/`:

```powershell
npm run type-check
npm run build
```

Expected: type-check and build pass.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/types/resume.ts frontend/src/api/resume.ts frontend/src/stores/resume.ts frontend/src/components/resume frontend/src/views/resume
git commit -m "feat: add resume import and whole optimize UI"
```

## Task 7: Job Frontend Enhancements

**Files:**
- Modify: `frontend/src/types/job.ts`
- Modify: `frontend/src/api/job.ts`
- Modify: `frontend/src/stores/job.ts`
- Create: `frontend/src/views/job/JobListPage.vue`
- Create: `frontend/src/views/job/JobQuestionsPage.vue`
- Modify: `frontend/src/views/job/JobDetailPage.vue`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Add frontend types**

```ts
export interface JobListQueryRequest {
  keyword?: string
  city?: string
  techStack?: string
  applicationStatus?: string
}

export interface JobKeywordVO {
  hardSkills: string[]
  softSkills: string[]
  bonusSkills: string[]
  hiddenRequirements: string[]
}

export interface JobGapAnalysisVO {
  keywordCoverage: number
  matchedKeywords: string[]
  missingKeywords: string[]
  projectExpressionGaps: string[]
  optimizeActions: string[]
}

export interface JobQuestionPredictionVO {
  technicalQuestions: string[]
  projectQuestions: string[]
  systemDesignQuestions: string[]
  hrQuestions: string[]
}
```

- [ ] **Step 2: Add API functions**

```ts
export const listJobs = (params: JobListQueryRequest) => request.get<BaseResponse<JobVO[]>>('/api/job/list', { params })
export const extractJobKeywords = (jobId: number) => request.post<BaseResponse<JobKeywordVO>>(`/api/job/${jobId}/keywords`)
export const analyzeJobGap = (jobId: number, resumeId: number) => request.post<BaseResponse<JobGapAnalysisVO>>(`/api/job/${jobId}/gap`, null, { params: { resumeId } })
export const predictJobQuestions = (jobId: number) => request.post<BaseResponse<JobQuestionPredictionVO>>(`/api/job/${jobId}/questions`)
```

- [ ] **Step 3: Add pages and routes**

Routes:

```ts
{
  path: '/job/list',
  name: 'job-list',
  component: () => import('@/views/job/JobListPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/job/:id/questions',
  name: 'job-questions',
  component: () => import('@/views/job/JobQuestionsPage.vue'),
  meta: { requiresAuth: true },
},
```

- [ ] **Step 4: Update `JobDetailPage.vue`**

Add actions: “分析简历差距”, “预测面试题”, “去定向面试”.

- [ ] **Step 5: Verify frontend**

```powershell
npm run type-check
npm run build
```

Expected: type-check and build pass.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/types/job.ts frontend/src/api/job.ts frontend/src/stores/job.ts frontend/src/views/job frontend/src/router/index.ts
git commit -m "feat: add job preparation UI"
```

## Task 8: Interview Frontend Timer And Targeting

**Files:**
- Modify: `frontend/src/types/interview.ts`
- Modify: `frontend/src/views/interview/InterviewNewPage.vue`
- Modify: `frontend/src/views/interview/InterviewRoomPage.vue`

- [ ] **Step 1: Add interview request fields**

```ts
export interface InterviewCreateRequest {
  resumeId: number
  jobId?: number
  interviewType?: 'technical' | 'project' | 'hr' | 'system_design'
  difficulty?: 'easy' | 'medium' | 'hard'
  durationMinutes?: 10 | 20 | 30 | 45 | 60
}
```

- [ ] **Step 2: Update creation page**

Add selects for target job, interview type, difficulty and duration. Default values:

```ts
interviewType: 'technical'
difficulty: 'medium'
durationMinutes: 30
```

- [ ] **Step 3: Add room timer**

In `InterviewRoomPage.vue`, compute remaining seconds from `startedAt` and `durationMinutes`. Show warning when remaining time is below 120 seconds. Stop submitting new audio when remaining time is zero and prompt user to generate report.

- [ ] **Step 4: Verify frontend**

```powershell
npm run type-check
npm run build
```

Expected: type-check and build pass.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/types/interview.ts frontend/src/views/interview/InterviewNewPage.vue frontend/src/views/interview/InterviewRoomPage.vue
git commit -m "feat: add interview targeting and timer UI"
```

## Task 9: Analytics Overview Frontend

**Files:**
- Modify: `frontend/src/types/statistics.ts`
- Modify: `frontend/src/api/statistics.ts`
- Create: `frontend/src/views/analytics/AnalyticsOverviewPage.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue` if navigation needs an analytics link.

- [ ] **Step 1: Add analytics type**

```ts
export interface AnalyticsOverviewVO {
  resumeCount: number
  jobCount: number
  interviewCount: number
  averageInterviewScore: number
  topMissingSkills: string[]
  nextActions: string[]
}
```

- [ ] **Step 2: Add API function**

```ts
export const getAnalyticsOverview = () => {
  return request.get<BaseResponse<AnalyticsOverviewVO>>('/api/statistics/analytics/overview')
}
```

- [ ] **Step 3: Add analytics route**

```ts
{
  path: '/analytics',
  name: 'analytics',
  component: () => import('@/views/analytics/AnalyticsOverviewPage.vue'),
  meta: { requiresAuth: true },
},
```

- [ ] **Step 4: Add dashboard page**

Show four cards: 简历数、职位数、面试数、平均面试分。 Show two lists: 高频技能缺口、下一步行动。

- [ ] **Step 5: Verify frontend**

```powershell
npm run type-check
npm run build
```

Expected: type-check and build pass.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/types/statistics.ts frontend/src/api/statistics.ts frontend/src/views/analytics frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue
git commit -m "feat: add analytics overview page"
```

## Task 10: Full Verification

**Files:**
- No source edits unless verification exposes defects.

- [ ] **Step 1: Run backend full tests**

Run in `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: all tests pass. If integration tests require external services, document the exact failing test and verify unit/controller tests separately.

- [ ] **Step 2: Run backend package**

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: package succeeds.

- [ ] **Step 3: Run frontend verification**

Run in `frontend/`:

```powershell
npm run type-check
npm run build
npm run test:unit
```

Expected: type-check, build, and unit tests pass.

- [ ] **Step 4: Review diff**

Run at repository root:

```powershell
git status --short
git diff --stat
```

Expected: only intended Phase 1 files changed.

- [ ] **Step 5: Commit final verification fixes if needed**

If verification required fixes:

```powershell
git add <fixed-files>
git commit -m "fix: stabilize phase one closed loop"
```

---

## Self-Review

### Spec Coverage

- 简历文件导入：Task 2 and Task 6.
- 整份简历 AI 优化：Task 2 and Task 6.
- 岗位定向面试：Task 4 and Task 8.
- 面试计时器：Task 4 and Task 8.
- 职位列表/搜索：Task 3 and Task 7.
- JD 关键词和简历差距分析：Task 3 and Task 7.
- 求职仪表盘基础版：Task 5 and Task 9.

### Scope Decisions

- Phase 1 uses paste-first resume import in frontend to reduce parser risk. Backend DTO supports file metadata so PDF/DOCX parsing can be added without changing the user-facing contract.
- Phase 1 adds predicted interview questions as a job preparation feature because it is directly tied to job-targeted interviews.
- Phase 1 does not include Diff, rollback, recording, learning plan, job funnel or sharing because those are Phase 2/3 in the approved design.

### Verification

- Each backend task includes focused tests before implementation and a Maven verification command.
- Each frontend task includes `npm run type-check` and `npm run build`.
- Final task includes backend tests, backend package, frontend type-check, frontend build and frontend unit tests.
