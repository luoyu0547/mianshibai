# Resume AI Chat Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Spring AI tool calling to resume AI chat so AI can propose resume edits, stream replies, and apply changes only after user comparison and approval.

**Architecture:** Backend registers a Spring AI `@Tool` during resume chat to collect safe resume patch proposals without mutating database state. The chat endpoint streams plain text as normal SSE `message` events and emits proposal payloads as named `resume_patch_proposal` SSE events. Frontend parses both event types, renders streamed assistant text immediately, and opens a compare dialog before applying the proposal to local editor state.

**Tech Stack:** Spring Boot 3.5.x, Java 17, Spring AI 1.1.2 `ChatClient.tools(...)`, `SseEmitter`, Vue 3, TypeScript, Vite, Vitest, Element Plus.

---

## File Structure

- Create `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumePatchRequest.java`: Spring AI tool input DTO for a proposed resume section replacement.
- Create `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumePatchProposalVO.java`: serialized proposal payload sent to the frontend.
- Create `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeChatStreamEventVO.java`: service-level event wrapper with `event`, `content`, and optional proposal.
- Create `backend/src/main/java/com/mianshiba/ai/service/tool/ResumePatchTools.java`: Spring AI `@Tool` class that validates and collects proposals without applying them.
- Modify `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`: change `chatStream` return type from `Flux<String>` to `Flux<ResumeChatStreamEventVO>`.
- Modify `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`: register `ResumePatchTools`, update prompt, emit text/proposal events, preserve fallback behavior.
- Modify `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java`: send named SSE events based on `ResumeChatStreamEventVO`.
- Modify `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`: update chat stream assertions for event wrappers.
- Create `backend/src/test/java/com/mianshiba/ai/service/tool/ResumePatchToolsTest.java`: validate tool collection and input rejection.
- Modify `frontend/src/types/resume.ts`: add proposal/tool event types.
- Create `frontend/src/utils/sse.ts`: focused parser for SSE frames from `ReadableStream` text chunks.
- Create `frontend/src/utils/__tests__/sse.spec.ts`: parser tests.
- Create `frontend/src/components/resume/ResumePatchConfirmDialog.vue`: reusable compare-confirm dialog for AI chat proposals.
- Modify `frontend/src/components/resume/AiChatPanel.vue`: parse named SSE events, stream text, display proposal action strip, emit proposal events.
- Modify `frontend/src/views/resume/ResumeEditPage.vue`: open confirmation dialog and apply approved proposals to local section state.

## Task 1: Backend Proposal DTOs And Tool

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumePatchRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumePatchProposalVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeChatStreamEventVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/tool/ResumePatchTools.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/tool/ResumePatchToolsTest.java`

- [ ] **Step 1: Write the failing tool validation test**

```java
package com.mianshiba.ai.service.tool;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.model.dto.resume.ResumePatchRequest;
import com.mianshiba.ai.model.vo.resume.ResumePatchProposalVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumePatchToolsTest {

    @Test
    void proposeResumePatchCollectsValidProposal() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("summary");
        request.setOperation("replace_section");
        request.setReason("强化自我评价");
        request.setSectionData(Map.of("content", "具备扎实 Java 后端经验，关注业务结果。"));

        String result = tools.proposeResumePatch(request);

        assertThat(result).contains("等待用户确认");
        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).getSectionType()).isEqualTo("summary");
        assertThat(proposals.get(0).getOperation()).isEqualTo("replace_section");
        assertThat(proposals.get(0).getReason()).isEqualTo("强化自我评价");
        assertThat(proposals.get(0).getSectionData()).containsEntry("content", "具备扎实 Java 后端经验，关注业务结果。");
    }

    @Test
    void proposeResumePatchRejectsUnknownSectionType() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("unknown");
        request.setOperation("replace_section");
        request.setSectionData(Map.of("content", "invalid"));

        assertThatThrownBy(() -> tools.proposeResumePatch(request))
                .isInstanceOf(BusinessException.class);
        assertThat(proposals).isEmpty();
    }

    @Test
    void proposeResumePatchRejectsUnsupportedOperation() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("summary");
        request.setOperation("append_item");
        request.setSectionData(Map.of("content", "invalid"));

        assertThatThrownBy(() -> tools.proposeResumePatch(request))
                .isInstanceOf(BusinessException.class);
        assertThat(proposals).isEmpty();
    }
}
```

- [ ] **Step 2: Run the failing backend test**

Run: `.

Working directory: `backend`

Expected: compilation fails because `ResumePatchTools`, `ResumePatchRequest`, and `ResumePatchProposalVO` do not exist.

- [ ] **Step 3: Add DTOs and event wrapper**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumePatchRequest.java`:

```java
package com.mianshiba.ai.model.dto.resume;

import lombok.Data;

import java.util.Map;

@Data
public class ResumePatchRequest {

    private String sectionType;

    private String operation;

    private String reason;

    private Map<String, Object> sectionData;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumePatchProposalVO.java`:

```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.util.Map;

@Data
public class ResumePatchProposalVO {

    private String sectionType;

    private String operation;

    private String reason;

    private Map<String, Object> sectionData;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeChatStreamEventVO.java`:

```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

@Data
public class ResumeChatStreamEventVO {

    public static final String EVENT_MESSAGE = "message";
    public static final String EVENT_PROPOSAL = "resume_patch_proposal";

    private String event;

    private String content;

    private ResumePatchProposalVO proposal;

    public static ResumeChatStreamEventVO message(String content) {
        ResumeChatStreamEventVO event = new ResumeChatStreamEventVO();
        event.setEvent(EVENT_MESSAGE);
        event.setContent(content);
        return event;
    }

    public static ResumeChatStreamEventVO proposal(ResumePatchProposalVO proposal) {
        ResumeChatStreamEventVO event = new ResumeChatStreamEventVO();
        event.setEvent(EVENT_PROPOSAL);
        event.setProposal(proposal);
        return event;
    }
}
```

- [ ] **Step 4: Add Spring AI tool class**

Create `backend/src/main/java/com/mianshiba/ai/service/tool/ResumePatchTools.java`:

```java
package com.mianshiba.ai.service.tool;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.dto.resume.ResumePatchRequest;
import com.mianshiba.ai.model.vo.resume.ResumePatchProposalVO;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ResumePatchTools {

    private static final Set<String> VALID_SECTION_TYPES = Set.of("basic", "education", "work", "project", "skills", "summary");
    private static final String OPERATION_REPLACE_SECTION = "replace_section";

    private final Consumer<ResumePatchProposalVO> proposalConsumer;

    public ResumePatchTools(Consumer<ResumePatchProposalVO> proposalConsumer) {
        this.proposalConsumer = proposalConsumer;
    }

    @Tool(description = "Propose a resume section edit for user review. Use this when the user asks to modify, polish, rewrite, add, or fill resume content. Do not save or apply changes. The only supported operation is replace_section.")
    public String proposeResumePatch(ResumePatchRequest request) {
        validate(request);
        ResumePatchProposalVO proposal = new ResumePatchProposalVO();
        proposal.setSectionType(request.getSectionType().trim());
        proposal.setOperation(request.getOperation().trim());
        proposal.setReason(request.getReason() == null ? "AI 建议修改该模块" : request.getReason().trim());
        proposal.setSectionData(request.getSectionData());
        proposalConsumer.accept(proposal);
        return "已生成简历修改提案，等待用户确认后才会应用。";
    }

    private void validate(ResumePatchRequest request) {
        if (request == null || request.getSectionType() == null || request.getOperation() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历修改提案参数不完整");
        }
        String sectionType = request.getSectionType().trim();
        String operation = request.getOperation().trim();
        Map<String, Object> sectionData = request.getSectionData();
        if (!VALID_SECTION_TYPES.contains(sectionType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历模块类型不合法");
        }
        if (!OPERATION_REPLACE_SECTION.equals(operation)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历修改操作不支持");
        }
        if (sectionData == null || sectionData.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历修改内容不能为空");
        }
    }
}
```

- [ ] **Step 5: Run the tool tests**

Run: `.

Working directory: `backend`

Expected: tests pass.

## Task 2: Backend Chat Stream Events And Tool Registration

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`

- [ ] **Step 1: Update existing chat stream tests to expect event wrappers**

In `ResumeAiServiceImplTest`, update the fallback test body:

```java
List<ResumeChatStreamEventVO> chunks = service.chatStream(auth, 1L, "怎么优化项目经历？").collectList().block();

assertThat(chunks).hasSize(1);
assertThat(chunks.get(0).getEvent()).isEqualTo(ResumeChatStreamEventVO.EVENT_MESSAGE);
assertThat(chunks.get(0).getContent()).isEqualTo("请补充项目量化成果。");
ArgumentCaptor<ResumeChatMessage> messageCaptor = ArgumentCaptor.forClass(ResumeChatMessage.class);
verify(chatMessageMapper, times(2)).insert(messageCaptor.capture());
assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
assertThat(messageCaptor.getAllValues().get(1).getContent()).isEqualTo("请补充项目量化成果。");
```

Add import:

```java
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
```

- [ ] **Step 2: Run updated service test to verify it fails**

Run: `.

Working directory: `backend`

Expected: compilation fails because `chatStream` still returns `Flux<String>`.

- [ ] **Step 3: Update service interface return type**

In `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`, replace the chat method signature:

```java
Flux<ResumeChatStreamEventVO> chatStream(String authorizationHeader, Long resumeId, String message);
```

Add import:

```java
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
```

- [ ] **Step 4: Update service implementation chat stream**

In `ResumeAiServiceImpl.java`, update imports:

```java
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
import com.mianshiba.ai.model.vo.resume.ResumePatchProposalVO;
import com.mianshiba.ai.service.tool.ResumePatchTools;
```

Replace `CHAT_SYSTEM_PROMPT` with:

```java
private static final String CHAT_SYSTEM_PROMPT =
        "你是一位专业的简历顾问助手。用户正在编辑简历，以下是当前简历的模块内容摘要：\n%s\n\n" +
        "请根据用户的问题提供建议。回答要简洁专业。\n" +
        "如果用户明确要求你修改、补充、润色、重写或填写简历内容，请调用 proposeResumePatch 工具生成待确认的修改提案。\n" +
        "工具只用于提出修改，不会保存简历；用户确认前不要声称已经修改完成。";
```

Replace `chatStream` implementation with:

```java
@Override
public Flux<ResumeChatStreamEventVO> chatStream(String authorizationHeader, Long resumeId, String message) {
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
    List<ResumePatchProposalVO> proposals = new ArrayList<>();
    ResumePatchTools tools = new ResumePatchTools(proposals::add);

    Flux<ResumeChatStreamEventVO> textStream = chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .tools(tools)
            .stream()
            .content()
            .doOnNext(fullResponse::append)
            .map(ResumeChatStreamEventVO::message);

    return textStream
            .concatWith(Flux.defer(() -> Flux.fromIterable(proposals).map(ResumeChatStreamEventVO::proposal)))
            .doOnComplete(() -> saveAssistantMessage(resumeId, fullResponse.toString()))
            .doOnError(e -> log.error("AI 对话流异常", e))
            .onErrorResume(e -> {
                logAiProviderError("AI 对话流调用失败，尝试降级为非流式调用", e);
                return Flux.defer(() -> {
                    String fallbackResponse = callAi(systemPrompt, message);
                    fullResponse.append(fallbackResponse);
                    saveAssistantMessage(resumeId, fallbackResponse);
                    return Flux.just(ResumeChatStreamEventVO.message(fallbackResponse));
                });
            });
}
```

- [ ] **Step 5: Update controller SSE sending**

In `ResumeAiController.java`, add imports:

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
```

Add field:

```java
private final ObjectMapper objectMapper = new ObjectMapper();
```

Replace the subscription body in `chat`:

```java
Flux<ResumeChatStreamEventVO> stream = resumeAiService.chatStream(authorizationHeader, resumeId, chatRequest.getMessage());
stream.subscribe(event -> {
    try {
        if (ResumeChatStreamEventVO.EVENT_PROPOSAL.equals(event.getEvent())) {
            emitter.send(SseEmitter.event()
                    .name(ResumeChatStreamEventVO.EVENT_PROPOSAL)
                    .data(toJson(event.getProposal())));
        } else {
            emitter.send(SseEmitter.event().data(event.getContent()));
        }
    } catch (IOException e) {
        emitter.completeWithError(e);
    }
}, emitter::completeWithError, emitter::complete);
```

Add helper method:

```java
private String toJson(Object value) {
    try {
        return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
        throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "SSE 数据序列化失败");
    }
}
```

Add imports for `BusinessException` and `ErrorCode` if not already present.

- [ ] **Step 6: Run backend chat tests**

Run: `.

Working directory: `backend`

Expected: tests pass.

## Task 3: Frontend SSE Parser

**Files:**
- Create: `frontend/src/utils/sse.ts`
- Create: `frontend/src/utils/__tests__/sse.spec.ts`

- [ ] **Step 1: Write parser tests**

Create `frontend/src/utils/__tests__/sse.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { createSseParser } from '../sse'

describe('createSseParser', () => {
  it('parses plain message events incrementally', () => {
    const events: Array<{ event: string; data: string }> = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: 你')
    parser.push('好\n\n')

    expect(events).toEqual([{ event: 'message', data: '你好' }])
  })

  it('parses named proposal events', () => {
    const events: Array<{ event: string; data: string }> = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('event: resume_patch_proposal\n')
    parser.push('data: {"sectionType":"summary"}\n\n')

    expect(events).toEqual([
      { event: 'resume_patch_proposal', data: '{"sectionType":"summary"}' },
    ])
  })
})
```

- [ ] **Step 2: Run parser test to verify it fails**

Run: `npm run test:unit -- sse.spec.ts`

Working directory: `frontend`

Expected: test fails because `src/utils/sse.ts` does not exist.

- [ ] **Step 3: Implement parser**

Create `frontend/src/utils/sse.ts`:

```ts
export interface SseEvent {
  event: string
  data: string
}

export function createSseParser(onEvent: (event: SseEvent) => void) {
  let buffer = ''

  function dispatch(frame: string) {
    const lines = frame.split('\n')
    let event = 'message'
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim() || 'message'
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }

    if (dataLines.length > 0) {
      onEvent({ event, data: dataLines.join('\n') })
    }
  }

  return {
    push(chunk: string) {
      buffer += chunk
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() || ''
      for (const frame of frames) {
        if (frame.trim()) dispatch(frame)
      }
    },
    flush() {
      if (buffer.trim()) {
        dispatch(buffer)
        buffer = ''
      }
    },
  }
}
```

- [ ] **Step 4: Run parser test**

Run: `npm run test:unit -- sse.spec.ts`

Working directory: `frontend`

Expected: test passes.

## Task 4: Frontend Proposal Types And Confirmation Dialog

**Files:**
- Modify: `frontend/src/types/resume.ts`
- Create: `frontend/src/components/resume/ResumePatchConfirmDialog.vue`

- [ ] **Step 1: Add TypeScript proposal types**

Append to `frontend/src/types/resume.ts`:

```ts
export interface ResumePatchProposal {
  sectionType: SectionType
  operation: 'replace_section'
  reason?: string
  sectionData: Record<string, unknown>
}
```

- [ ] **Step 2: Add confirmation dialog component**

Create `frontend/src/components/resume/ResumePatchConfirmDialog.vue`:

```vue
<template>
  <el-dialog
    :model-value="visible"
    title="确认 AI 修改"
    width="860px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="patch-dialog__summary">
      <strong>{{ sectionLabel }}</strong>
      <span>{{ proposal?.reason || 'AI 建议修改该模块' }}</span>
    </div>

    <div class="patch-dialog__compare">
      <div class="patch-dialog__side">
        <NbSectionTitle title="当前内容" />
        <pre>{{ currentJson }}</pre>
      </div>
      <div class="patch-dialog__side patch-dialog__side--after">
        <NbSectionTitle title="AI 建议" />
        <pre>{{ proposedJson }}</pre>
      </div>
    </div>

    <template #footer>
      <NbButton variant="ghost" @click="$emit('update:visible', false)">取消</NbButton>
      <NbButton variant="primary" :disabled="!proposal" @click="handleApply">应用到简历</NbButton>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumePatchProposal, SectionType } from '@/types/resume'
import NbButton from '@/components/NbButton.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'

const props = defineProps<{
  visible: boolean
  proposal: ResumePatchProposal | null
  currentData: Record<string, unknown> | Record<string, unknown>[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'apply', proposal: ResumePatchProposal): void
}>()

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

const sectionLabel = computed(() => {
  const type = props.proposal?.sectionType
  return type ? sectionLabelMap[type] : '简历模块'
})

const currentJson = computed(() => JSON.stringify(props.currentData, null, 2))
const proposedJson = computed(() => JSON.stringify(props.proposal?.sectionData || {}, null, 2))

function handleApply() {
  if (!props.proposal) return
  emit('apply', props.proposal)
  emit('update:visible', false)
}
</script>

<style scoped>
.patch-dialog__summary {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  margin-bottom: 14px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-primary-light);
}

.patch-dialog__compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.patch-dialog__side {
  min-width: 0;
  padding: 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
}

.patch-dialog__side--after {
  box-shadow: var(--nb-shadow-xs);
}

pre {
  max-height: 420px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .patch-dialog__compare {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 3: Run frontend type-check**

Run: `npm run type-check`

Working directory: `frontend`

Expected: type-check passes or only reports pre-existing unrelated errors. Any error in `ResumePatchConfirmDialog.vue` or `types/resume.ts` must be fixed before proceeding.

## Task 5: Frontend Chat Panel Proposal Handling

**Files:**
- Modify: `frontend/src/components/resume/AiChatPanel.vue`

- [ ] **Step 1: Extend chat message state and emits**

In `AiChatPanel.vue`, import the parser and proposal type:

```ts
import { createSseParser } from '@/utils/sse'
import type { SectionType, ChatMessageVO, ResumePatchProposal } from '@/types/resume'
```

Replace `ChatMessage` interface:

```ts
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  proposals?: ResumePatchProposal[]
  ignoredProposalIndexes?: number[]
}
```

Replace emits:

```ts
const emit = defineEmits<{
  extracted: [sectionType: SectionType, sectionData: Record<string, unknown>]
  proposal: [proposal: ResumePatchProposal]
}>()
```

- [ ] **Step 2: Add proposal action UI under assistant bubbles**

Inside the message bubble template after content span, add:

```vue
<div v-if="msg.role === 'assistant' && visibleProposals(msg).length" class="ai-chat-panel__proposals">
  <div
    v-for="item in visibleProposals(msg)"
    :key="item.index"
    class="ai-chat-panel__proposal"
  >
    <span>AI 建议修改{{ sectionLabel(item.proposal.sectionType) }}</span>
    <small>{{ item.proposal.reason || '等待确认后应用' }}</small>
    <div class="ai-chat-panel__proposal-actions">
      <NbButton variant="primary" size="small" @click="emit('proposal', item.proposal)">查看对比</NbButton>
      <NbButton variant="ghost" size="small" @click="ignoreProposal(msg, item.index)">忽略</NbButton>
    </div>
  </div>
</div>
```

Add helpers:

```ts
const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

function sectionLabel(type: SectionType) {
  return sectionLabelMap[type] || type
}

function visibleProposals(msg: ChatMessage) {
  const ignored = new Set(msg.ignoredProposalIndexes || [])
  return (msg.proposals || [])
    .map((proposal, index) => ({ proposal, index }))
    .filter((item) => !ignored.has(item.index))
}

function ignoreProposal(msg: ChatMessage, index: number) {
  msg.ignoredProposalIndexes = [...(msg.ignoredProposalIndexes || []), index]
}
```

- [ ] **Step 3: Replace stream parsing in `handleSend`**

Replace the manual JSON line parsing loop with:

```ts
const parser = createSseParser((event) => {
  if (event.event === 'resume_patch_proposal') {
    try {
      const proposal = JSON.parse(event.data) as ResumePatchProposal
      if (proposal.operation === 'replace_section' && proposal.sectionType && proposal.sectionData) {
        assistantMsg.proposals = [...(assistantMsg.proposals || []), proposal]
      }
    } catch {
      // 忽略无法解析的工具事件，不影响正常聊天
    }
    return
  }

  if (event.data && event.data !== '[DONE]') {
    assistantMsg.content += event.data
  }
  scrollToBottom()
})

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  parser.push(decoder.decode(value, { stream: true }))
}
parser.flush()
```

- [ ] **Step 4: Add proposal styles**

Append to `AiChatPanel.vue` styles:

```css
.ai-chat-panel__proposals {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  width: 100%;
}

.ai-chat-panel__proposal {
  padding: 10px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-primary-light);
}

.ai-chat-panel__proposal small {
  display: block;
  margin-top: 4px;
  color: var(--nb-muted);
}

.ai-chat-panel__proposal-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
```

- [ ] **Step 5: Run frontend type-check**

Run: `npm run type-check`

Working directory: `frontend`

Expected: type-check passes or only reports pre-existing unrelated errors. Any error in `AiChatPanel.vue` or `sse.ts` must be fixed before proceeding.

## Task 6: Resume Edit Page Confirmation And Apply

**Files:**
- Modify: `frontend/src/views/resume/ResumeEditPage.vue`

- [ ] **Step 1: Import dialog and proposal type**

Add imports:

```ts
import ResumePatchConfirmDialog from '@/components/resume/ResumePatchConfirmDialog.vue'
import type { SectionVO, SectionType, ResumePatchProposal } from '@/types/resume'
```

Ensure the old type import line is replaced rather than duplicated.

- [ ] **Step 2: Add dialog to template**

After `WholeResumeOptimizeDialog`, add:

```vue
<ResumePatchConfirmDialog
  v-model:visible="patchConfirmVisible"
  :proposal="pendingPatchProposal"
  :current-data="pendingPatchCurrentData"
  @apply="handlePatchProposalApplied"
/>
```

- [ ] **Step 3: Wire chat panel proposal event**

Update `AiChatPanel` usage:

```vue
<AiChatPanel
  v-if="aiPanelMode === 'chat'"
  :resume-id="resumeId"
  @extracted="handleExtracted"
  @proposal="handlePatchProposal"
/>
```

- [ ] **Step 4: Add pending proposal state and helpers**

In script setup near AI panel state, add:

```ts
const patchConfirmVisible = ref(false)
const pendingPatchProposal = ref<ResumePatchProposal | null>(null)
```

Add computed current data:

```ts
const pendingPatchCurrentData = computed<Record<string, unknown> | Record<string, unknown>[]>(() => {
  const type = pendingPatchProposal.value?.sectionType
  if (!type) return {}
  return sectionDataMap.value[type]
})
```

Add handlers:

```ts
function handlePatchProposal(proposal: ResumePatchProposal) {
  if (proposal.operation !== 'replace_section') {
    ElMessage.warning('暂不支持该 AI 修改类型')
    return
  }
  pendingPatchProposal.value = proposal
  patchConfirmVisible.value = true
}

function handlePatchProposalApplied(proposal: ResumePatchProposal) {
  handleOptimizeApplied(proposal.sectionType, proposal.sectionData)
  ElMessage.success('已应用 AI 修改结果，请检查后保存')
}
```

- [ ] **Step 5: Run frontend type-check**

Run: `npm run type-check`

Working directory: `frontend`

Expected: type-check passes or only reports pre-existing unrelated errors. Any error in `ResumeEditPage.vue` or `ResumePatchConfirmDialog.vue` must be fixed before proceeding.

## Task 7: Full Verification

**Files:**
- Verify all modified files.

- [ ] **Step 1: Run focused backend tests**

Run: `.

Working directory: `backend`

Expected: tests pass.

- [ ] **Step 2: Run backend package**

Run: `.

Working directory: `backend`

Expected: build succeeds.

- [ ] **Step 3: Run frontend tests**

Run: `npm run test:unit -- sse.spec.ts`

Working directory: `frontend`

Expected: test passes.

- [ ] **Step 4: Run frontend type-check**

Run: `npm run type-check`

Working directory: `frontend`

Expected: type-check succeeds.

- [ ] **Step 5: Inspect diff**

Run: `git diff -- backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumePatchRequest.java backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumePatchProposalVO.java backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeChatStreamEventVO.java backend/src/main/java/com/mianshiba/ai/service/tool/ResumePatchTools.java backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java frontend/src/types/resume.ts frontend/src/utils/sse.ts frontend/src/components/resume/ResumePatchConfirmDialog.vue frontend/src/components/resume/AiChatPanel.vue frontend/src/views/resume/ResumeEditPage.vue`

Working directory: repository root.

Expected: diff shows Spring AI tool calling, named SSE proposal events, streaming parser, and user confirmation before local form mutation. Diff must not show direct resume database writes from `ResumePatchTools`.

## Self-Review

- Spec coverage: backend Spring AI tool calling is covered in Tasks 1-2; named SSE events are covered in Task 2; frontend streaming parser is covered in Task 3; compare-confirm UI is covered in Tasks 4 and 6; safety and no direct DB writes are covered in Task 1 and Task 7.
- Placeholder scan: the plan contains no deferred implementation markers. Every new file has concrete code or a concrete test path.
- Type consistency: `ResumePatchRequest`, `ResumePatchProposalVO`, `ResumeChatStreamEventVO`, and frontend `ResumePatchProposal` consistently use `sectionType`, `operation`, `reason`, and `sectionData`.
- Commit handling: this plan omits commit steps because repository instructions say git commits require an explicit user request.
