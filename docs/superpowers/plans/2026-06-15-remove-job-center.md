# Remove Job Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the job center, job crawling, company intelligence, and job recommendation features, while keeping application tracking as a manual job application notebook.

**Architecture:** Delete job-domain controllers, services, mappers, entities, DTOs, VOs, scheduler, browser auth, and frontend routes/pages. Keep `ApplicationService` and `ApplicationTodoService`, but remove `jobId` and `JobMapper` coupling so applications are stored from user-entered company, role, JD/source, salary, location, status, contacts, and notes.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis-Plus, JUnit 5/Mockito, Vue 3, TypeScript, Vite, Pinia, Element Plus, MySQL init SQL.

---

## File Structure

- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationCreateRequest.java` removes `jobId`; optionally adds `jobDescription` if the SQL entity already supports it.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationUpdateRequest.java` removes `jobId`.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationListQueryRequest.java` removes `jobId` filter.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/JobApplication.java` removes `jobId` field and maps only manual application fields.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/application/JobApplicationVO.java` removes `jobId`.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java` removes `JobMapper`/`Job` import, constructor field, validation, persistence, update, and list filtering by `jobId`.
- Delete: `backend/src/main/java/com/mianshiba/ai/controller/JobController.java` and every job/company/crawl/recommendation controller, service, mapper, entity, DTO, VO, scheduler, config, and tool class that is not used by applications.
- Modify: `backend/src/main/resources/sql/init.sql` deletes job/company/crawl/recommendation/auth tables and removes `job_id` from `job_application`.
- Modify: `backend/src/main/resources/application.yml` removes `app.job-sourcing` configuration.
- Delete: `backend/scripts/open-boss-auth-profile.ps1` and `backend/src/main/java/com/mianshiba/ai/tools/OpenBossAuthProfile.java`.
- Modify/Delete tests under `backend/src/test/java/com/mianshiba/ai/**` so no test imports deleted job/crawl/company classes.
- Modify: `frontend/src/router/index.ts` removes `/job/**`, `/company/:id`, and admin job crawl routes.
- Modify: `frontend/src/layouts/MainLayout.vue` removes job center navigation.
- Modify: `frontend/src/types/application.ts` removes `jobId` from request/list/VO types.
- Modify: `frontend/src/views/application/*.vue` removes any job link or job id usage and keeps manual fields.
- Delete: `frontend/src/views/job/**`, `frontend/src/api/job*.ts`, `frontend/src/stores/job*.ts`, `frontend/src/types/job*.ts`, and admin job crawl frontend files.

## Tasks

### Task 1: Lock Manual Application Behavior

**Files:**
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/ApplicationServiceImplTest.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java`

- [ ] **Step 1: Add or update a service test that creates an application without `jobId`**

```java
@Test
void createApplication_acceptsManualJobFieldsWithoutJobId() {
    ApplicationCreateRequest request = new ApplicationCreateRequest();
    request.setCompanyName("手动公司");
    request.setJobTitle("后端开发工程师");
    request.setSource("内推");
    request.setStatus("submitted");
    request.setSalaryRange("20-30K");
    request.setLocation("北京");

    when(jwtUtils.getUserIdFromToken(anyString())).thenReturn(1L);
    when(applicationMapper.insert(any(JobApplication.class))).thenAnswer(invocation -> {
        JobApplication saved = invocation.getArgument(0);
        saved.setId(10L);
        saved.setCreateTime(LocalDateTime.now());
        saved.setUpdateTime(LocalDateTime.now());
        return 1;
    });

    JobApplicationVO result = applicationService.createApplication("Bearer token", request);

    assertThat(result.getCompanyName()).isEqualTo("手动公司");
    assertThat(result.getJobTitle()).isEqualTo("后端开发工程师");
    assertThat(result.getSource()).isEqualTo("内推");
    verify(applicationMapper).insert(argThat(app -> app.getUserId().equals(1L)
            && app.getCompanyName().equals("手动公司")
            && app.getJobTitle().equals("后端开发工程师")));
}
```

- [ ] **Step 2: Run the focused test and confirm current behavior**

Run: `.[mvnw.cmd test -pl . -Dtest=ApplicationServiceImplTest#createApplication_acceptsManualJobFieldsWithoutJobId`

Expected: either PASS if the manual path already works, or FAIL because the constructor/test still expects `JobMapper` or `jobId` behavior.

- [ ] **Step 3: Remove `jobId` from backend application request/query/VO/entity/service code**

Delete these exact pieces:
- `private Long jobId;` from application DTO/VO/entity classes.
- `JobMapper` and `Job` imports and constructor field from `ApplicationServiceImpl`.
- `jobMapper.selectById(...)` validation block from `createApplication`.
- `app.setJobId(...)` calls from create/update.
- `request.getJobId()` query filter from `listApplications`.

- [ ] **Step 4: Run application tests**

Run: `.[mvnw.cmd test -pl . -Dtest=ApplicationServiceImplTest,ApplicationControllerTest`

Expected: PASS.

### Task 2: Delete Backend Job Domain

**Files:**
- Delete: backend job/company/crawl/recommendation controllers, services, implementations, mappers, entities, DTOs, VOs, scheduler, tools, and config.
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` only if it has now-unused job/crawl/company error codes and no remaining references.
- Modify: backend tests importing deleted classes.

- [ ] **Step 1: Find backend job-domain references**

Run: `rg "JobController|JobService|JobMapper|JobFavorite|JobMatch|JobRecommendation|CompanyProfile|JobCrawl|JobSourcing|BrowserSession|PlatformAuthSession|OpenBossAuthProfile|JobSourcingProperties" backend/src`

Expected: references identify files to delete or update.

- [ ] **Step 2: Delete job-domain files**

Delete files where the class belongs only to job center, company intelligence, crawling, sourcing, platform browser auth, or recommendation. Do not delete `JobApplication*`, `ApplicationTodo*`, training, interview, resume, coach, dashboard, or statistics files.

- [ ] **Step 3: Remove stale Spring configuration**

Remove `@ConfigurationProperties` registration/imports for `JobSourcingProperties`, remove `app.job-sourcing` from `application.yml`, and remove Playwright/Boss auth scripts/tools.

- [ ] **Step 4: Compile backend**

Run: `.[mvnw.cmd clean package -DskipTests`

Expected: SUCCESS. If compilation fails, every failure should be either a stale import/reference to deleted job code or a test-only reference that must be removed.

### Task 3: Clean SQL Schema

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`

- [ ] **Step 1: Remove `job_id` from `job_application`**

Remove the `job_id BIGINT ...` line and `idx_application_job_id` index.

- [ ] **Step 2: Delete job/company/crawl/recommendation/auth tables**

Remove complete `CREATE TABLE` blocks for `job`, `job_analysis`, `job_match`, `job_favorite`, `job_recommendation`, `job_crawl_task`, `job_crawl_run`, `job_crawl_item`, `company`, `company_certification`, and `platform_auth_session`.

- [ ] **Step 3: Search SQL for stale job references**

Run: `rg "job_id|job_crawl|job_recommendation|platform_auth|company_certification|CREATE TABLE job|CREATE TABLE company" backend/src/main/resources/sql/init.sql`

Expected: no matches except unrelated natural-language comments if any remain and are intentional.

### Task 4: Delete Frontend Job Domain

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/types/application.ts`
- Modify: `frontend/src/views/application/*.vue`
- Delete: job and job crawl frontend API/store/type/view files.

- [ ] **Step 1: Remove job/company/admin crawl routes**

Delete routes for `/job/recommendations`, `/job/import`, `/job/favorites`, `/job/list`, `/job/:id`, `/job/:id/questions`, `/company/:id`, and admin job crawl pages.

- [ ] **Step 2: Remove navigation entries**

Delete the `MainLayout.vue` navigation item for job intelligence/job center. Keep application, resume, interview, training, coach, analytics, profile, and admin navigation.

- [ ] **Step 3: Remove job types from application types**

Delete `jobId` from `JobApplicationVO`, `ApplicationCreateRequest`, and `ApplicationListQueryRequest` in `frontend/src/types/application.ts`.

- [ ] **Step 4: Delete frontend job-domain files**

Delete `frontend/src/views/job/**`, job crawl admin pages, `frontend/src/api/job*.ts`, `frontend/src/stores/job*.ts`, and `frontend/src/types/job*.ts` if no remaining non-job module imports them.

- [ ] **Step 5: Search frontend stale references**

Run: `rg "jobId|/job|/company|JobCrawl|jobCrawl|JobRecommendation|JobFavorite" frontend/src`

Expected: no matches except legitimate `jobTitle` field names used by manual applications.

### Task 5: Full Verification

**Files:**
- All touched files.

- [ ] **Step 1: Run backend tests**

Run: `.[mvnw.cmd test`

Expected: SUCCESS. If MySQL/Redis integration assumptions leak into tests, keep tests aligned with the project rule that tests do not connect to real infrastructure.

- [ ] **Step 2: Run frontend type-check/build**

Run: `npm run build`

Expected: SUCCESS.

- [ ] **Step 3: Final stale-reference search**

Run: `rg "JobController|JobService|JobMapper|JobFavorite|JobMatch|JobRecommendation|CompanyProfile|JobCrawl|JobSourcing|BrowserSession|PlatformAuthSession|/job|/company|jobId|job_id" backend/src frontend/src backend/src/main/resources/sql/init.sql`

Expected: no matches except `JobApplication`, `jobTitle`, and manual application wording.

- [ ] **Step 4: Inspect git diff**

Run: `git status --short` and `git diff --stat`

Expected: only files related to job-domain deletion, application manual tracking, SQL cleanup, and this plan are changed.

## Self-Review

- Spec coverage: The plan removes job center, job crawling, company intelligence, and recommendation while preserving manual application tracking.
- Placeholder scan: No task uses `TBD`, `TODO`, or unspecified implementation language.
- Type consistency: `jobId`/`job_id` removal is consistently applied across DTO, VO, entity, service, SQL, and frontend types.
