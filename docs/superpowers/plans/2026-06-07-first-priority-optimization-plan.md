# 第一优先级优化功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐面试吧平台已规划但未完成的四个关键功能：简历版本历史、AI 对话持久化、Section AI 优化按钮、首页统计。

**Architecture:** 四个模块相互独立。模块一纯后端（ResumeVersionService + Controller 编排），模块二前后端各半（chatStream 入库 + 前端历史加载），模块三纯前端 + 后端微调（AiOptimizeDialog + @NotNull 解绑），模块四前后端各半（StatisticsController + HomePage 接入）。

**Tech Stack:** Spring Boot 3.5 + Java 17 + MyBatis-Plus + Spring AI ChatClient + Reactor Flux；Vue 3 + TypeScript + Element Plus + Pinia。

**Design Spec:** `docs/superpowers/specs/2026-06-07-first-priority-optimization-design.md`

---

## File Structure

### Backend — 新增文件

| 文件 | 职责 |
|------|------|
| `backend/src/main/java/com/mianshiba/ai/service/ResumeVersionService.java` | 版本历史接口 |
| `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeVersionServiceImpl.java` | 版本历史实现：保存快照、查询列表 |
| `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ChatMessageVO.java` | AI 对话历史 VO |
| `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/HomeStatsVO.java` | 首页统计 VO |
| `backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java` | 统计接口 |
| `backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java` | 统计实现 |
| `backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java` | 统计控制器 |

### Backend — 修改文件

| 文件 | 变更 |
|------|------|
| `backend/src/main/java/com/mianshiba/ai/controller/ResumeController.java` | 5 个方法编排版本保存；新增 `GET /{id}/versions` 端点 |
| `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java` | 新增 `getChatHistory` 方法声明 |
| `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java` | `chatStream` 末尾入库；实现 `getChatHistory`；注入 `ResumeChatMessageMapper` |
| `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java` | 新增 `GET /{id}/chat/history` 端点 |
| `backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiOptimizeRequest.java` | 移除 `sectionId` 的 `@NotNull` 约束 |

### Backend — 测试文件

| 文件 | 职责 |
|------|------|
| `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeVersionServiceImplTest.java` | 版本服务单元测试 |
| `backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java` | 统计接口测试 |

### Frontend — 新增文件

| 文件 | 职责 |
|------|------|
| `frontend/src/components/resume/AiOptimizeDialog.vue` | AI 优化对比弹窗组件 |
| `frontend/src/api/statistics.ts` | 统计 API |
| `frontend/src/types/statistics.ts` | 统计类型定义 |

### Frontend — 修改文件

| 文件 | 变更 |
|------|------|
| `frontend/src/api/resume.ts` | 新增 `getChatHistory` 函数 |
| `frontend/src/types/resume.ts` | 新增 `ChatMessageVO` 接口 |
| `frontend/src/components/resume/AiChatPanel.vue` | `onMounted` 加载历史消息 |
| `frontend/src/views/resume/ResumeEditPage.vue` | 6 个按钮绑定点击；集成 `AiOptimizeDialog` |
| `frontend/src/views/home/HomePage.vue` | `onMounted` 调统计 API 替换硬编码 0 |

---

## 模块一：简历版本历史

### Task 1.1: 创建 ResumeVersionService 接口

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/ResumeVersionService.java`

- [ ] **Step 1: 写入接口文件**

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.resume.VersionVO;

import java.util.List;

public interface ResumeVersionService {

    void saveSnapshot(Long resumeId, String changeSummary);

    List<VersionVO> listVersions(Long resumeId);
}
```

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 1.2: 创建 ResumeVersionServiceImpl

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeVersionServiceImpl.java`

- [ ] **Step 1: 写入实现类**

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.ResumeVersion;
import com.mianshiba.ai.model.vo.resume.VersionVO;
import com.mianshiba.ai.service.ResumeVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVersionServiceImpl implements ResumeVersionService {

    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSnapshot(Long resumeId, String changeSummary) {
        try {
            Resume resume = resumeMapper.selectById(resumeId);
            if (resume == null) {
                log.warn("保存版本快照失败：简历 {} 不存在", resumeId);
                return;
            }

            List<ResumeSection> sections = resumeSectionMapper.selectList(
                    Wrappers.lambdaQuery(ResumeSection.class)
                            .eq(ResumeSection::getResumeId, resumeId)
                            .orderByAsc(ResumeSection::getSortOrder));

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("title", resume.getTitle());
            snapshot.put("templateType", resume.getTemplateType());

            List<Map<String, Object>> sectionList = new ArrayList<>();
            for (ResumeSection section : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("sectionType", section.getSectionType());
                sectionMap.put("sectionData", section.getSectionData());
                sectionMap.put("sortOrder", section.getSortOrder());
                sectionList.add(sectionMap);
            }
            snapshot.put("sections", sectionList);

            int nextVersion = computeNextVersion(resumeId);

            ResumeVersion version = new ResumeVersion();
            version.setResumeId(resumeId);
            version.setVersion(nextVersion);
            version.setSnapshot(snapshot);
            version.setChangeSummary(changeSummary != null ? changeSummary : "");
            version.setCreateTime(LocalDateTime.now());

            resumeVersionMapper.insert(version);
            log.info("简历 {} 版本 {} 已保存：{}", resumeId, nextVersion, changeSummary);
        } catch (Exception e) {
            log.error("保存版本快照失败：resumeId={}", resumeId, e);
        }
    }

    @Override
    public List<VersionVO> listVersions(Long resumeId) {
        List<ResumeVersion> versions = resumeVersionMapper.selectList(
                Wrappers.lambdaQuery(ResumeVersion.class)
                        .eq(ResumeVersion::getResumeId, resumeId)
                        .orderByDesc(ResumeVersion::getCreateTime));

        return versions.stream().map(v -> {
            VersionVO vo = new VersionVO();
            vo.setId(v.getId());
            vo.setVersion(v.getVersion());
            vo.setChangeSummary(v.getChangeSummary());
            vo.setCreateTime(v.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private int computeNextVersion(Long resumeId) {
        List<ResumeVersion> existing = resumeVersionMapper.selectList(
                Wrappers.lambdaQuery(ResumeVersion.class)
                        .eq(ResumeVersion::getResumeId, resumeId)
                        .orderByDesc(ResumeVersion::getVersion)
                        .last("LIMIT 1"));
        if (existing.isEmpty()) {
            return 1;
        }
        return existing.get(0).getVersion() + 1;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 1.3: 修改 ResumeController — 新增版本端点 + 编排版本保存

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/ResumeController.java`

- [ ] **Step 1: 注入 ResumeVersionService**

在现有 `private final ResumeService resumeService;` 下方添加：

```java
private final ResumeVersionService resumeVersionService;
```

> 注：由于使用了 `@RequiredArgsConstructor`，Lombok 会自动将 `resumeVersionService` 加入构造函数。

- [ ] **Step 2: 在 5 个方法末尾添加版本保存调用**

`updateResume` 方法（在 `return ResultUtils.success(result);` 前添加）：

```java
resumeVersionService.saveSnapshot(id, "更新了简历基本信息");
```

`addSection` 方法（在 `return ResultUtils.success(result);` 前添加）：

```java
String sectionType = request.getSectionType() != null ? request.getSectionType() : "未知";
resumeVersionService.saveSnapshot(resumeId, "添加了" + sectionType + "模块");
```

`updateSection` 方法（在 `return ResultUtils.success(result);` 前添加）：

```java
String sectionType = section.getSectionType() != null ? section.getSectionType() : "未知";
resumeVersionService.saveSnapshot(resumeId, "更新了" + sectionType + "模块");
```

`deleteSection` 方法（在 `return ResultUtils.success(null);` 前添加）：

```java
String sectionType = section.getSectionType() != null ? section.getSectionType() : "未知";
resumeVersionService.saveSnapshot(resumeId, "删除了" + sectionType + "模块");
```

`sortSections` 方法（在 `return ResultUtils.success(null);` 前添加）：

```java
resumeVersionService.saveSnapshot(resumeId, "调整了模块排序");
```

- [ ] **Step 3: 新增版本历史查询端点**

在 `ResumeController` 类末尾（`sortSections` 之后，类结束 `}` 之前）添加：

```java
@GetMapping("/{resumeId}/versions")
@Operation(summary = "获取简历版本历史")
public BaseResponse<List<VersionVO>> getVersions(@PathVariable("resumeId") Long resumeId) {
    return ResultUtils.success(resumeVersionService.listVersions(resumeId));
}
```

需要新增 import：

```java
import com.mianshiba.ai.model.vo.resume.VersionVO;
```

- [ ] **Step 4: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 1.4: 编写 ResumeVersionServiceImpl 测试

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeVersionServiceImplTest.java`

- [ ] **Step 1: 写入测试类**

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.ResumeVersion;
import com.mianshiba.ai.model.vo.resume.VersionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeVersionServiceImplTest {

    @Mock
    private ResumeVersionMapper resumeVersionMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeSectionMapper resumeSectionMapper;

    @InjectMocks
    private ResumeVersionServiceImpl service;

    @BeforeEach
    void setUp() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setTitle("测试简历");
        resume.setTemplateType("minimal_tech");
        lenient().when(resumeMapper.selectById(1L)).thenReturn(resume);

        ResumeSection section = new ResumeSection();
        section.setSectionType("basic");
        section.setSectionData(java.util.Map.of("name", "张三"));
        section.setSortOrder(0);
        lenient().when(resumeSectionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(section));

        lenient().when(resumeVersionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());
    }

    @Test
    void saveSnapshot_shouldInsertVersion() {
        service.saveSnapshot(1L, "测试变更");

        ArgumentCaptor<ResumeVersion> captor = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeVersionMapper).insert(captor.capture());
        ResumeVersion saved = captor.getValue();
        assertThat(saved.getResumeId()).isEqualTo(1L);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getChangeSummary()).isEqualTo("测试变更");
        assertThat(saved.getSnapshot()).containsKey("title");
        assertThat(saved.getSnapshot()).containsKey("sections");
    }

    @Test
    void saveSnapshot_shouldIncrementVersion() {
        ResumeVersion existing = new ResumeVersion();
        existing.setVersion(3);
        when(resumeVersionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(existing));

        service.saveSnapshot(1L, "再次变更");

        ArgumentCaptor<ResumeVersion> captor = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeVersionMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(4);
    }

    @Test
    void saveSnapshot_shouldNotThrowWhenResumeNotFound() {
        when(resumeMapper.selectById(999L)).thenReturn(null);
        service.saveSnapshot(999L, "不存在");
        verify(resumeVersionMapper, never()).insert(any());
    }

    @Test
    void listVersions_shouldReturnVersionVOList() {
        ResumeVersion v1 = new ResumeVersion();
        v1.setId(1L);
        v1.setVersion(2);
        v1.setChangeSummary("变更A");
        v1.setCreateTime(java.time.LocalDateTime.now());

        ResumeVersion v2 = new ResumeVersion();
        v2.setId(2L);
        v2.setVersion(1);
        v2.setChangeSummary("初始创建");
        v2.setCreateTime(java.time.LocalDateTime.now().minusHours(1));

        when(resumeVersionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(v1, v2));

        List<VersionVO> result = service.listVersions(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(2);
        assertThat(result.get(1).getVersion()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `.\mvnw.cmd test -pl . -Dtest=ResumeVersionServiceImplTest`
Expected: Tests run: 4, Failures: 0, BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/mianshiba/ai/service/ResumeVersionService.java
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeVersionServiceImpl.java
git add backend/src/main/java/com/mianshiba/ai/controller/ResumeController.java
git add backend/src/test/java/com/mianshiba/ai/service/impl/ResumeVersionServiceImplTest.java
git commit -m "feat: add resume version history - save snapshot on manual save"
```

---

## 模块二：AI 对话记录持久化

### Task 2.1: 创建 ChatMessageVO

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ChatMessageVO.java`

- [ ] **Step 1: 写入 VO**

```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String role;
    private String content;
    private String relatedSectionType;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 2.2: 修改 ResumeAiService 接口

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`

- [ ] **Step 1: 添加 getChatHistory 方法声明**

在接口末尾（`}` 前）添加：

```java
List<com.mianshiba.ai.model.vo.resume.ChatMessageVO> getChatHistory(Long resumeId);
```

同时新增 import：

```java
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
import java.util.List;
```

--- 或者直接使用全限定名避免修改 import：

```java
List<com.mianshiba.ai.model.vo.resume.ChatMessageVO> getChatHistory(Long resumeId);
```

> 注：如果 `java.util.List` 已 import，只需新增 `ChatMessageVO` 的 import。

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 2.3: 修改 ResumeAiServiceImpl — chatStream 入库 + getChatHistory

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`

- [ ] **Step 1: 注入 ResumeChatMessageMapper**

在现有字段区域（`private final JwtUtils jwtUtils;` 之后）添加：

```java
private final ResumeChatMessageMapper chatMessageMapper;
```

同时新增 import：

```java
import com.mianshiba.ai.mapper.ResumeChatMessageMapper;
import com.mianshiba.ai.model.entity.ResumeChatMessage;
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
```

- [ ] **Step 2: 修改 chatStream 方法 — 流式结束后入库**

将现有的 `chatStream` 方法体替换为：

```java
@Override
public Flux<String> chatStream(String authorizationHeader, Long resumeId, String message) {
    Long userId = resolveUserId(authorizationHeader);
    getResumeAndCheckOwner(resumeId, userId);

    ResumeChatMessage userMsg = new ResumeChatMessage();
    userMsg.setResumeId(resumeId);
    userMsg.setRole("user");
    userMsg.setContent(message);
    chatMessageMapper.insert(userMsg);

    List<ResumeSection> sections = resumeSectionMapper.selectList(
            Wrappers.lambdaQuery(ResumeSection.class)
                    .eq(ResumeSection::getResumeId, resumeId)
                    .orderByAsc(ResumeSection::getSortOrder));

    String sectionsSummary = buildSectionsSummary(sections);
    String systemPrompt = String.format(CHAT_SYSTEM_PROMPT, sectionsSummary);

    StringBuilder fullResponse = new StringBuilder();

    return chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .stream()
            .content()
            .doOnNext(fullResponse::append)
            .doOnComplete(() -> saveAssistantMessage(resumeId, fullResponse.toString()))
            .doOnError(e -> {
                log.error("AI 对话流异常", e);
                saveAssistantMessage(resumeId, fullResponse + "\n[对话异常中断]");
            });
}

private void saveAssistantMessage(Long resumeId, String content) {
    try {
        ResumeChatMessage assistantMsg = new ResumeChatMessage();
        assistantMsg.setResumeId(resumeId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        chatMessageMapper.insert(assistantMsg);
    } catch (Exception e) {
        log.error("保存 AI 回复失败：resumeId={}", resumeId, e);
    }
}
```

- [ ] **Step 3: 实现 getChatHistory 方法**

在 `chatStream` 方法下方添加：

```java
@Override
public List<ChatMessageVO> getChatHistory(Long resumeId) {
    List<ResumeChatMessage> messages = chatMessageMapper.selectList(
            Wrappers.lambdaQuery(ResumeChatMessage.class)
                    .eq(ResumeChatMessage::getResumeId, resumeId)
                    .orderByAsc(ResumeChatMessage::getCreateTime));

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

- [ ] **Step 4: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 2.4: 修改 ResumeAiController — 新增 chat/history 端点

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java`

- [ ] **Step 1: 新增 GET 端点**

在 `chat` 方法之后、`extractTargetPosition` 方法之前添加：

```java
@GetMapping("/{resumeId}/chat/history")
@Operation(summary = "获取 AI 对话历史")
public BaseResponse<List<ChatMessageVO>> getChatHistory(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable("resumeId") Long resumeId) {
    resumeService.getResumeDetail(authorizationHeader, resumeId);
    return ResultUtils.success(resumeAiService.getChatHistory(resumeId));
}
```

新增 import：

```java
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
import org.springframework.web.bind.annotation.GetMapping;
```

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 2.5: 前端 — 新增 ChatMessageVO 类型 + getChatHistory API

**Files:**
- Modify: `frontend/src/types/resume.ts`
- Modify: `frontend/src/api/resume.ts`

- [ ] **Step 1: 在 types/resume.ts 末尾添加**

```typescript
export interface ChatMessageVO {
  id: number
  role: string
  content: string
  relatedSectionType?: string
  createTime: string
}
```

- [ ] **Step 2: 在 api/resume.ts 中添加函数**

在文件末尾添加：

```typescript
export function getChatHistory(resumeId: number) {
  return request.get<BaseResponse<ChatMessageVO[]>>(`/api/resume/${resumeId}/chat/history`)
}
```

并更新 import，在现有 import 中添加 `ChatMessageVO`：

```typescript
import type {
  // ... 已有类型
  ChatMessageVO,
} from '@/types/resume'
```

> 注：`ChatMessageVO` 在 `AiScoreVO` 之后、`VersionVO` 之前的 import 列表中追加即可。

- [ ] **Step 3: Type-check 验证**

Run: `npm run type-check`
Expected: exit code 0

---

### Task 2.6: 前端 — 修改 AiChatPanel 加载历史消息

**Files:**
- Modify: `frontend/src/components/resume/AiChatPanel.vue`

- [ ] **Step 1: 添加 onMounted + 历史加载逻辑**

在 `<script setup>` 区域，现有 `import { ref, nextTick } from 'vue'` 后追加 `onMounted`：

```typescript
import { ref, nextTick, onMounted } from 'vue'
```

在 `const isLoading = ref(false)` 之后添加：

```typescript
import { getChatHistory } from '@/api/resume'
import type { ChatMessageVO } from '@/types/resume'
```

在 `scrollToBottom` 函数下方添加：

```typescript
onMounted(async () => {
  if (props.resumeId) {
    try {
      const res = await getChatHistory(props.resumeId)
      if (res.data.code === 0 && res.data.data) {
        messages.value = res.data.data.map((m: ChatMessageVO) => ({
          role: m.role as 'user' | 'assistant',
          content: m.content,
        }))
        scrollToBottom()
      }
    } catch {
      // 加载失败不展示错误，保持空白面板
    }
  }
})
```

- [ ] **Step 2: Type-check + lint**

Run: `npm run type-check; if ($?) { npm run lint }`
Expected: exit code 0

---

### Task 2.7: Commit 模块二

```bash
git add backend/src/main/java/com/mianshiba/ai/model/vo/resume/ChatMessageVO.java
git add backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java
git add backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java
git add frontend/src/types/resume.ts
git add frontend/src/api/resume.ts
git add frontend/src/components/resume/AiChatPanel.vue
git commit -m "feat: persist AI chat messages and load history on panel open"
```

---

## 模块三：Section "AI 优化" 按钮

### Task 3.1: 后端微调 — 移除 AiOptimizeRequest.sectionId 的 @NotNull

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiOptimizeRequest.java`

- [ ] **Step 1: 修改**

将：

```java
@NotNull(message = "模块 id 不能为空")
private Long sectionId;
```

改为：

```java
private Long sectionId;
```

同时删除不再需要的 `import jakarta.validation.constraints.NotNull;`。

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 3.2: 前端 — 创建 AiOptimizeDialog.vue

**Files:**
- Create: `frontend/src/components/resume/AiOptimizeDialog.vue`

- [ ] **Step 1: 写入组件**

```vue
<!-- src/components/resume/AiOptimizeDialog.vue -->
<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="'AI 优化 — ' + sectionLabel"
    width="860px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="loading" class="optimize-dialog__loading">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>AI 正在优化中...</span>
    </div>

    <div v-else-if="error" class="optimize-dialog__error">
      <p>{{ error }}</p>
      <el-button type="primary" @click="doOptimize">重试</el-button>
    </div>

    <div v-else class="optimize-dialog__compare">
      <div class="optimize-dialog__side">
        <h4 class="optimize-dialog__side-title">原始内容</h4>
        <div class="optimize-dialog__content optimize-dialog__content--readonly">
          <pre>{{ formatData(sectionData) }}</pre>
        </div>
      </div>
      <div class="optimize-dialog__divider">
        <el-icon :size="20"><Right /></el-icon>
      </div>
      <div class="optimize-dialog__side">
        <h4 class="optimize-dialog__side-title optimize-dialog__side-title--optimized">优化后</h4>
        <div class="optimize-dialog__content">
          <pre>{{ formatData(optimizedData) }}</pre>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :disabled="loading || !!error" @click="handleApply">
        应用优化结果
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Loading, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiOptimizeSection } from '@/api/resume'
import type { SectionType } from '@/types/resume'

const props = defineProps<{
  visible: boolean
  resumeId: number
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sectionLabel: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'applied', sectionType: SectionType, data: Record<string, unknown>): void
}>()

const loading = ref(false)
const error = ref('')
const optimizedData = ref<Record<string, unknown>>({})

watch(() => props.visible, (val) => {
  if (val) {
    doOptimize()
  }
})

async function doOptimize() {
  loading.value = true
  error.value = ''
  try {
    const res = await aiOptimizeSection(props.resumeId, {
      sectionId: 0,
      sectionType: props.sectionType,
      sectionData: props.sectionData,
    })
    if (res.data.code === 0) {
      optimizedData.value = res.data.data as Record<string, unknown>
    } else {
      error.value = '优化失败，请重试'
    }
  } catch {
    error.value = '网络错误，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

function handleApply() {
  emit('applied', props.sectionType, optimizedData.value)
  emit('update:visible', false)
  ElMessage.success('已应用优化结果')
}

function formatData(data: Record<string, unknown>): string {
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}
</script>

<style scoped>
.optimize-dialog__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: var(--nb-muted);
}

.optimize-dialog__error {
  text-align: center;
  padding: 48px 0;
  color: var(--el-color-danger);
}

.optimize-dialog__compare {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.optimize-dialog__side {
  flex: 1;
  min-width: 0;
}

.optimize-dialog__side-title {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-muted);
  margin: 0 0 8px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--nb-border-color, #ddd);
}

.optimize-dialog__side-title--optimized {
  color: var(--nb-primary);
  border-bottom-color: var(--nb-primary);
}

.optimize-dialog__content {
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.optimize-dialog__content pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
}

.optimize-dialog__divider {
  display: flex;
  align-items: center;
  padding-top: 40px;
  color: var(--nb-primary);
  flex-shrink: 0;
}
</style>
```

- [ ] **Step 2: Type-check**

Run: `npm run type-check`
Expected: exit code 0

---

### Task 3.3: 前端 — 修改 ResumeEditPage 集成 AiOptimizeDialog

**Files:**
- Modify: `frontend/src/views/resume/ResumeEditPage.vue`

- [ ] **Step 1: 修改 6 个 Section 的 AI 优化按钮**

将 6 个 `@click.stop` 改为绑定函数。例如基本信息：

```html
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('basic')">AI 优化</el-button>
```

其余 5 个同理：
```html
<!-- education -->
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('education')">AI 优化</el-button>
<!-- work -->
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('work')">AI 优化</el-button>
<!-- project -->
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('project')">AI 优化</el-button>
<!-- skills -->
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('skills')">AI 优化</el-button>
<!-- summary -->
<el-button size="small" text class="ai-btn" @click.stop="openOptimize('summary')">AI 优化</el-button>
```

- [ ] **Step 2: 在模板中添加 AiOptimizeDialog 组件**

在 `</div>` (resume-edit-page__body 闭合) 之前添加：

```html
<AiOptimizeDialog
  v-model:visible="optimizeVisible"
  :resume-id="resumeId"
  :section-type="optimizeSectionType"
  :section-data="optimizeSectionData"
  :section-label="optimizeSectionLabel"
  @applied="handleOptimizeApplied"
/>
```

- [ ] **Step 3: 在 script setup 中添加导入和逻辑**

导入 AiOptimizeDialog：

```typescript
import AiOptimizeDialog from '@/components/resume/AiOptimizeDialog.vue'
```

在 `const activeTab = ref('chat')` 之后添加：

```typescript
const optimizeVisible = ref(false)
const optimizeSectionType = ref<SectionType>('basic')
const optimizeSectionData = ref<Record<string, unknown>>({})
const optimizeSectionLabel = ref('')

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

const sectionDataMap = computed(() => ({
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
  switch (type) {
    case 'basic':
      basicData.value = data
      break
    case 'education':
      educationItems.value = Array.isArray(data) ? data : [data]
      break
    case 'work':
      workItems.value = Array.isArray(data) ? data : [data]
      break
    case 'project':
      projectItems.value = Array.isArray(data) ? data : [data]
      break
    case 'skills':
      skillsData.value = data
      break
    case 'summary':
      summaryData.value = data
      break
  }
}
```

- [ ] **Step 4: Type-check + lint**

Run: `npm run type-check; if ($?) { npm run lint }`
Expected: exit code 0

---

### Task 3.4: Commit 模块三

```bash
git add backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiOptimizeRequest.java
git add frontend/src/components/resume/AiOptimizeDialog.vue
git add frontend/src/views/resume/ResumeEditPage.vue
git commit -m "feat: connect section AI optimize buttons with preview dialog"
```

---

## 模块四：首页统计数据

### Task 4.1: 创建 HomeStatsVO

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/statistics/HomeStatsVO.java`

- [ ] **Step 1: 写入 VO**

```java
package com.mianshiba.ai.model.vo.statistics;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class HomeStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long completedInterviews;
    private long totalQuestions;
    private long practiceDays;
}
```

---

### Task 4.2: 创建 StatisticsService 接口 + 实现

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java`

- [ ] **Step 1: 写入接口**

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;

public interface StatisticsService {
    HomeStatsVO getHomeStats(Long userId);
}
```

- [ ] **Step 2: 写入实现**

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;
import com.mianshiba.ai.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;

    @Override
    public HomeStatsVO getHomeStats(Long userId) {
        HomeStatsVO vo = new HomeStatsVO();

        vo.setCompletedInterviews(sessionMapper.selectCount(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "completed")));

        List<Long> sessionIds = sessionMapper.selectList(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .select(InterviewSession::getId))
                .stream()
                .map(InterviewSession::getId)
                .toList();

        if (!sessionIds.isEmpty()) {
            vo.setTotalQuestions(turnMapper.selectCount(
                    Wrappers.lambdaQuery(InterviewTurn.class)
                            .in(InterviewTurn::getSessionId, sessionIds)
                            .eq(InterviewTurn::getTurnType, "main")));

            List<InterviewSession> sessions = sessionMapper.selectList(
                    Wrappers.lambdaQuery(InterviewSession.class)
                            .eq(InterviewSession::getUserId, userId)
                            .select(InterviewSession::getCreateTime));
            vo.setPracticeDays(sessions.stream()
                    .map(s -> s.getCreateTime().toLocalDate())
                    .distinct()
                    .count());
        }

        return vo;
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 4.3: 创建 StatisticsController

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java`

- [ ] **Step 1: 写入 Controller**

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;
import com.mianshiba.ai.service.StatisticsService;
import com.mianshiba.ai.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        return user.getId();
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `.\mvnw.cmd compile -pl .`
Expected: BUILD SUCCESS

---

### Task 4.4: 编写 StatisticsController 测试

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java`

- [ ] **Step 1: 写入测试**

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;
import com.mianshiba.ai.service.StatisticsService;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private UserMapper userMapper;

    @Test
    void getHomeStats_shouldReturnStats() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUserStatus(0);
        user.setIsDelete(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(jwtUtils.resolveToken(any())).thenReturn("valid-token");
        JwtUtils.JwtUserClaims claims = new JwtUtils.JwtUserClaims(1L, "test");
        when(jwtUtils.parseToken("valid-token")).thenReturn(claims);

        HomeStatsVO stats = new HomeStatsVO();
        stats.setCompletedInterviews(3);
        stats.setTotalQuestions(15);
        stats.setPracticeDays(5);
        when(statisticsService.getHomeStats(anyLong())).thenReturn(stats);

        mockMvc.perform(get("/api/statistics/home")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.completedInterviews").value(3))
                .andExpect(jsonPath("$.data.totalQuestions").value(15))
                .andExpect(jsonPath("$.data.practiceDays").value(5));
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `.\mvnw.cmd test -pl . -Dtest=StatisticsControllerTest`
Expected: Tests run: 1, Failures: 0, BUILD SUCCESS

---

### Task 4.5: 前端 — 创建统计 API + 类型

**Files:**
- Create: `frontend/src/types/statistics.ts`
- Create: `frontend/src/api/statistics.ts`

- [ ] **Step 1: 写入类型定义**

```typescript
// src/types/statistics.ts
export interface HomeStatsVO {
  completedInterviews: number
  totalQuestions: number
  practiceDays: number
}
```

- [ ] **Step 2: 写入 API 函数**

```typescript
// src/api/statistics.ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { HomeStatsVO } from '@/types/statistics'

export function getHomeStats() {
  return request.get<BaseResponse<HomeStatsVO>>('/api/statistics/home')
}
```

---

### Task 4.6: 前端 — 修改 HomePage 接入真实数据

**Files:**
- Modify: `frontend/src/views/home/HomePage.vue`

- [ ] **Step 1: 替换硬编码 0 为响应式数据**

将三个 `stat-card__value` 中的 `0` 改为 `{{ stats.completedInterviews }}`、`{{ stats.totalQuestions }}`、`{{ stats.practiceDays }}`：

```html
<div class="stat-card__value">{{ stats.completedInterviews }}</div>
<div class="stat-card__label">已完成面试</div>
...
<div class="stat-card__value">{{ stats.totalQuestions }}</div>
<div class="stat-card__label">面试题目</div>
...
<div class="stat-card__value">{{ stats.practiceDays }}</div>
<div class="stat-card__label">练习天数</div>
```

- [ ] **Step 2: 在 script setup 中添加数据获取逻辑**

在现有的 `<script setup>` 区域，`import { useUserStore } from '@/stores/user'` 之后添加：

```typescript
import { getHomeStats } from '@/api/statistics'
import type { HomeStatsVO } from '@/types/statistics'
import { reactive, onMounted } from 'vue'
```

在 `const userStore = useUserStore()` 之后添加：

```typescript
const stats = reactive<HomeStatsVO>({
  completedInterviews: 0,
  totalQuestions: 0,
  practiceDays: 0,
})

onMounted(async () => {
  try {
    const res = await getHomeStats()
    if (res.data.code === 0 && res.data.data) {
      Object.assign(stats, res.data.data)
    }
  } catch {
    // 保持默认值 0
  }
})
```

- [ ] **Step 3: Type-check + lint**

Run: `npm run type-check; if ($?) { npm run lint }`
Expected: exit code 0

---

### Task 4.7: Commit 模块四

```bash
git add backend/src/main/java/com/mianshiba/ai/model/vo/statistics/HomeStatsVO.java
git add backend/src/main/java/com/mianshiba/ai/service/StatisticsService.java
git add backend/src/main/java/com/mianshiba/ai/service/impl/StatisticsServiceImpl.java
git add backend/src/main/java/com/mianshiba/ai/controller/StatisticsController.java
git add backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java
git add frontend/src/types/statistics.ts
git add frontend/src/api/statistics.ts
git add frontend/src/views/home/HomePage.vue
git commit -m "feat: add homepage statistics API and real data display"
```

---

## 最终验证

### Task V.1: 后端全量编译 + 测试

```bash
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd test
```

Expected: BUILD SUCCESS, all tests pass

### Task V.2: 前端 type-check + lint + build

```bash
npm run type-check
npm run lint
npm run build
```

Expected: exit code 0 for all three

---

## 自审检查清单

- [x] **Spec coverage** — 四个模块均有对应 Task（1.x 版本历史，2.x 对话持久化，3.x AI 优化按钮，4.x 首页统计）
- [x] **Placeholder scan** — 无 TBD/TODO/空占位，所有步骤含完整代码
- [x] **Type consistency** — `VersionVO` 在 Task 1.3（VO import）与 Task 1.2（返回类型）一致；`ChatMessageVO` 在 Task 2.1（定义）与 Task 2.3/2.5（使用）一致；`HomeStatsVO` 在 Task 4.1（定义）与 Task 4.2/4.5（使用）一致
