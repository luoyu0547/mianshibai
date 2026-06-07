# 第一优先级优化功能设计文档

> 日期：2026-06-07
> 状态：设计完成，待实现

## 1. 目标

补齐面试吧平台已规划但未完成的关键功能，使系统达到可交付的完整度。覆盖四个独立模块：

1. **简历版本历史** — 每次手动保存时自动创建版本快照
2. **AI 对话记录持久化** — 流式对话结束后入库，支持加载历史
3. **Section "AI 优化" 按钮** — 6 个模块的 AI 优化按钮接入后端接口，支持预览确认
4. **首页统计数据** — 首页三个统计卡片接入真实后端数据

## 2. 首版范围

### 2.1 包含能力

- 简历版本快照在用户手动保存时自动创建（updateResume / addSection / updateSection / deleteSection / sortSections）
- 快照包含完整简历数据（标题 + 模板 + 所有模块），变更摘要由操作类型自动生成
- 前端版本历史抽屉展示版本时间线，从 `GET /api/resume/{id}/versions` 加载
- AI 对话的每一次问答（用户消息 + AI 流式回复）在流式结束后批量入库
- 打开 AI 对话面板时自动加载该简历的历史对话记录
- 6 个 Section 的「AI 优化」按钮连接后端 `POST /api/resume/{id}/ai/optimize-section` 接口
- AI 优化结果以弹窗形式展示新旧内容对比，用户确认后应用
- 首页统计（已完成面试数、总题数、练习天数）通过 `GET /api/statistics/home` 实时计算

### 2.2 不包含能力

- 不做版本快照的恢复/回滚功能（仅展示）
- 不做版本快照的差异对比（diff）
- 不做对话记录的编辑/删除
- 不做 AI 优化的批量/一键全模块优化
- 不做统计数据的图表展示
- 不做统计数据的趋势分析（周/月对比）

## 3. 总体架构

### 3.1 模块一：简历版本历史

```text
frontend/
├── VersionHistory.vue         [已有，无需改动]
├── api/resume.ts               [已有 getResumeVersions，无需改动]
└── types/resume.ts             [已有 VersionVO，无需改动]

backend/
├── ResumeController            [修改：5 个方法编排版本保存]
├── ResumeService               [不变]
├── ResumeVersionService        [新增接口]
├── ResumeVersionServiceImpl    [新增实现]
├── ResumeVersionMapper         [已有]
├── ResumeVersion (Entity)      [已有]
├── ResumeMapper                [已有]
└── VersionVO                   [已有]
```

### 3.2 模块二：AI 对话记录持久化

```text
frontend/
├── AiChatPanel.vue             [修改：onMounted 加载历史]
├── api/resume.ts               [新增 getChatHistory]
└── types/resume.ts             [已有 ChatMessage，新增 ChatMessageVO]

backend/
├── ResumeAiController          [新增 GET /{id}/chat/history]
├── ResumeAiService             [新增 getChatHistory]
├── ResumeAiServiceImpl         [修改：chatStream 末尾入库]
├── ResumeChatMessageMapper     [已有]
├── ResumeChatMessage (Entity)  [已有]
└── ChatMessageVO               [新增]
```

### 3.3 模块三：Section "AI 优化" 按钮

```text
frontend/
├── ResumeEditPage.vue          [修改：6 个按钮绑定点击]
├── AiOptimizeDialog.vue        [新增：优化对比弹窗]
├── api/resume.ts               [已有 aiOptimizeSection，无需改动]
└── types/resume.ts             [已有 AiOptimizeRequest，无需改动]

backend/
└── [无变更，POST /api/resume/{id}/ai/optimize-section 已完整实现]
```

### 3.4 模块四：首页统计数据

```text
frontend/
├── HomePage.vue                [修改：onMounted 调统计 API]
├── api/                         [新增 statistics.ts]
└── types/                       [新增 HomeStatsVO 类型]

backend/
├── StatisticsController         [新增]
├── StatisticsService            [新增接口]
├── StatisticsServiceImpl        [新增实现]
├── InterviewSessionMapper       [已有]
├── InterviewTurnMapper          [已有]
└── HomeStatsVO                  [新增]
```

## 4. 详细设计

### 4.1 模块一：简历版本历史

#### 4.1.1 数据模型

```sql
-- resume_version 表（已在 init.sql 中定义，无需变更）
CREATE TABLE IF NOT EXISTS resume_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resume_id BIGINT NOT NULL,
  version INT NOT NULL,
  snapshot JSON NOT NULL,          -- 完整简历快照
  change_summary VARCHAR(256) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resume_version (resume_id, version)
);
```

#### 4.1.2 快照数据结构

```json
{
  "title": "Java 后端工程师 - 简历",
  "templateType": "minimal_tech",
  "sections": [
    {
      "sectionType": "basic",
      "sectionData": { "name": "张三", "email": "zhang@example.com", ... },
      "sortOrder": 0
    },
    {
      "sectionType": "education",
      "sectionData": { "school": "北京大学", "major": "计算机科学", ... },
      "sortOrder": 1
    }
  ]
}
```

#### 4.1.3 ResumeVersionService 接口

```java
public interface ResumeVersionService {
    void saveSnapshot(Long resumeId, String changeSummary);
    List<VersionVO> listVersions(Long resumeId);
}
```

- `saveSnapshot`：获取 Resume + 所有 ResumeSection → 构造 snapshot Map → 计算下一版本号（`MAX(version) + 1` 或默认 1）→ 写入 `resume_version`
- `listVersions`：`SELECT id, version, change_summary, create_time FROM resume_version WHERE resume_id = ? ORDER BY create_time DESC` → 映射为 `List<VersionVO>`

#### 4.1.4 Controller 编排

```java
// 示例：updateResume 方法新增版本保存
@PutMapping("/{id}")
public BaseResponse<ResumeVO> updateResume(...) {
    ResumeVO result = resumeService.updateResume(authorizationHeader, id, request);
    resumeVersionService.saveSnapshot(id, "更新了简历基本信息");
    return ResultUtils.success(result);
}

// 已有的版本查询端点（前端已调用）
@GetMapping("/{id}/versions")
public BaseResponse<List<VersionVO>> getVersions(@PathVariable("id") Long id) {
    return ResultUtils.success(resumeVersionService.listVersions(id));
}
```

涉及编排的方法（共 5 个）：`updateResume`、`addSection`、`updateSection`、`deleteSection`、`sortSections`。

变更摘要映射：
- `updateResume` → "更新了简历基本信息"
- `addSection` → "添加了{sectionType}模块"
- `updateSection` → "更新了{sectionType}模块"
- `deleteSection` → "删除了{sectionType}模块"
- `sortSections` → "调整了模块排序"

#### 4.1.5 事务处理

`ResumeService` 的方法已标记 `@Transactional`，在方法返回时事务已提交。`ResumeVersionService.saveSnapshot()` 在 Controller 中调用时处于新的事务边界（Spring 默认事务传播），版本保存失败不影响已成功的业务操作——异常被 Controller 层 catch 并记录日志。

#### 4.1.6 前端

`VersionHistory.vue` 已完整实现（组件 + `getResumeVersions` API + `VersionVO` 类型），无需前端改动。

### 4.2 模块二：AI 对话记录持久化

#### 4.2.1 数据模型

```sql
-- resume_chat_message 表（已在 init.sql 中定义，无需变更）
CREATE TABLE IF NOT EXISTS resume_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resume_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,              -- 'user' | 'assistant'
  content TEXT NOT NULL,
  related_section_type VARCHAR(32) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_resume_id (resume_id)
);
```

#### 4.2.2 chatStream 修改

```java
@Override
public Flux<String> chatStream(String authorizationHeader, Long resumeId, String message) {
    Long userId = resolveUserId(authorizationHeader);
    getResumeAndCheckOwner(resumeId, userId);

    // 1. 用户消息立即入库
    ResumeChatMessage userMsg = new ResumeChatMessage();
    userMsg.setResumeId(resumeId);
    userMsg.setRole("user");
    userMsg.setContent(message);
    chatMessageMapper.insert(userMsg);

    List<ResumeSection> sections = resumeSectionMapper.selectList(...);
    String sectionsSummary = buildSectionsSummary(sections);
    String systemPrompt = String.format(CHAT_SYSTEM_PROMPT, sectionsSummary);

    // 2. 流式返回 + 结束时保存 AI 回复
    StringBuilder fullResponse = new StringBuilder();
    return chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .stream()
            .content()
            .doOnNext(fullResponse::append)   // 收集完整文本
            .doOnComplete(() -> {
                ResumeChatMessage assistantMsg = new ResumeChatMessage();
                assistantMsg.setResumeId(resumeId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(fullResponse.toString());
                chatMessageMapper.insert(assistantMsg);
            })
            .doOnError(e -> {
                ResumeChatMessage errorMsg = new ResumeChatMessage();
                errorMsg.setResumeId(resumeId);
                errorMsg.setRole("assistant");
                errorMsg.setContent(fullResponse + "\n[对话异常中断]");
                chatMessageMapper.insert(errorMsg);
            });
}
```

#### 4.2.3 ChatMessageVO

```java
@Data
public class ChatMessageVO implements Serializable {
    private Long id;
    private String role;       // "user" | "assistant"
    private String content;
    private String relatedSectionType;
    private LocalDateTime createTime;
}
```

#### 4.2.4 getChatHistory 实现

```java
@Override
public List<ChatMessageVO> getChatHistory(Long resumeId) {
    List<ResumeChatMessage> messages = chatMessageMapper.selectList(
        Wrappers.lambdaQuery(ResumeChatMessage.class)
            .eq(ResumeChatMessage::getResumeId, resumeId)
            .orderByAsc(ResumeChatMessage::getCreateTime)
    );
    return messages.stream().map(m -> {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(m.getId());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setRelatedSectionType(m.getRelatedSectionType());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }).collect(Collectors.toList());
}
```

#### 4.2.5 Controller 新增端点

```java
@GetMapping("/{resumeId}/chat/history")
@Operation(summary = "获取 AI 对话历史")
public BaseResponse<List<ChatMessageVO>> getChatHistory(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable("resumeId") Long resumeId) {
    return ResultUtils.success(resumeAiService.getChatHistory(resumeId));
}
```

#### 4.2.6 前端修改

`AiChatPanel.vue` 改动：

```typescript
// 新增 onMounted 加载历史
onMounted(async () => {
  if (props.resumeId) {
    const res = await getChatHistory(props.resumeId)
    if (res.data.code === 0) {
      messages.value = res.data.data.map((m: ChatMessageVO) => ({
        role: m.role as 'user' | 'assistant',
        content: m.content,
      }))
    }
  }
})
```

`api/resume.ts` 新增：
```typescript
export function getChatHistory(resumeId: number) {
  return request.get<BaseResponse<ChatMessageVO[]>>(`/api/resume/${resumeId}/chat/history`)
}
```

`types/resume.ts` 新增：
```typescript
export interface ChatMessageVO {
  id: number
  role: string
  content: string
  relatedSectionType?: string
  createTime: string
}
```

### 4.3 模块三：Section "AI 优化" 按钮

#### 4.3.0 后端微调

`AiOptimizeRequest.sectionId` 当前标记为 `@NotNull`，但未保存的新 Section 没有 ID。需移除 `@NotNull` 约束，`sectionId` 在 AI 优化 prompt 中未使用，不影响逻辑。

前端 API 传递时，已有 Section 传真实 ID，新 Section 传 `0`。

#### 4.3.1 AiOptimizeDialog 组件

**Props：**
```typescript
interface Props {
  visible: boolean
  resumeId: number
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sectionLabel: string  // 如 "基本信息"，用于标题
}
```

**Emits：**
```typescript
interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'applied', sectionType: SectionType, data: Record<string, unknown>): void
}
```

**内部流程：**
1. `watch(visible, ...)` 当 visible 为 true 时调 `aiOptimizeSection(resumeId, { sectionType, sectionData, sectionId: 0 })`
2. 展示 loading 状态
3. API 返回后，左栏渲染原始 `sectionData`，右栏渲染优化后的结果
4. 右栏数据存储为本地 `ref`，使用 `v-model` 绑定到对应 Section Editor 组件
5. 用户点「应用」→ emit `applied` + 关闭弹窗
6. 用户点「取消」→ 直接关闭

**模板结构：**
```html
<el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)"
           title="AI 优化 — {sectionLabel}" width="800px" :close-on-click-modal="false">
  <div v-if="loading" class="optimize-loading">AI 正在优化中...</div>
  <div v-else class="optimize-compare">
    <div class="optimize-compare__side optimize-compare__side--original">
      <h4>原始内容</h4>
      <component :is="readonlyEditor" :model-value="sectionData" readonly />
    </div>
    <div class="optimize-compare__side optimize-compare__side--optimized">
      <h4>优化后</h4>
      <component :is="editorComponent" v-model="optimizedData" />
    </div>
  </div>
  <template #footer>
    <el-button @click="$emit('update:visible', false)">取消</el-button>
    <el-button type="primary" @click="handleApply">应用优化结果</el-button>
  </template>
</el-dialog>
```

> 注：右栏可编辑，用对应 Section Editor 渲染（如 `BasicInfoEditor`）。左栏只读，用同样的 Editor 但传 `disabled` prop 或直接用纯文本/JSON 展示。

#### 4.3.2 ResumeEditPage 修改

```html
<!-- 6 个 Section 的按钮改为 -->
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('basic')">AI 优化</el-button>
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('education')">AI 优化</el-button>
...
```

```typescript
// 新增状态和方法
const optimizeVisible = ref(false)
const optimizeSectionType = ref<SectionType>('basic')
const optimizeSectionData = ref<Record<string, unknown>>({})
const optimizeSectionLabel = ref('')

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息', education: '教育经历', work: '工作经历',
  project: '项目经历', skills: '技能标签', summary: '自我评价',
}

const sectionDataMap = computed<Record<SectionType, Record<string, unknown>>>(() => ({
  basic: basicData.value,
  education: educationItems.value as unknown as Record<string, unknown>,
  work: workItems.value as unknown as Record<string, unknown>,
  project: projectItems.value as unknown as Record<string, unknown>,
  skills: skillsData.value,
  summary: summaryData.value,
}))

function openOptimize(type: SectionType) {
  optimizeSectionType.value = type
  optimizeSectionLabel.value = sectionLabelMap[type]
  optimizeSectionData.value = sectionDataMap.value[type]
  optimizeVisible.value = true
}

function handleOptimizeApplied(type: SectionType, data: Record<string, unknown>) {
  // 复用 handleExtracted 的逻辑
  handleExtracted(type, data)
}
```

```html
<!-- 模板中新增 -->
<AiOptimizeDialog
  v-model:visible="optimizeVisible"
  :resume-id="resumeId"
  :section-type="optimizeSectionType"
  :section-data="optimizeSectionData"
  :section-label="optimizeSectionLabel"
  @applied="handleOptimizeApplied"
/>
```

### 4.4 模块四：首页统计数据

#### 4.4.1 HomeStatsVO

```java
@Data
public class HomeStatsVO implements Serializable {
    private long completedInterviews;  // 已完成面试数
    private long totalQuestions;       // 总面试题数
    private long practiceDays;         // 练习天数
}
```

#### 4.4.2 StatisticsService

```java
public interface StatisticsService {
    HomeStatsVO getHomeStats(Long userId);
}
```

```java
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;

    @Override
    public HomeStatsVO getHomeStats(Long userId) {
        HomeStatsVO vo = new HomeStatsVO();

        // 已完成面试数
        vo.setCompletedInterviews(sessionMapper.selectCount(
            Wrappers.lambdaQuery(InterviewSession.class)
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getStatus, "completed")
        ));

        // 总面试题数（主问题）
        // 方式：子查询获取用户的所有 session_id，再统计 main 类型 turn
        List<Long> sessionIds = sessionMapper.selectList(
            Wrappers.lambdaQuery(InterviewSession.class)
                .eq(InterviewSession::getUserId, userId)
                .select(InterviewSession::getId)
        ).stream().map(InterviewSession::getId).toList();

        if (!sessionIds.isEmpty()) {
            vo.setTotalQuestions(turnMapper.selectCount(
                Wrappers.lambdaQuery(InterviewTurn.class)
                    .in(InterviewTurn::getSessionId, sessionIds)
                    .eq(InterviewTurn::getTurnType, "main")
            ));
        }

        // 练习天数：不同日期数（基于 interview_session.create_time）
        // MyBatis-Plus 不易直接做 COUNT(DISTINCT DATE(x))，用 Mapper XML 或 BaseMapper.selectList 后 Java 端计算
        if (!sessionIds.isEmpty()) {
            List<InterviewSession> sessions = sessionMapper.selectList(
                Wrappers.lambdaQuery(InterviewSession.class)
                    .eq(InterviewSession::getUserId, userId)
                    .select(InterviewSession::getCreateTime)
            );
            vo.setPracticeDays(sessions.stream()
                    .map(s -> s.getCreateTime().toLocalDate())
                    .distinct()
                    .count());
        }

        return vo;
    }
}
```

> 注：`totalQuestions` 和 `practiceDays` 如果 `sessionIds` 为空则保持默认值 0。

#### 4.4.3 StatisticsController

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
@Tag(name = "统计数据接口")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @GetMapping("/home")
    @Operation(summary = "获取首页统计数据")
    public BaseResponse<HomeStatsVO> getHomeStats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        return ResultUtils.success(statisticsService.getHomeStats(userId));
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        return claims.userId();
    }
}
```

#### 4.4.4 前端修改

**新增 `api/statistics.ts`：**
```typescript
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { HomeStatsVO } from '@/types/statistics'

export function getHomeStats() {
  return request.get<BaseResponse<HomeStatsVO>>('/api/statistics/home')
}
```

**新增 `types/statistics.ts`：**
```typescript
export interface HomeStatsVO {
  completedInterviews: number
  totalQuestions: number
  practiceDays: number
}
```

**修改 `HomePage.vue`：**
```html
<NbCard hoverable class="stat-card">
  <div class="stat-card__value">{{ stats.completedInterviews }}</div>
  <div class="stat-card__label">已完成面试</div>
</NbCard>
<NbCard hoverable class="stat-card">
  <div class="stat-card__value">{{ stats.totalQuestions }}</div>
  <div class="stat-card__label">面试题目</div>
</NbCard>
<NbCard hoverable class="stat-card">
  <div class="stat-card__value">{{ stats.practiceDays }}</div>
  <div class="stat-card__label">练习天数</div>
</NbCard>
```

```typescript
import { getHomeStats } from '@/api/statistics'
import type { HomeStatsVO } from '@/types/statistics'
import { reactive, onMounted } from 'vue'

const stats = reactive<HomeStatsVO>({
  completedInterviews: 0,
  totalQuestions: 0,
  practiceDays: 0,
})

onMounted(async () => {
  try {
    const res = await getHomeStats()
    if (res.data.code === 0) {
      Object.assign(stats, res.data.data)
    }
  } catch {
    // 保持默认值 0
  }
})
```

## 5. 错误处理

| 场景 | 处理方式 |
|------|----------|
| 版本快照保存失败 | 日志记录 + 不影响主保存操作（捕获异常，不抛给前端） |
| AI 对话入库失败 | 日志记录 + 不中断 SSE 流 |
| 对话历史查询为空 | 返回空数组，前端正常展示空面板 |
| AI 优化 API 失败 | el-dialog 中展示错误提示，不关闭弹窗，允许重试 |
| 统计数据查询失败 | 前端 catch 保持默认值 0，不做错误提示 |

## 6. 测试策略

| 模块 | 测试重点 |
|------|----------|
| ResumeVersionService | `saveSnapshot` 快照内容完整性；version 号自增正确；空 section 场景 |
| chatStream 入库 | `doOnComplete` 正常保存；`doOnError` 保存部分内容；并发安全 |
| getChatHistory | 排序正确；空结果处理；大量历史数据性能 |
| StatisticsService | 无面试记录时返回 0；多场面试时计数正确；`practiceDays` 去重正确 |
| AiOptimizeDialog | 弹窗开关逻辑；`applied` 事件数据正确传递；取消不触发 applied |

## 7. 实施顺序

四个模块相互独立，建议并行实施：

```
第 1 天：模块一 (版本历史) + 模块三 (AI 优化按钮)
         ├── 模块一纯后端，无前端改动
         └── 模块三纯前端，无后端改动

第 2 天：模块二 (对话持久化) + 模块四 (首页统计)
         ├── 模块二：后端 chatStream 改造 + 前端加载历史
         └── 模块四：后端统计接口 + 前端 HomePage 接入
```
