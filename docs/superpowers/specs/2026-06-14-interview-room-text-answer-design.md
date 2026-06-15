# 模拟面试房间支持语音/文字切换输入

## 背景

用户在 AI 模拟面试房间中使用麦克风回答时，常遇到语音识别（ASR）不准确的情况：发音问题、环境噪音、专业术语识别错误等。当前流程录音结束后直接自动提交识别结果，用户没有机会手动修正，导致 AI 拿到的回答不是用户真正想表达的内容，面试体验卡住。

## 目标

让面试房间支持「语音回答」与「文字回答」两种输入方式，并提供一键切换能力。用户可在任意合适时机切换到文字输入，修正或重新输入回答内容后再提交。

## 需求结论

通过澄清讨论确定以下方向：

- **输入方式**：提供「语音/文字」切换开关。
- **默认状态**：默认使用语音输入。
- **切换时内容处理**：从语音切换到文字时，已识别的文本自动带入文字输入框，可继续编辑。
- **切换入口位置**：题目卡片底部控制区。
- **提交方式**：文字模式下点击「提交回答」按钮提交。
- **实现方案**：全程实时切换（录音中也可切换）。

## 设计方案

### 1. 状态与交互

在前端 `InterviewRoomPage.vue` 中新增状态：

```ts
const inputMode = ref<'voice' | 'text'>('voice')
const textAnswer = ref('')
```

交互规则：

| 当前状态 | inputMode | 界面展示 |
|---------|-----------|---------|
| `readyToAnswer` | `voice` | 「开始回答」主按钮 + 「切换为文字」次按钮 |
| `readyToAnswer` | `text` | 多行文本输入框 + 「提交回答」主按钮 + 「切换为语音」次按钮 |
| `recording` | `voice` | 「结束回答」主按钮 + 「切换为文字」次按钮 |

切换行为：

- `voice -> text`：
  - 若正在录音，停止录音并清理音频资源（调用现有 `cleanupAudio()`）。
  - 将 `finalText` 的值赋给 `textAnswer`。
  - 状态回到 `readyToAnswer`。
- `text -> voice`：
  - 清空 `finalText` / `partialText`。
  - 状态回到 `readyToAnswer`，用户可点击「开始回答」重新录音。

### 2. UI 组件与布局

修改范围：`frontend/src/views/interview/InterviewRoomPage.vue`

- **底部控制区**（`.interview-room__controls`）始终显示模式切换按钮。
  - 使用 `NbButton` 的 secondary / outline 变体。
  - 文案：
    - 语音模式下：「⌨️ 切换为文字」
    - 文字模式下：「🎤 切换为语音」
- **文字模式**下，底部控制区上方显示一个占满宽度的多行文本输入框，使用原生 `<textarea>` 或 Element Plus 的 `ElInput`（type="textarea"）。
- **实时语音识别区**在文字模式下直接变为可编辑输入框区域，保持视觉一致性；仅保留问题文本区域不变。
- 保持现有 Neubrutalism 设计风格：2px 黑边框、偏移阴影、圆角、暖白底。

### 3. 数据流与错误处理

- `submitAnswer()` 调整：
  - 当 `inputMode === 'text'` 时，使用 `textAnswer.value.trim()` 作为 `answerText`。
  - 当 `inputMode === 'voice'` 时，保持现有逻辑，使用 `finalText.value.trim()`。
- 空回答校验：
  - 文字模式下若 `textAnswer` 为空，调用 `ElMessage.warning('回答内容不能为空，请输入后提交')`，不进入 `submittingAnswer` 状态。
- 时长字段：
  - 文字模式下 `answerDurationSeconds` 传 `0`，后续如有需要可再扩展为记录文字输入时长。
- 资源清理：
  - 切换时调用 `cleanupAudio()`，确保 WebSocket 关闭、麦克风释放，避免音频资源泄漏。

### 4. 后端影响

无需改动后端：

- 复用现有接口 `POST /api/interview/session/{sessionId}/turn/{turnId}/answer`。
- `InterviewAnswerRequest` 已包含 `answerText` 与 `answerDurationSeconds` 字段，`@NotBlank` 校验仍然有效。

### 5. 测试计划

#### 前端测试

在 `frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts`（如不存在则新建）中补充：

1. 点击「切换为文字」后，出现文本输入框，且输入框初始值为当前 `finalText`。
2. 文字模式下点击「提交回答」时，调用 `interviewStore.submitAnswer` 并传入 `textAnswer` 内容。
3. 文字模式下输入框为空时点击「提交回答」，不触发提交并弹出警告。
4. 录音中切换到文字模式时，麦克风资源被释放，输入框包含已识别文本。

#### 后端测试

无需新增后端测试，接口与 DTO 行为不变。

#### 手动验证

- 默认进入房间为语音模式。
- 语音模式正常录音提交。
- `readyToAnswer` 时切换到文字模式，输入答案并提交。
- 录音中切换到文字模式，已识别内容自动带入，修改后提交。
- 文字模式切回语音模式，可重新录音。
- 最后一题文字提交后正常进入报告页。

## 风险与边界

- 录音中切换会丢失正在识别但尚未返回的 partial 结果；由于切换时会等待 ASR 结束并取 `finalText`，可接受。
- 文字模式下如果用户已输入内容再切回语音，输入内容会被清空；如后续有需求可保留。
- 移动端键盘弹起可能遮挡输入框，需确保 textarea 可滚动。

## 未决事项

无。
