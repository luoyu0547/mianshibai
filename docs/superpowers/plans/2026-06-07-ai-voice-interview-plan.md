# AI 语音模拟面试功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为面试吧平台实现基于简历的 AI 技术岗语音模拟面试，支持阿里云 TTS 播报、实时 ASR 识别、AI 追问、面试报告和历史记录。

**Architecture:** 后端新增 `interview` 业务分层和 `speech` 语音服务抽象，REST 接口负责会话/轮次/报告，WebSocket 负责实时 ASR 音频流桥接。前端新增面试记录、创建、房间、报告页面，房间页通过状态机驱动 TTS 播放、录音、实时字幕和回答提交。

**Tech Stack:** Spring Boot 3.5 + Java 17 + MyBatis-Plus + Spring AI ChatClient + WebSocket + 阿里云智能语音交互；Vue 3 + TypeScript + Pinia + Element Plus + Web Audio API。

**Design Spec:** `docs/superpowers/specs/2026-06-07-ai-voice-interview-design.md`

---

## File Structure

### Backend — 新增文件

| 文件 | 职责 |
|------|------|
| `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewSession.java` | 面试会话实体 |
| `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurn.java` | 面试轮次实体 |
| `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewReport.java` | 面试报告实体 |
| `backend/src/main/java/com/mianshiba/ai/mapper/InterviewSessionMapper.java` | 会话 Mapper |
| `backend/src/main/java/com/mianshiba/ai/mapper/InterviewTurnMapper.java` | 轮次 Mapper |
| `backend/src/main/java/com/mianshiba/ai/mapper/InterviewReportMapper.java` | 报告 Mapper |
| `backend/src/main/java/com/mianshiba/ai/model/dto/interview/InterviewCreateRequest.java` | 创建面试请求 |
| `backend/src/main/java/com/mianshiba/ai/model/dto/interview/InterviewAnswerRequest.java` | 提交回答请求 |
| `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewSessionVO.java` | 面试列表/详情基础 VO |
| `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnVO.java` | 轮次 VO |
| `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewQuestionVO.java` | 问题 + TTS 响应 VO |
| `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAnswerResultVO.java` | 提交回答后的下一步 VO |
| `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewReportVO.java` | 报告 VO |
| `backend/src/main/java/com/mianshiba/ai/service/InterviewService.java` | 面试业务接口 |
| `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java` | 面试业务实现 |
| `backend/src/main/java/com/mianshiba/ai/service/SpeechService.java` | 语音服务抽象 |
| `backend/src/main/java/com/mianshiba/ai/service/impl/AliyunSpeechServiceImpl.java` | 阿里云 TTS/ASR 实现骨架 |
| `backend/src/main/java/com/mianshiba/ai/config/SpeechProperties.java` | 语音配置属性 |
| `backend/src/main/java/com/mianshiba/ai/config/WebSocketConfig.java` | WebSocket 注册配置 |
| `backend/src/main/java/com/mianshiba/ai/websocket/InterviewAsrWebSocketHandler.java` | 实时 ASR WebSocket 处理器 |
| `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java` | 面试 REST 控制器 |

### Backend — 修改文件

| 文件 | 变更 |
|------|------|
| `backend/src/main/resources/sql/init.sql` | 追加 3 张 interview 表 |
| `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` | 追加面试和语音错误码 |
| `backend/src/main/resources/application.yml` | 追加 `app.speech` 配置 |
| `backend/pom.xml` | 追加 WebSocket 与阿里云语音 SDK 依赖 |

### Backend — 测试文件

| 文件 | 职责 |
|------|------|
| `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewServiceImplTest.java` | 面试业务单元测试 |
| `backend/src/test/java/com/mianshiba/ai/controller/InterviewControllerTest.java` | REST 接口测试 |
| `backend/src/test/java/com/mianshiba/ai/websocket/InterviewAsrWebSocketHandlerTest.java` | WebSocket 校验测试 |
| `backend/src/test/java/com/mianshiba/ai/service/impl/AliyunSpeechServiceImplTest.java` | 语音服务 mock 测试 |

### Frontend — 新增文件

| 文件 | 职责 |
|------|------|
| `frontend/src/types/interview.ts` | 面试类型定义 |
| `frontend/src/api/interview.ts` | 面试 API 调用 |
| `frontend/src/stores/interview.ts` | 面试 Pinia Store |
| `frontend/src/utils/audio/pcm.ts` | Float32 转 16kHz Int16 PCM |
| `frontend/src/utils/audio/asrClient.ts` | ASR WebSocket 客户端 |
| `frontend/src/views/interview/InterviewListPage.vue` | 面试记录列表页 |
| `frontend/src/views/interview/InterviewNewPage.vue` | 创建面试页 |
| `frontend/src/views/interview/InterviewRoomPage.vue` | 语音面试房间页 |
| `frontend/src/views/interview/InterviewReportPage.vue` | 面试报告页 |

### Frontend — 修改文件

| 文件 | 变更 |
|------|------|
| `frontend/src/router/index.ts` | 新增 4 条 `/interview` 路由 |
| `frontend/src/views/home/HomePage.vue` | “开始面试”和“面试记录”按钮接入路由 |
| `frontend/src/layouts/MainLayout.vue` | 导航新增“模拟面试”入口 |

---

## Task 1: Backend Schema and Error Codes

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- Test: `backend/src/test/java/com/mianshiba/ai/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 追加 SQL 表结构**

在 `backend/src/main/resources/sql/init.sql` 末尾追加设计文档第 5 节中的 `interview_session`、`interview_turn`、`interview_report` 三张表，使用 `CREATE TABLE IF NOT EXISTS`，字段和索引与设计文档保持一致。

- [ ] **Step 2: 追加错误码**

在 `ErrorCode.java` 的 AI 简历错误码之后、`SYSTEM_ERROR` 之前插入：

```java
INTERVIEW_NOT_FOUND_ERROR(40410, "面试不存在"),
INTERVIEW_STATUS_ERROR(40010, "面试状态异常"),
INTERVIEW_TURN_ERROR(40011, "面试轮次异常"),
SPEECH_SERVICE_ERROR(50010, "语音服务调用失败"),
SPEECH_RECOGNITION_ERROR(50011, "语音识别失败"),
SPEECH_SYNTHESIS_ERROR(50012, "语音合成失败"),
```

- [ ] **Step 3: 运行现有异常测试**

Run: `.\mvnw.cmd test -Dtest=GlobalExceptionHandlerTest`

Expected: 测试通过，证明新增错误码没有破坏异常响应。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java
git commit -m "feat(interview): add interview schema and error codes"
```

## Task 2: Backend Entities, Mappers, DTOs, and VOs

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewSession.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurn.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewReport.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewSessionMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewTurnMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/InterviewReportMapper.java`
- Create: DTO/VO files under `backend/src/main/java/com/mianshiba/ai/model/dto/interview/` and `backend/src/main/java/com/mianshiba/ai/model/vo/interview/`

- [ ] **Step 1: Create entity classes**

Use the existing `Resume` entity style: `@Data`, `@TableName`, `@TableId(type = IdType.AUTO)`, `@TableLogic(value = "0", delval = "1")` for logical-delete entities. Map JSON columns to `List<String>` for `InterviewReport.suggestions` following the existing MyBatis-Plus JSON mapping style used by `ResumeSection.sectionData`.

- [ ] **Step 2: Create mapper interfaces**

Each mapper extends `BaseMapper<T>`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.InterviewSession;

public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}
```

Repeat for `InterviewTurn` and `InterviewReport`.

- [ ] **Step 3: Create request DTOs**

`InterviewCreateRequest` fields:

```java
@NotNull(message = "简历 id 不能为空")
private Long resumeId;

@NotBlank(message = "目标岗位不能为空")
@Size(max = 128, message = "目标岗位过长")
private String targetPosition;

@Size(max = 128, message = "技术方向过长")
private String techDirection;
```

`InterviewAnswerRequest` fields:

```java
@NotBlank(message = "回答内容不能为空")
private String answerText;

@Min(value = 0, message = "回答时长不能为负数")
private Integer answerDurationSeconds;
```

- [ ] **Step 4: Create VO classes**

Create these VOs with `@Data`:

```text
InterviewSessionVO: id, resumeId, title, interviewType, targetPosition, techDirection, totalQuestions, currentQuestionNo, status, startedAt, endedAt, createTime, updateTime
InterviewTurnVO: id, sessionId, questionNo, turnType, questionText, answerText, aiFeedback, answerDurationSeconds, createTime, updateTime
InterviewQuestionVO: turnId, questionNo, turnType, questionText, ttsAudioBase64
InterviewAnswerResultVO: nextAction, turn, reportId
InterviewReportVO: id, sessionId, totalScore, accuracyScore, clarityScore, depthScore, matchingScore, summary, suggestions, turns, createTime
```

- [ ] **Step 5: Compile backend**

Run: `.\mvnw.cmd test -DskipTests`

Expected: 编译通过。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/mianshiba/ai/model/entity backend/src/main/java/com/mianshiba/ai/mapper backend/src/main/java/com/mianshiba/ai/model/dto/interview backend/src/main/java/com/mianshiba/ai/model/vo/interview
git commit -m "feat(interview): add interview data models"
```

## Task 3: Speech Service Abstraction and Configuration

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/mianshiba/ai/config/SpeechProperties.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/SpeechService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AliyunSpeechServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/AliyunSpeechServiceImplTest.java`

- [ ] **Step 1: Add dependencies**

Add Spring WebSocket dependency first. Keep Alibaba NLS integration behind `SpeechService` so the rest of the system can compile and be tested without real credentials or network calls:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

For Alibaba NLS, implement the concrete SDK call only inside `AliyunSpeechServiceImpl`. If the Maven mirror cannot resolve the official Alibaba Cloud NLS SDK during implementation, keep the `AliyunSpeechServiceImpl` method boundaries and tests in place, throw `SPEECH_SERVICE_ERROR` when credentials are configured but SDK support is unavailable, and complete the interview flow with mocked `SpeechService` tests. This keeps the feature architecture correct while isolating provider-specific dependency risk.

- [ ] **Step 2: Add configuration**

Append to `application.yml`:

```yaml
app:
  speech:
    provider: aliyun
    aliyun:
      app-key: ${ALIYUN_NLS_APP_KEY:}
      region: ${ALIYUN_NLS_REGION:cn-shanghai}
```

- [ ] **Step 3: Create `SpeechProperties`**

Implement `@ConfigurationProperties(prefix = "app.speech")` with fields `provider` and nested `Aliyun` containing `appKey` and `region`.

- [ ] **Step 4: Create `SpeechService`**

Define:

```java
public interface SpeechService {
    String synthesizeToBase64(String text);

    AsrStreamSession createAsrStreamSession(Consumer<String> onPartial,
                                            Consumer<String> onFinal,
                                            Consumer<Throwable> onError);

    interface AsrStreamSession extends AutoCloseable {
        void start();
        void sendAudio(byte[] audio);
        void stop();
        @Override
        void close();
    }
}
```

- [ ] **Step 5: Implement `AliyunSpeechServiceImpl`**

Implement TTS and ASR behind the interface. Convert Alibaba SDK exceptions to `BusinessException` with `SPEECH_SYNTHESIS_ERROR` or `SPEECH_RECOGNITION_ERROR`. Do not persist audio. For first compile-safe implementation, allow an internal fallback that returns an empty Base64 string when `appKey` is blank only in test profile; production profile must throw `SPEECH_SERVICE_ERROR` if credentials are missing.

- [ ] **Step 6: Test speech service failure mapping**

Create a unit test that constructs the service with blank production config and asserts `synthesizeToBase64("hello")` throws `BusinessException` with `SPEECH_SERVICE_ERROR`.

Run: `.\mvnw.cmd test -Dtest=AliyunSpeechServiceImplTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/mianshiba/ai/config/SpeechProperties.java backend/src/main/java/com/mianshiba/ai/service/SpeechService.java backend/src/main/java/com/mianshiba/ai/service/impl/AliyunSpeechServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/AliyunSpeechServiceImplTest.java
git commit -m "feat(interview): add speech service abstraction"
```

## Task 4: Interview Service Core Logic

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/InterviewService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewServiceImplTest.java`

- [ ] **Step 1: Write service interface**

```java
public interface InterviewService {
    InterviewSessionVO createSession(String authorizationHeader, InterviewCreateRequest request);
    InterviewQuestionVO startSession(String authorizationHeader, Long sessionId);
    InterviewAnswerResultVO submitAnswer(String authorizationHeader, Long sessionId, Long turnId, InterviewAnswerRequest request);
    InterviewSessionVO getSession(String authorizationHeader, Long sessionId);
    List<InterviewSessionVO> listSessions(String authorizationHeader);
    InterviewReportVO getReport(String authorizationHeader, Long sessionId);
    void cancelSession(String authorizationHeader, Long sessionId);
}
```

- [ ] **Step 2: Implement ownership checks**

Reuse the existing JWT resolving pattern from `ResumeServiceImpl`: resolve token, load user, reject deleted/disabled users, verify session user id and resume owner.

- [ ] **Step 3: Implement create session**

Rules:

```text
resume must exist and belong to current user
title = targetPosition + " 技术模拟面试"
interviewType = technical
totalQuestions = 5
currentQuestionNo = 0
status = created
```

- [ ] **Step 4: Implement start session**

Rules:

```text
only created or in_progress with no answered turn can start
set status = in_progress
set startedAt if null
generate first main question using ChatClient
create interview_turn questionNo = 1, turnType = main
currentQuestionNo = 1
call SpeechService.synthesizeToBase64(questionText)
return InterviewQuestionVO
```

- [ ] **Step 5: Implement submit answer**

Rules:

```text
turn must belong to session
turn answerText must be blank before submit
save answerText and answerDurationSeconds
if current main question has no follow_up and AI decision is FOLLOW_UP, create follow_up turn with same questionNo
else if currentQuestionNo < totalQuestions, create next main turn and increment currentQuestionNo
else create report and mark session completed
```

- [ ] **Step 6: Implement report generation**

Use ChatClient to produce JSON matching `InterviewReportVO` score fields. Parse JSON using `ObjectMapper`; on parse failure throw `AI_RESPONSE_PARSE_ERROR`.

- [ ] **Step 7: Unit test core flow**

Mock mappers, `JwtUtils`, `UserMapper`, `ChatClient`, and `SpeechService`. Verify:

```text
createSession rejects foreign resume
startSession creates first turn and calls TTS
submitAnswer saves answer and creates next turn
submitAnswer on final question creates report and status completed
```

Run: `.\mvnw.cmd test -Dtest=InterviewServiceImplTest`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/mianshiba/ai/service/InterviewService.java backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/InterviewServiceImplTest.java
git commit -m "feat(interview): implement interview service flow"
```

## Task 5: Interview REST Controller

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/controller/InterviewControllerTest.java`

- [ ] **Step 1: Implement controller endpoints**

Create `@RestController`, `@RequestMapping("/api/interview")`, `@RequiredArgsConstructor`, `@Tag(name = "AI 模拟面试接口")`.

Endpoints:

```text
POST /session
POST /session/{sessionId}/start
POST /session/{sessionId}/turn/{turnId}/answer
GET /session/{sessionId}
GET /session/list
GET /session/{sessionId}/report
POST /session/{sessionId}/cancel
```

Each endpoint reads optional `Authorization` header and delegates to `InterviewService`.

- [ ] **Step 2: Controller tests**

Use `@WebMvcTest(InterviewController.class)` with mocked `InterviewService`. Assert:

```text
POST /api/interview/session with valid body returns code 0
POST /api/interview/session with missing resumeId returns validation error
GET /api/interview/session/list returns list
POST /api/interview/session/1/cancel returns code 0
```

- [ ] **Step 3: Run controller tests**

Run: `.\mvnw.cmd test -Dtest=InterviewControllerTest`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java backend/src/test/java/com/mianshiba/ai/controller/InterviewControllerTest.java
git commit -m "feat(interview): expose interview REST APIs"
```

## Task 6: WebSocket ASR Handler

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/config/WebSocketConfig.java`
- Create: `backend/src/main/java/com/mianshiba/ai/websocket/InterviewAsrWebSocketHandler.java`
- Test: `backend/src/test/java/com/mianshiba/ai/websocket/InterviewAsrWebSocketHandlerTest.java`

- [ ] **Step 1: Register handler**

Register `/ws/interview/asr` via `WebSocketConfigurer`. Allow origins matching current CORS policy used by frontend development.

- [ ] **Step 2: Implement query parsing and validation**

Parse `sessionId`, `turnId`, and `token` from URI query. Validate token using `JwtUtils`; validate session and turn via mappers or a small method in `InterviewService` such as `validateAsrAccess(userId, sessionId, turnId)`.

- [ ] **Step 3: Bridge audio frames**

On text `START`, create `SpeechService.AsrStreamSession`. On binary message, send bytes to ASR session. On text `END`, stop ASR session and close resources. Return JSON messages `ASR_PARTIAL`, `ASR_FINAL`, and `ERROR`.

- [ ] **Step 4: Handler tests**

Use mocked `SpeechService` and `WebSocketSession`. Verify:

```text
missing token closes session or sends ERROR
START creates ASR session
binary message forwards audio bytes
END stops ASR session
```

- [ ] **Step 5: Run WebSocket tests**

Run: `.\mvnw.cmd test -Dtest=InterviewAsrWebSocketHandlerTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/mianshiba/ai/config/WebSocketConfig.java backend/src/main/java/com/mianshiba/ai/websocket/InterviewAsrWebSocketHandler.java backend/src/test/java/com/mianshiba/ai/websocket/InterviewAsrWebSocketHandlerTest.java
git commit -m "feat(interview): add ASR websocket bridge"
```

## Task 7: Frontend Types, API, Store, and Routes

**Files:**
- Create: `frontend/src/types/interview.ts`
- Create: `frontend/src/api/interview.ts`
- Create: `frontend/src/stores/interview.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/home/HomePage.vue`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Create TypeScript types**

Define types matching backend VOs: `InterviewSessionVO`, `InterviewTurnVO`, `InterviewQuestionVO`, `InterviewAnswerResultVO`, `InterviewReportVO`, `InterviewCreateRequest`, `InterviewAnswerRequest`, and union types for `InterviewStatus`, `InterviewNextAction`, `InterviewTurnType`.

- [ ] **Step 2: Create API module**

Implement:

```ts
createInterviewSession(data)
startInterviewSession(sessionId)
submitInterviewAnswer(sessionId, turnId, data)
getInterviewSession(sessionId)
listInterviewSessions()
getInterviewReport(sessionId)
cancelInterviewSession(sessionId)
```

- [ ] **Step 3: Create Pinia store**

Store state: `sessions`, `currentSession`, `currentQuestion`, `currentReport`, `loading`. Actions wrap API functions and update state on success.

- [ ] **Step 4: Add routes**

Add authenticated routes:

```ts
{ path: '/interview', name: 'InterviewList', component: () => import('@/views/interview/InterviewListPage.vue'), meta: { requiresAuth: true } }
{ path: '/interview/new', name: 'InterviewNew', component: () => import('@/views/interview/InterviewNewPage.vue'), meta: { requiresAuth: true } }
{ path: '/interview/:id/room', name: 'InterviewRoom', component: () => import('@/views/interview/InterviewRoomPage.vue'), meta: { requiresAuth: true } }
{ path: '/interview/:id/report', name: 'InterviewReport', component: () => import('@/views/interview/InterviewReportPage.vue'), meta: { requiresAuth: true } }
```

- [ ] **Step 5: Wire home buttons and nav**

Make 首页“立即开始” route to `/interview/new` and “查看记录” route to `/interview`. Add MainLayout nav link to `/interview`.

- [ ] **Step 6: Type-check**

Run: `npm run type-check`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/types/interview.ts frontend/src/api/interview.ts frontend/src/stores/interview.ts frontend/src/router/index.ts frontend/src/views/home/HomePage.vue frontend/src/layouts/MainLayout.vue
git commit -m "feat(interview): add frontend interview APIs and routes"
```

## Task 8: Frontend Audio and ASR Utilities

**Files:**
- Create: `frontend/src/utils/audio/pcm.ts`
- Create: `frontend/src/utils/audio/asrClient.ts`
- Test: `frontend/src/utils/audio/__tests__/pcm.spec.ts`

- [ ] **Step 1: Implement PCM conversion**

Create functions:

```ts
export function downsampleBuffer(input: Float32Array, inputSampleRate: number, outputSampleRate = 16000): Float32Array
export function floatTo16BitPCM(input: Float32Array): ArrayBuffer
```

Rules: clamp values to `[-1, 1]`, map negative values to `value * 0x8000`, positive values to `value * 0x7fff`.

- [ ] **Step 2: Test PCM conversion**

Test:

```ts
expect(floatTo16BitPCM(new Float32Array([0, 1, -1])).byteLength).toBe(6)
expect(downsampleBuffer(new Float32Array(48000), 48000, 16000).length).toBe(16000)
```

- [ ] **Step 3: Implement AsrClient**

Class responsibilities:

```text
connect(sessionId, turnId, token)
sendStart()
sendAudio(buffer)
sendEnd()
close()
onPartial(text)
onFinal(text)
onError(message)
```

Build WebSocket URL from `VITE_API_BASE_URL`, converting `http` to `ws` and `https` to `wss`.

- [ ] **Step 4: Run frontend unit tests**

Run: `npm run test:unit -- src/utils/audio/__tests__/pcm.spec.ts`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/utils/audio/pcm.ts frontend/src/utils/audio/asrClient.ts frontend/src/utils/audio/__tests__/pcm.spec.ts
git commit -m "feat(interview): add audio streaming utilities"
```

## Task 9: Frontend Interview Pages

**Files:**
- Create: `frontend/src/views/interview/InterviewListPage.vue`
- Create: `frontend/src/views/interview/InterviewNewPage.vue`
- Create: `frontend/src/views/interview/InterviewRoomPage.vue`
- Create: `frontend/src/views/interview/InterviewReportPage.vue`
- Test: `frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts`

- [ ] **Step 1: Implement list page**

Use `MainLayout`, `NbCard`, `NbButton`. Fetch sessions on mounted. Render status, total score if report exists, and actions for continue/report/cancel.

- [ ] **Step 2: Implement new page**

Fetch resume list using existing `useResumeStore`. Require selecting a resume. Submit `resumeId`, `targetPosition`, `techDirection`. On success route to `/interview/${id}/room`.

- [ ] **Step 3: Implement room page state machine**

Use `ref<InterviewRoomState>('idle')`. Implement handlers:

```text
startInterview()
playQuestionAudio(question)
startRecording()
stopRecording()
submitAnswer()
handleNextAction(result)
cancelInterview()
```

During `playingQuestion`, disable recording. During `recording`, stream PCM frames via `AsrClient`. Keep `partialText`, `finalText`, and `answerText` visible.

- [ ] **Step 4: Implement report page**

Fetch report by session id. Render total score, 4 dimension progress bars, summary, suggestions, and turn review.

- [ ] **Step 5: Test room state transitions**

Mock API and `AsrClient`. Assert initial state, start interview moves to `playingQuestion`, audio ended moves to `readyToAnswer`, submit answer with `REPORT_READY` routes to report page.

- [ ] **Step 6: Run frontend checks**

Run: `npm run type-check`

Run: `npm run test:unit -- src/views/interview/__tests__/InterviewRoomPage.spec.ts`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/interview frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts
git commit -m "feat(interview): add voice interview pages"
```

## Task 10: Full Verification and Documentation

**Files:**
- Modify: `D:\code_schl\mianshiba\AGENTS.md` or project docs only if command/env docs need updating
- Modify: `docs/superpowers/specs/2026-06-07-ai-voice-interview-design.md` only if implementation discoveries require design clarification

- [ ] **Step 1: Backend full tests**

Run in `backend/`: `.\mvnw.cmd test`

Expected: all tests pass. If environment credentials are missing, tests must still pass because external services are mocked.

- [ ] **Step 2: Frontend verification**

Run in `frontend/`: `npm run type-check`

Run in `frontend/`: `npm run test:unit`

Run in `frontend/`: `npm run build`

Expected: all pass.

- [ ] **Step 3: Manual smoke test**

With backend and frontend running and Aliyun credentials configured:

```text
register/login
create or select resume
open /interview/new
create interview
start interview
hear AI question audio
answer by microphone
see real-time transcript
submit answer
complete 5 questions
view report
open /interview and confirm history exists
```

- [ ] **Step 4: Update environment documentation**

Document required variables:

```text
ALIYUN_ACCESS_KEY_ID
ALIYUN_ACCESS_KEY_SECRET
ALIYUN_NLS_APP_KEY
ALIYUN_NLS_REGION
```

Also document that audio is not persisted and Chrome/Edge is recommended for Web Audio recording.

- [ ] **Step 5: Final diff review**

Run: `git status`

Run: `git diff --stat`

Run: `git diff -- backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai frontend/src`

Confirm no secrets, no generated build artifacts, no unrelated changes.

- [ ] **Step 6: Commit final docs if changed**

```bash
git add docs AGENTS.md
git commit -m "docs(interview): document voice interview setup"
```

Only run the commit if documentation files changed.

---

## Self-Review

- Spec coverage: The plan covers database schema, backend REST APIs, WebSocket ASR, Alibaba speech abstraction, AI generation/scoring flow, frontend pages, state machine, audio utilities, tests, and verification.
- Placeholder scan: No unresolved placeholder text is intended; Alibaba SDK dependency risk is isolated behind `SpeechService` so backend interview flow remains testable without provider credentials.
- Type consistency: DTO/VO names, route paths, API names, status names, `nextAction` values, and state-machine values match the design spec.
