# Second Phase Phase 2 Review Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Phase 2 review enhancement: asynchronous interview report enhancement, per-turn excellent answers, report comparison, and review analytics.

**Architecture:** Keep the existing Spring Boot layered backend and Vue 3 frontend structure. Add two report enhancement tables, a small Redis Stream publisher/consumer boundary, focused services for enhancement/comparison/analytics, and lightweight frontend sections on the existing report and analytics pages. The basic interview report remains available immediately; AI enhancement runs asynchronously and is queried by status.

**Tech Stack:** Java 17, Spring Boot 3.5.x, MyBatis-Plus, Spring Data Redis Stream, Spring AI ChatClient, MySQL JSON columns, Vue 3, TypeScript, Pinia, Element Plus, Vite.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql` — create `interview_report_enhancement` and `interview_turn_review` tables.
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` — add review enhancement error codes.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewReportEnhancement.java` — DB entity for enhanced report status and summary JSON fields.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurnReview.java` — DB entity for per-turn review output.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewReportEnhancementMapper.java` — MyBatis-Plus mapper.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewTurnReviewMapper.java` — MyBatis-Plus mapper.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportEnhancementVO.java` — API response for enhancement status/results.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnReviewVO.java` — per-turn review response.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportCompareVO.java` — report comparison response.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewScoreDeltaVO.java` — dimension score delta response.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/ReviewAnalyticsVO.java` — review analytics response.
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewReportEnhancementService.java` — enhancement task API.
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewReportCompareService.java` — report comparison API.
- Create: `backend/src/main/java/com/mianshiba/ai/service/ReviewAnalyticsService.java` — review analytics API.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImpl.java` — task creation/query/retry and AI persistence.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportCompareServiceImpl.java` — comparison implementation.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ReviewAnalyticsServiceImpl.java` — review analytics implementation.
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewReportEnhancementQueue.java` — Redis Stream publishing abstraction.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/RedisInterviewReportEnhancementQueue.java` — Redis Stream publisher.
- Create: `backend/src/main/java/com/mianshiba/ai/worker/InterviewReportEnhancementWorker.java` — Redis Stream consumer.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java` — trigger enhancement after base report insert.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java` — add enhancement and compare endpoints.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java` — add review analytics endpoint.

### Frontend Files

- Modify: `frontend/src/types/interview.ts` — add enhancement and compare types.
- Modify: `frontend/src/types/statistics.ts` — add review analytics types.
- Modify: `frontend/src/api/interview.ts` — add enhancement, retry, compare API calls.
- Modify: `frontend/src/api/statistics.ts` — add review analytics API call.
- Modify: `frontend/src/stores/interview.ts` — add enhancement and comparison state/actions.
- Modify: `frontend/src/views/interview/InterviewReportPage.vue` — render async enhancement, per-turn excellent answers, retry, comparison.
- Modify: `frontend/src/views/analytics/AnalyticsOverviewPage.vue` — render review radar, skill gaps, score trend, action items.

---

## Task 1: Backend Schema And Error Codes

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`

- [ ] **Step 1: Add failing schema presence test**

Create `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase2Test.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase2Test {

    @Test
    void initSqlContainsPhase2ReviewTables() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS interview_report_enhancement");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS interview_turn_review");
        assertThat(sql).contains("UNIQUE KEY uk_report_id (report_id)");
        assertThat(sql).contains("KEY idx_report_id (report_id)");
        assertThat(sql).contains("KEY idx_turn_id (turn_id)");
    }
}
```

- [ ] **Step 2: Run the failing test**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase2Test
```

Expected: FAIL because the two table definitions are not present.

- [ ] **Step 3: Add Phase 2 tables to `init.sql`**

Add after `interview_report` table:

```sql
CREATE TABLE IF NOT EXISTS interview_report_enhancement (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告增强 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  session_id BIGINT NOT NULL COMMENT '面试会话 id',
  report_id BIGINT NOT NULL COMMENT '面试报告 id',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/completed/failed',
  summary TEXT DEFAULT NULL COMMENT '增强复盘摘要',
  radar_json JSON DEFAULT NULL COMMENT '能力雷达',
  skill_gaps_json JSON DEFAULT NULL COMMENT '技能缺口',
  action_items_json JSON DEFAULT NULL COMMENT '下一步行动建议',
  error_message VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_report_id (report_id),
  KEY idx_user_id (user_id),
  KEY idx_session_id (session_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 面试报告增强表';

CREATE TABLE IF NOT EXISTS interview_turn_review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '轮次复盘 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  session_id BIGINT NOT NULL COMMENT '面试会话 id',
  report_id BIGINT NOT NULL COMMENT '面试报告 id',
  turn_id BIGINT NOT NULL COMMENT '面试轮次 id',
  question TEXT NOT NULL COMMENT '问题快照',
  answer_summary TEXT DEFAULT NULL COMMENT '用户回答摘要',
  diagnosis TEXT DEFAULT NULL COMMENT '回答问题诊断',
  excellent_answer TEXT DEFAULT NULL COMMENT '优秀回答示例',
  improved_answer TEXT DEFAULT NULL COMMENT '基于用户回答改写后的版本',
  knowledge_points_json JSON DEFAULT NULL COMMENT '考察知识点',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_session_id (session_id),
  KEY idx_report_id (report_id),
  KEY idx_turn_id (turn_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 面试轮次复盘表';
```

- [ ] **Step 4: Add error codes**

In `ErrorCode.java`, add:

```java
INTERVIEW_REPORT_ENHANCE_ERROR(50013, "面试报告增强失败"),
INTERVIEW_REPORT_COMPARE_ERROR(50014, "面试报告对比失败"),
```

- [ ] **Step 5: Run the schema test**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase2Test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase2Test.java
git commit -m "feat: add phase two review schema"
```

## Task 2: Backend Entities, Mappers, And VO Types

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewReportEnhancement.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurnReview.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewReportEnhancementMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewTurnReviewMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnReviewVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportEnhancementVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewScoreDeltaVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportCompareVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/ReviewAnalyticsVO.java`

- [ ] **Step 1: Create entity classes**

`InterviewReportEnhancement.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "interview_report_enhancement", autoResultMap = true)
public class InterviewReportEnhancement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long reportId;
    private String status;
    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> radarJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> skillGapsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> actionItemsJson;

    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

`InterviewTurnReview.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "interview_turn_review", autoResultMap = true)
public class InterviewTurnReview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long reportId;
    private Long turnId;
    private String question;
    private String answerSummary;
    private String diagnosis;
    private String excellentAnswer;
    private String improvedAnswer;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> knowledgePointsJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 2: Create mapper classes**

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.InterviewReportEnhancement;

public interface InterviewReportEnhancementMapper extends BaseMapper<InterviewReportEnhancement> {
}
```

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.InterviewTurnReview;

public interface InterviewTurnReviewMapper extends BaseMapper<InterviewTurnReview> {
}
```

- [ ] **Step 3: Create VO classes**

Use these fields exactly:

```java
// InterviewTurnReviewVO.java fields
private Long id;
private Long turnId;
private String question;
private String answerSummary;
private String diagnosis;
private String excellentAnswer;
private String improvedAnswer;
private List<String> knowledgePoints;
```

```java
// InterviewReportEnhancementVO.java fields
private Long id;
private Long sessionId;
private Long reportId;
private String status;
private String summary;
private Map<String, Integer> radar;
private List<Map<String, String>> skillGaps;
private List<String> actionItems;
private String errorMessage;
private Integer retryCount;
private List<InterviewTurnReviewVO> turnReviews;
```

```java
// InterviewScoreDeltaVO.java fields
private String key;
private String label;
private Integer baseScore;
private Integer targetScore;
private Integer delta;
```

```java
// InterviewReportCompareVO.java fields
private Long baseSessionId;
private Long targetSessionId;
private Integer baseTotalScore;
private Integer targetTotalScore;
private Integer totalDelta;
private List<InterviewScoreDeltaVO> dimensions;
private List<String> newSkillGaps;
private List<String> resolvedSkillGaps;
private List<String> summary;
```

```java
// ReviewAnalyticsVO.java fields
private Map<String, Integer> radar;
private List<Map<String, String>> topSkillGaps;
private List<Map<String, Object>> recentScoreTrend;
private List<String> latestActionItems;
```

- [ ] **Step 4: Compile**

Run in `backend/`:

```powershell
.\mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/entity/InterviewReportEnhancement.java backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurnReview.java backend/src/main/java/com/mianshiba/ai/mapper/InterviewReportEnhancementMapper.java backend/src/main/java/com/mianshiba/ai/mapper/InterviewTurnReviewMapper.java backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnReviewVO.java backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportEnhancementVO.java backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewScoreDeltaVO.java backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportCompareVO.java backend/src/main/java/com/mianshiba/ai/model/vo/statistics/ReviewAnalyticsVO.java
git commit -m "feat: add report enhancement models"
```

## Task 3: Enhancement Queue And Service

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewReportEnhancementQueue.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewReportEnhancementService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/RedisInterviewReportEnhancementQueue.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImplTest.java`

- [ ] **Step 1: Write service tests first**

Create tests for task creation and retry guard:

```java
@Test
void createEnhancementTask_shouldCreatePendingTaskAndPublishMessage() {
    InterviewReport report = new InterviewReport();
    report.setId(10L);
    report.setSessionId(20L);
    InterviewSession session = new InterviewSession();
    session.setId(20L);
    session.setUserId(1L);

    enhancementService.createTaskIfAbsent(session, report);

    verify(enhancementMapper).insert(argThat(task ->
            task.getUserId().equals(1L)
                    && task.getSessionId().equals(20L)
                    && task.getReportId().equals(10L)
                    && "pending".equals(task.getStatus())));
    verify(queue).publish(argThat(task -> task.getReportId().equals(10L)));
}

@Test
void retryEnhancement_shouldRejectRunningTask() {
    InterviewReportEnhancement task = new InterviewReportEnhancement();
    task.setId(1L);
    task.setUserId(1L);
    task.setSessionId(20L);
    task.setReportId(10L);
    task.setStatus("running");

    when(enhancementMapper.selectOne(any())).thenReturn(task);

    assertThrows(BusinessException.class, () -> enhancementService.retry("Bearer token", 20L));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportEnhancementServiceImplTest
```

Expected: FAIL because service and queue classes do not exist.

- [ ] **Step 3: Define service and queue interfaces**

`InterviewReportEnhancementQueue.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.InterviewReportEnhancement;

public interface InterviewReportEnhancementQueue {
    String STREAM_KEY = "interview.report.enhancement.stream";
    String GROUP_NAME = "interview-report-enhancement-workers";

    void publish(InterviewReportEnhancement enhancement);
}
```

`InterviewReportEnhancementService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.vo.interview.InterviewReportEnhancementVO;

public interface InterviewReportEnhancementService {
    void createTaskIfAbsent(InterviewSession session, InterviewReport report);

    InterviewReportEnhancementVO getEnhancement(String authorizationHeader, Long sessionId);

    InterviewReportEnhancementVO retry(String authorizationHeader, Long sessionId);

    void runTask(Long enhancementId);
}
```

- [ ] **Step 4: Implement Redis publisher**

Use `StringRedisTemplate` and publish four fields:

```java
Map<String, String> body = Map.of(
        "enhancementId", String.valueOf(enhancement.getId()),
        "reportId", String.valueOf(enhancement.getReportId()),
        "sessionId", String.valueOf(enhancement.getSessionId()),
        "userId", String.valueOf(enhancement.getUserId())
);
stringRedisTemplate.opsForStream().add(InterviewReportEnhancementQueue.STREAM_KEY, body);
```

- [ ] **Step 5: Implement minimal service methods**

Rules:

- `createTaskIfAbsent` selects by `reportId`; if found, return without publishing.
- New task fields: `status=pending`, `retryCount=0`, `isDelete=0`.
- Insert first, then publish so the message includes `enhancement.id`.
- `getEnhancement` resolves user from JWT, checks session ownership, returns empty `pending` VO if no task exists.
- `retry` rejects `running`, sets `status=pending`, clears `errorMessage`, increments `retryCount`, updates DB, then publishes.
- `runTask` is implemented in Task 4; for Task 3 it may load the task and throw `BusinessException(ErrorCode.INTERVIEW_REPORT_ENHANCE_ERROR)` if missing.

- [ ] **Step 6: Trigger task from report generation**

In `InterviewServiceImpl`, inject `InterviewReportEnhancementService` and update `generateReport` after `interviewReportMapper.insert(report);`:

```java
interviewReportEnhancementService.createTaskIfAbsent(session, report);
```

- [ ] **Step 7: Run tests and compile**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportEnhancementServiceImplTest
.\mvnw.cmd compile
```

Expected: tests PASS and compile BUILD SUCCESS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/InterviewReportEnhancementQueue.java backend/src/main/java/com/mianshiba/ai/service/InterviewReportEnhancementService.java backend/src/main/java/com/mianshiba/ai/service/impl/RedisInterviewReportEnhancementQueue.java backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImplTest.java
git commit -m "feat: enqueue interview report enhancements"
```

## Task 4: Enhancement Worker And AI Persistence

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/worker/InterviewReportEnhancementWorker.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementWorkerTest.java`

- [ ] **Step 1: Write worker/service test for successful AI persistence**

Test with a mocked AI JSON response:

```json
{
  "summary": "整体不错，但系统设计深度不足",
  "radar": {"accuracy": 80, "clarity": 75, "depth": 60, "matching": 70, "systemDesign": 55},
  "skillGaps": [{"name": "系统设计", "severity": "high", "evidence": "缺少容量估算"}],
  "actionItems": ["练习缓存和限流设计"],
  "turnReviews": [{"turnId": 1, "answerSummary": "回答了基本概念", "diagnosis": "缺少工程取舍", "excellentAnswer": "可以从目标、方案、权衡回答", "improvedAnswer": "我会先明确目标...", "knowledgePoints": ["缓存", "限流"]}]
}
```

Verify:

```java
verify(enhancementMapper).updateById(argThat(task ->
        "completed".equals(task.getStatus())
                && "整体不错，但系统设计深度不足".equals(task.getSummary())));
verify(turnReviewMapper).insert(argThat(review ->
        review.getTurnId().equals(1L)
                && review.getExcellentAnswer().contains("目标、方案、权衡")));
```

- [ ] **Step 2: Run test to verify it fails**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportEnhancementWorkerTest
```

Expected: FAIL because `runTask` does not persist AI output yet.

- [ ] **Step 3: Implement `runTask(Long enhancementId)`**

Implementation rules:

- Load enhancement by id; return if status is `completed`.
- Set status to `running` before AI call.
- Load session, report, and turns ordered by `questionNo`, `id`.
- Build prompt with report scores, summary, suggestions, and all answered turns.
- Call `ChatClient` and parse JSON using the same JSON-code-block pattern style used in `InterviewServiceImpl`.
- Delete existing `interview_turn_review` rows for this report before inserting regenerated rows.
- Insert one `InterviewTurnReview` per returned turn review.
- Update enhancement with `completed`, `summary`, `radarJson`, `skillGapsJson`, `actionItemsJson`, and `errorMessage=null`.
- On any exception, set `failed` and `errorMessage` to a 512-character max message, then throw `BusinessException(ErrorCode.INTERVIEW_REPORT_ENHANCE_ERROR)`.

- [ ] **Step 4: Implement Redis Stream worker**

Use a scheduled poller instead of a permanently blocking thread:

```java
@Scheduled(fixedDelay = 5000)
public void poll() {
    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
            Consumer.from(InterviewReportEnhancementQueue.GROUP_NAME, consumerName),
            StreamReadOptions.empty().count(5).block(Duration.ofSeconds(1)),
            StreamOffset.create(InterviewReportEnhancementQueue.STREAM_KEY, ReadOffset.lastConsumed())
    );
    if (records == null) {
        return;
    }
    for (MapRecord<String, Object, Object> record : records) {
        Long enhancementId = Long.valueOf(String.valueOf(record.getValue().get("enhancementId")));
        enhancementService.runTask(enhancementId);
        stringRedisTemplate.opsForStream().acknowledge(
                InterviewReportEnhancementQueue.STREAM_KEY,
                InterviewReportEnhancementQueue.GROUP_NAME,
                record.getId()
        );
    }
}
```

Add group creation in `@PostConstruct`; ignore BUSYGROUP errors.

- [ ] **Step 5: Enable scheduling**

Add `@EnableScheduling` to `backend/src/main/java/com/mianshiba/ai/MianshibaAiBackendApplication.java` if it is not already present.

- [ ] **Step 6: Run tests**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportEnhancementWorkerTest,InterviewReportEnhancementServiceImplTest
.\mvnw.cmd compile
```

Expected: PASS and BUILD SUCCESS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/worker/InterviewReportEnhancementWorker.java backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementServiceImpl.java backend/src/main/java/com/mianshiba/ai/MianshibaAiBackendApplication.java backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportEnhancementWorkerTest.java
git commit -m "feat: process report enhancements asynchronously"
```

## Task 5: Enhancement And Compare API

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewReportCompareService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportCompareServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportCompareServiceImplTest.java`

- [ ] **Step 1: Write comparison tests**

Test expected deltas:

```java
@Test
void compare_shouldReturnScoreDeltas() {
    InterviewReport base = report(1L, 10L, 70, 70, 65, 60, 75);
    InterviewReport target = report(2L, 20L, 82, 85, 80, 70, 78);

    when(reportMapper.selectOne(argThat(query -> true))).thenReturn(base, target);

    InterviewReportCompareVO vo = compareService.compare("Bearer token", 10L, 20L);

    assertThat(vo.getTotalDelta()).isEqualTo(12);
    assertThat(vo.getDimensions()).extracting(InterviewScoreDeltaVO::getKey)
            .containsExactly("accuracy", "clarity", "depth", "matching");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportCompareServiceImplTest
```

Expected: FAIL because comparison service does not exist.

- [ ] **Step 3: Implement comparison service**

Rules:

- Resolve user through `JwtUtils` using the existing pattern from `InterviewServiceImpl`.
- Load both sessions and verify both belong to the user.
- Load reports by `sessionId`; missing report returns `INTERVIEW_NOT_FOUND_ERROR`.
- Return deltas for `accuracy`, `clarity`, `depth`, `matching`.
- If both reports have completed enhancements, compare skill gap `name` values into `newSkillGaps` and `resolvedSkillGaps`.
- `summary` contains three strings: total score change, strongest dimension change, and weakest dimension change.

- [ ] **Step 4: Add controller endpoints**

In `InterviewController` add:

```java
@GetMapping("/session/{sessionId}/report/enhancement")
@Operation(summary = "获取面试报告增强复盘")
public BaseResponse<InterviewReportEnhancementVO> getReportEnhancement(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable("sessionId") Long sessionId) {
    return ResultUtils.success(interviewReportEnhancementService.getEnhancement(authorizationHeader, sessionId));
}

@PostMapping("/session/{sessionId}/report/enhancement/retry")
@Operation(summary = "重试面试报告增强复盘")
public BaseResponse<InterviewReportEnhancementVO> retryReportEnhancement(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable("sessionId") Long sessionId) {
    return ResultUtils.success(interviewReportEnhancementService.retry(authorizationHeader, sessionId));
}

@GetMapping("/reports/compare")
@Operation(summary = "对比两次面试报告")
public BaseResponse<InterviewReportCompareVO> compareReports(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @RequestParam("baseSessionId") Long baseSessionId,
        @RequestParam("targetSessionId") Long targetSessionId) {
    return ResultUtils.success(interviewReportCompareService.compare(authorizationHeader, baseSessionId, targetSessionId));
}
```

Add constructor-injected fields for `InterviewReportEnhancementService` and `InterviewReportCompareService`.

- [ ] **Step 5: Compile and run tests**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewReportCompareServiceImplTest,InterviewReportEnhancementServiceImplTest
.\mvnw.cmd compile
```

Expected: PASS and BUILD SUCCESS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/InterviewReportCompareService.java backend/src/main/java/com/mianshiba/ai/service/impl/InterviewReportCompareServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java backend/src/test/java/com/mianshiba/ai/service/impl/InterviewReportCompareServiceImplTest.java
git commit -m "feat: add report enhancement APIs"
```

## Task 6: Review Analytics API

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/ReviewAnalyticsService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ReviewAnalyticsServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java` only if delegating through the existing statistics service is preferred.
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/ReviewAnalyticsServiceImplTest.java`

- [ ] **Step 1: Write analytics tests**

Verify aggregation from completed enhancements:

```java
@Test
void getReviewAnalytics_shouldAverageRadarAndRankSkillGaps() {
    InterviewReportEnhancement first = enhancement(Map.of("accuracy", 80, "clarity", 70),
            List.of(Map.of("name", "MySQL", "severity", "high")), List.of("练习索引"));
    InterviewReportEnhancement second = enhancement(Map.of("accuracy", 60, "clarity", 90),
            List.of(Map.of("name", "MySQL", "severity", "high"), Map.of("name", "Redis", "severity", "medium")), List.of("练习缓存"));

    when(enhancementMapper.selectList(any())).thenReturn(List.of(first, second));

    ReviewAnalyticsVO vo = reviewAnalyticsService.getReviewAnalytics(1L);

    assertThat(vo.getRadar()).containsEntry("accuracy", 70).containsEntry("clarity", 80);
    assertThat(vo.getTopSkillGaps().get(0)).containsEntry("name", "MySQL");
    assertThat(vo.getLatestActionItems()).contains("练习缓存");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=ReviewAnalyticsServiceImplTest
```

Expected: FAIL because service does not exist.

- [ ] **Step 3: Implement `ReviewAnalyticsService`**

Interface:

```java
public interface ReviewAnalyticsService {
    ReviewAnalyticsVO getReviewAnalytics(Long userId);
}
```

Implementation rules:

- Query completed `InterviewReportEnhancement` rows for user, ordered by `updateTime desc`, max 20.
- Average radar dimensions into integers.
- Count skill gap names and return top 5 maps with `name`, `severity`, `count` as strings.
- Build `recentScoreTrend` from up to 5 latest reports joined by `reportId`; each map has `sessionId`, `score`, `date`.
- `latestActionItems` is from the newest completed enhancement.
- Empty data returns empty maps/lists, not null.

- [ ] **Step 4: Add controller endpoint**

In `StatisticsController`, inject `ReviewAnalyticsService` and add:

```java
@GetMapping("/analytics/review")
@Operation(summary = "获取复盘分析总览")
public BaseResponse<ReviewAnalyticsVO> getReviewAnalytics(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
    Long userId = resolveUserId(authorizationHeader);
    return ResultUtils.success(reviewAnalyticsService.getReviewAnalytics(userId));
}
```

Use the same `resolveUserId` helper already present in `StatisticsController`.

- [ ] **Step 5: Run tests and compile**

Run in `backend/`:

```powershell
.\mvnw.cmd test -Dtest=ReviewAnalyticsServiceImplTest
.\mvnw.cmd compile
```

Expected: PASS and BUILD SUCCESS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/ReviewAnalyticsService.java backend/src/main/java/com/mianshiba/ai/service/impl/ReviewAnalyticsServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java backend/src/main/java/com/mianshiba/ai/model/vo/statistics/ReviewAnalyticsVO.java backend/src/test/java/com/mianshiba/ai/service/impl/ReviewAnalyticsServiceImplTest.java
git commit -m "feat: add review analytics API"
```

## Task 7: Frontend Types, APIs, And Store

**Files:**
- Modify: `frontend/src/types/interview.ts`
- Modify: `frontend/src/types/statistics.ts`
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/api/statistics.ts`
- Modify: `frontend/src/stores/interview.ts`

- [ ] **Step 1: Add interview types**

Add to `frontend/src/types/interview.ts`:

```ts
export type ReportEnhancementStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface InterviewTurnReviewVO {
  id: number
  turnId: number
  question: string
  answerSummary: string | null
  diagnosis: string | null
  excellentAnswer: string | null
  improvedAnswer: string | null
  knowledgePoints: string[]
}

export interface InterviewReportEnhancementVO {
  id: number | null
  sessionId: number
  reportId: number | null
  status: ReportEnhancementStatus
  summary: string | null
  radar: Record<string, number>
  skillGaps: Array<Record<string, string>>
  actionItems: string[]
  errorMessage: string | null
  retryCount: number
  turnReviews: InterviewTurnReviewVO[]
}

export interface InterviewScoreDeltaVO {
  key: string
  label: string
  baseScore: number
  targetScore: number
  delta: number
}

export interface InterviewReportCompareVO {
  baseSessionId: number
  targetSessionId: number
  baseTotalScore: number
  targetTotalScore: number
  totalDelta: number
  dimensions: InterviewScoreDeltaVO[]
  newSkillGaps: string[]
  resolvedSkillGaps: string[]
  summary: string[]
}
```

- [ ] **Step 2: Add statistics type**

Add to `frontend/src/types/statistics.ts`:

```ts
export interface ReviewAnalyticsVO {
  radar: Record<string, number>
  topSkillGaps: Array<Record<string, string>>
  recentScoreTrend: Array<Record<string, string | number>>
  latestActionItems: string[]
}
```

- [ ] **Step 3: Add API calls**

In `frontend/src/api/interview.ts`:

```ts
export function getInterviewReportEnhancement(sessionId: number) {
  return request.get<BaseResponse<InterviewReportEnhancementVO>>(
    `/api/interview/session/${sessionId}/report/enhancement`,
  )
}

export function retryInterviewReportEnhancement(sessionId: number) {
  return request.post<BaseResponse<InterviewReportEnhancementVO>>(
    `/api/interview/session/${sessionId}/report/enhancement/retry`,
  )
}

export function compareInterviewReports(baseSessionId: number, targetSessionId: number) {
  return request.get<BaseResponse<InterviewReportCompareVO>>('/api/interview/reports/compare', {
    params: { baseSessionId, targetSessionId },
  })
}
```

In `frontend/src/api/statistics.ts`:

```ts
export function getReviewAnalytics() {
  return request.get<BaseResponse<ReviewAnalyticsVO>>('/api/statistics/analytics/review')
}
```

- [ ] **Step 4: Add store state/actions**

In `frontend/src/stores/interview.ts`, add refs:

```ts
const currentEnhancement = ref<InterviewReportEnhancementVO | null>(null)
const currentComparison = ref<InterviewReportCompareVO | null>(null)
const enhancementLoading = ref(false)
```

Add actions:

```ts
async function fetchReportEnhancement(sessionId: number) {
  enhancementLoading.value = true
  try {
    const res = await getEnhancementApi(sessionId)
    if (res.data.code === 0) {
      currentEnhancement.value = res.data.data
    }
    return res
  } finally {
    enhancementLoading.value = false
  }
}

async function retryReportEnhancement(sessionId: number) {
  const res = await retryEnhancementApi(sessionId)
  if (res.data.code === 0) {
    currentEnhancement.value = res.data.data
  }
  return res
}

async function compareReports(baseSessionId: number, targetSessionId: number) {
  const res = await compareReportsApi(baseSessionId, targetSessionId)
  if (res.data.code === 0) {
    currentComparison.value = res.data.data
  }
  return res
}
```

- [ ] **Step 5: Run frontend type-check**

Run in `frontend/`:

```powershell
npm run type-check
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/types/interview.ts frontend/src/types/statistics.ts frontend/src/api/interview.ts frontend/src/api/statistics.ts frontend/src/stores/interview.ts
git commit -m "feat: add report enhancement frontend APIs"
```

## Task 8: Frontend Report Page Enhancement

**Files:**
- Modify: `frontend/src/views/interview/InterviewReportPage.vue`

- [ ] **Step 1: Add enhancement loading flow**

In `script setup`, import `onUnmounted`, `ElMessage`, and add polling:

```ts
let enhancementTimer: number | undefined

const enhancement = computed(() => interviewStore.currentEnhancement)
const comparison = computed(() => interviewStore.currentComparison)

function shouldPollEnhancement() {
  return enhancement.value?.status === 'pending' || enhancement.value?.status === 'running'
}

async function loadEnhancement() {
  await interviewStore.fetchReportEnhancement(sessionId.value)
  if (shouldPollEnhancement() && enhancementTimer === undefined) {
    enhancementTimer = window.setInterval(async () => {
      await interviewStore.fetchReportEnhancement(sessionId.value)
      if (!shouldPollEnhancement() && enhancementTimer !== undefined) {
        window.clearInterval(enhancementTimer)
        enhancementTimer = undefined
      }
    }, 5000)
  }
}

async function retryEnhancement() {
  await interviewStore.retryReportEnhancement(sessionId.value)
  ElMessage.success('已重新提交复盘增强任务')
  await loadEnhancement()
}

onUnmounted(() => {
  if (enhancementTimer !== undefined) {
    window.clearInterval(enhancementTimer)
  }
})
```

Update `onMounted`:

```ts
onMounted(async () => {
  await interviewStore.fetchReport(sessionId.value)
  await loadEnhancement()
})
```

- [ ] **Step 2: Add enhancement status section**

Add after AI 总结:

```vue
<NbCard class="interview-report-page__section">
  <div class="interview-report-page__section-header">
    <h3 class="interview-report-page__section-title">复盘增强</h3>
    <el-tag v-if="enhancement" :type="enhancement.status === 'completed' ? 'success' : enhancement.status === 'failed' ? 'danger' : 'warning'">
      {{ enhancement.status }}
    </el-tag>
  </div>
  <p v-if="!enhancement || enhancement.status === 'pending' || enhancement.status === 'running'" class="interview-report-page__summary-text">
    AI 正在生成逐题复盘和优秀回答，稍后会自动刷新。
  </p>
  <template v-else-if="enhancement.status === 'failed'">
    <p class="interview-report-page__summary-text">{{ enhancement.errorMessage || '复盘增强生成失败' }}</p>
    <el-button type="primary" @click="retryEnhancement">重新生成</el-button>
  </template>
  <template v-else>
    <p class="interview-report-page__summary-text">{{ enhancement.summary }}</p>
    <div class="interview-report-page__tags">
      <el-tag v-for="gap in enhancement.skillGaps" :key="gap.name" type="warning">
        {{ gap.name }} · {{ gap.severity }}
      </el-tag>
    </div>
  </template>
</NbCard>
```

- [ ] **Step 3: Render per-turn reviews inside answer review**

Add helper:

```ts
function findTurnReview(turnId: number) {
  return enhancement.value?.turnReviews.find((item) => item.turnId === turnId)
}
```

Inside each collapse item after AI 评价:

```vue
<template v-if="findTurnReview(turn.id)">
  <div class="interview-report-page__turn-block">
    <div class="interview-report-page__turn-label">问题诊断</div>
    <div class="interview-report-page__turn-text">{{ findTurnReview(turn.id)?.diagnosis }}</div>
  </div>
  <div class="interview-report-page__turn-block">
    <div class="interview-report-page__turn-label">优秀回答</div>
    <div class="interview-report-page__turn-text">{{ findTurnReview(turn.id)?.excellentAnswer }}</div>
  </div>
  <div class="interview-report-page__turn-block">
    <div class="interview-report-page__turn-label">我的回答改进版</div>
    <div class="interview-report-page__turn-text">{{ findTurnReview(turn.id)?.improvedAnswer }}</div>
  </div>
</template>
```

- [ ] **Step 4: Add simple previous-report comparison**

Use `interviewStore.sessions` to find the latest completed session before current session. Add a button:

```vue
<el-button v-if="previousCompletedSessionId" @click="compareWithPrevious">
  与上次报告对比
</el-button>
```

Action:

```ts
const previousCompletedSessionId = computed(() => {
  return interviewStore.sessions
    .filter((item) => item.status === 'completed' && item.id !== sessionId.value)
    .sort((a, b) => b.id - a.id)[0]?.id ?? null
})

async function compareWithPrevious() {
  if (!previousCompletedSessionId.value) return
  await interviewStore.compareReports(previousCompletedSessionId.value, sessionId.value)
}
```

Render comparison card if `comparison` exists.

- [ ] **Step 5: Run frontend verification**

Run in `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both PASS.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/views/interview/InterviewReportPage.vue
git commit -m "feat: show enhanced interview report review"
```

## Task 9: Frontend Analytics Enhancement

**Files:**
- Modify: `frontend/src/views/analytics/AnalyticsOverviewPage.vue`

- [ ] **Step 1: Add review analytics fetch**

In `script setup`, import `getReviewAnalytics` and add:

```ts
const reviewAnalytics = ref<ReviewAnalyticsVO | null>(null)

async function loadReviewAnalytics() {
  const res = await getReviewAnalytics()
  if (res.data.code === 0) {
    reviewAnalytics.value = res.data.data
  }
}

onMounted(() => {
  loadOverview()
  loadReviewAnalytics()
})
```

- [ ] **Step 2: Add radar and skill gap sections**

Add cards:

```vue
<NbCard class="analytics-overview-page__section">
  <h3 class="analytics-overview-page__section-title">能力雷达</h3>
  <div v-if="reviewAnalytics && Object.keys(reviewAnalytics.radar).length > 0" class="analytics-overview-page__radar-list">
    <div v-for="(score, key) in reviewAnalytics.radar" :key="key" class="analytics-overview-page__radar-item">
      <span>{{ radarLabel(key) }}</span>
      <el-progress :percentage="score" :stroke-width="12" />
    </div>
  </div>
  <p v-else class="analytics-overview-page__empty">完成带增强复盘的面试后，这里会展示能力雷达。</p>
</NbCard>

<NbCard class="analytics-overview-page__section">
  <h3 class="analytics-overview-page__section-title">Top 技能缺口</h3>
  <div v-if="reviewAnalytics?.topSkillGaps.length" class="analytics-overview-page__tags">
    <el-tag v-for="gap in reviewAnalytics.topSkillGaps" :key="gap.name" type="warning">
      {{ gap.name }} · {{ gap.count }} 次
    </el-tag>
  </div>
  <p v-else class="analytics-overview-page__empty">暂无技能缺口数据。</p>
</NbCard>
```

Helper:

```ts
function radarLabel(key: string) {
  const labels: Record<string, string> = {
    accuracy: '技术准确性',
    clarity: '表达清晰度',
    depth: '项目深度',
    matching: '岗位匹配度',
    systemDesign: '系统设计',
  }
  return labels[key] ?? key
}
```

- [ ] **Step 3: Add recent score trend and action items**

Render `recentScoreTrend` as a simple list and `latestActionItems` as cards. Do not introduce a chart dependency in Phase 2.

- [ ] **Step 4: Run frontend verification**

Run in `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/analytics/AnalyticsOverviewPage.vue
git commit -m "feat: add review analytics dashboard"
```

## Task 10: Full Verification And Documentation Update

**Files:**
- Modify: `AGENTS.md` if project conventions need Phase 2 endpoint notes.
- Modify: `docs/superpowers/plans/2026-06-07-second-phase-phase2-review-enhancement-plan.md` only to mark completed checkboxes if executing manually.

- [ ] **Step 1: Run backend tests**

Run in `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: BUILD SUCCESS. If failures are unrelated to Phase 2, document exact failing test names and error messages before continuing.

- [ ] **Step 2: Run backend package**

Run in `backend/`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run frontend checks**

Run in `frontend/`:

```powershell
npm run type-check
npm run build-only
npm run test:unit
```

Expected: all pass. Existing chunk-size warnings from Vite are acceptable if build exits successfully.

- [ ] **Step 4: Inspect staged and unstaged diff**

Run in repo root:

```powershell
git status --short
git diff --stat
```

Expected: only Phase 2 files are changed for this work. Existing unrelated untracked frontend project files may remain; do not stage them unless they were intentionally changed for Phase 2.

- [ ] **Step 5: Final commit if needed**

If Task 10 changed docs or `AGENTS.md`, commit only those files:

```powershell
git add AGENTS.md docs/superpowers/plans/2026-06-07-second-phase-phase2-review-enhancement-plan.md
git commit -m "docs: update phase two review notes"
```

If no files changed, skip this commit.

---

## Self-Review

- Spec coverage: all Phase 2 spec items are mapped to tasks: async enhancement (Tasks 3-4), per-turn excellent answers (Tasks 2/4/8), report comparison (Task 5/8), ability radar and skill gaps (Task 6/9), retry (Task 3/5/8), no RabbitMQ/Kafka (Task 3 uses Redis Stream).
- Placeholder scan: plan contains no unfinished markers or vague unbounded steps.
- Type consistency: backend VO names match frontend types; API paths match the Phase 2 spec; status values are `pending/running/completed/failed` across backend and frontend.
