# AI 语音模拟面试功能设计文档

> 日期：2026-06-07  
> 状态：设计完成，待实现

## 1. 目标

为“面试吧”平台新增 AI 技术岗语音模拟面试功能。用户选择一份已有简历后开始面试，AI 根据简历内容、目标岗位、技术方向和工作年限动态生成技术问题，通过阿里云 TTS 播报问题；用户语音回答，系统通过阿里云实时 ASR 转写文本；面试完成后生成基础评分报告和历史记录。

首版目标是完成一条稳定可演示的语音技术面试闭环：创建面试、语音提问、实时语音识别、AI 追问、自动进入下一题、生成报告、查看历史记录。

## 2. 首版范围

### 2.1 包含能力

- 技术面试类型，仅围绕技术能力、项目经历、技能栈、工程实践提问。
- 用户必须选择一份简历开始面试。
- 每场默认 5 道主问题。
- 每道主问题最多允许 1 次 AI 追问。
- 问题完全由 AI 动态生成，不建设固定题库。
- AI 问题通过阿里云 TTS 播报。
- 用户回答通过阿里云实时 ASR 识别，并在前端展示实时字幕。
- 完整保留面试会话、每轮问题、用户回答、AI 简短反馈和最终报告。
- 报告包含总分、技术准确性、表达清晰度、项目深度、岗位匹配度和总体优化建议。

### 2.2 不包含能力

- 不做 HR 面试、行为面试、综合面试。
- 不做 AI 播报时用户打断。
- 不保存用户回答音频或 AI 问题音频。
- 不做逐题参考答案。
- 不做学习路径推荐。
- 不做题库管理。
- 不在前端暴露阿里云密钥。

## 3. 总体架构

```text
frontend/ Vue 3
├── 面试记录列表页
├── 创建面试页
├── 语音面试房间页
├── 面试报告页
├── interview api
├── interview store
└── audio/asr client utilities

backend/ Spring Boot
├── InterviewController
├── InterviewWsHandler
├── InterviewService
├── SpeechService
├── AliyunSpeechServiceImpl
├── InterviewMapper / InterviewTurnMapper / InterviewReportMapper
├── DTO / VO / Entity
└── ChatClient prompt orchestration

external services
├── DeepSeek via Spring AI ChatClient
└── Alibaba Cloud Intelligent Speech Interaction ASR/TTS
```

后端继续沿用现有 `controller -> service -> mapper` 分层。AI 文本推理仍通过已有 `ChatClient` 完成。语音能力通过新增 `SpeechService` 抽象封装，首版实现阿里云 Provider，后续如需切换讯飞、腾讯云或火山引擎，只替换 Provider 实现。

## 4. 核心流程

```text
1. 用户进入 /interview/new，选择一份简历
2. 前端提交 resumeId、targetPosition、techDirection 创建 interview_session
3. 用户进入 /interview/:id/room，点击开始面试
4. 后端读取简历模块并生成第 1 道技术问题
5. 后端调用阿里云 TTS 生成问题音频，返回问题文本和音频 Base64
6. 前端播放 AI 问题音频
7. 播放结束后，前端开启麦克风并连接 ASR WebSocket
8. 前端发送 PCM 音频帧到后端，后端桥接阿里云实时 ASR
9. 后端把实时识别结果返回前端，前端展示字幕
10. 用户点击结束回答，前端发送 END 并提交最终 answerText
11. 后端保存回答，AI 判断是否追问或进入下一题
12. 如果追问，生成 follow_up turn 并 TTS 播报
13. 如果进入下一题，生成 main turn 并 TTS 播报
14. 5 道主问题完成后，AI 生成 interview_report
15. 前端跳转 /interview/:id/report 展示报告
```

## 5. 数据库设计

### 5.1 interview_session

```sql
CREATE TABLE IF NOT EXISTS interview_session (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '面试会话 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  resume_id BIGINT NOT NULL COMMENT '关联简历 id',
  title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '面试标题',
  interview_type VARCHAR(32) NOT NULL DEFAULT 'technical' COMMENT '面试类型',
  target_position VARCHAR(128) NOT NULL DEFAULT '' COMMENT '目标岗位',
  tech_direction VARCHAR(128) NOT NULL DEFAULT '' COMMENT '技术方向',
  total_questions INT NOT NULL DEFAULT 5 COMMENT '主问题数量',
  current_question_no INT NOT NULL DEFAULT 0 COMMENT '当前主问题序号',
  status VARCHAR(32) NOT NULL DEFAULT 'created' COMMENT 'created/in_progress/generating_report/completed/cancelled',
  started_at DATETIME DEFAULT NULL COMMENT '开始时间',
  ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_resume_id (resume_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模拟面试会话表';
```

### 5.2 interview_turn

```sql
CREATE TABLE IF NOT EXISTS interview_turn (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '面试轮次 id',
  session_id BIGINT NOT NULL COMMENT '面试会话 id',
  question_no INT NOT NULL COMMENT '主问题序号',
  turn_type VARCHAR(32) NOT NULL COMMENT 'main/follow_up',
  question_text TEXT NOT NULL COMMENT 'AI 问题文本',
  answer_text TEXT DEFAULT NULL COMMENT '用户回答文本',
  ai_feedback TEXT DEFAULT NULL COMMENT 'AI 对本轮回答的简短反馈',
  answer_duration_seconds INT DEFAULT NULL COMMENT '回答耗时秒数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_session_id (session_id),
  KEY idx_question_no (question_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模拟面试轮次表';
```

### 5.3 interview_report

```sql
CREATE TABLE IF NOT EXISTS interview_report (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告 id',
  session_id BIGINT NOT NULL COMMENT '面试会话 id',
  total_score INT NOT NULL COMMENT '总分 0-100',
  accuracy_score INT NOT NULL COMMENT '技术准确性',
  clarity_score INT NOT NULL COMMENT '表达清晰度',
  depth_score INT NOT NULL COMMENT '项目深度',
  matching_score INT NOT NULL COMMENT '岗位匹配度',
  summary TEXT NOT NULL COMMENT '总体评价',
  suggestions JSON NOT NULL COMMENT '优化建议列表',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模拟面试报告表';
```

## 6. 后端 API 设计

所有接口沿用 `BaseResponse<T>`。接口前缀为 `/api/interview`。

### 6.1 创建面试会话

```text
POST /api/interview/session
Authorization: Bearer <token>
```

请求：

```json
{
  "resumeId": 1,
  "targetPosition": "Java 后端工程师",
  "techDirection": "Spring Boot / Redis / MySQL"
}
```

响应数据为 `InterviewSessionVO`。

### 6.2 开始面试

```text
POST /api/interview/session/{sessionId}/start
Authorization: Bearer <token>
```

响应：

```json
{
  "turnId": 1001,
  "questionNo": 1,
  "turnType": "main",
  "questionText": "请介绍你简历中最复杂的一个后端项目，并说明核心架构。",
  "ttsAudioBase64": "..."
}
```

### 6.3 提交回答

```text
POST /api/interview/session/{sessionId}/turn/{turnId}/answer
Authorization: Bearer <token>
```

请求：

```json
{
  "answerText": "我做过一个订单系统，主要负责订单创建、库存扣减和支付回调...",
  "answerDurationSeconds": 92
}
```

响应：

```json
{
  "nextAction": "NEXT_QUESTION",
  "turn": {
    "turnId": 1002,
    "questionNo": 2,
    "turnType": "main",
    "questionText": "你刚才提到 Redis 缓存，请说明如何处理缓存一致性问题。",
    "ttsAudioBase64": "..."
  },
  "reportId": null
}
```

`nextAction` 可选值：

- `FOLLOW_UP`：进入本题追问。
- `NEXT_QUESTION`：进入下一道主问题。
- `REPORT_READY`：报告已生成。

### 6.4 获取面试详情

```text
GET /api/interview/session/{sessionId}
Authorization: Bearer <token>
```

返回会话基本信息和全部 turn。

### 6.5 获取历史列表

```text
GET /api/interview/session/list
Authorization: Bearer <token>
```

返回当前用户的历史面试列表，按更新时间倒序。

### 6.6 获取报告

```text
GET /api/interview/session/{sessionId}/report
Authorization: Bearer <token>
```

返回报告和问答回顾。

### 6.7 取消面试

```text
POST /api/interview/session/{sessionId}/cancel
Authorization: Bearer <token>
```

仅允许取消 `created` 或 `in_progress` 状态的会话。

## 7. WebSocket 实时 ASR 协议

### 7.1 连接地址

```text
/ws/interview/asr?sessionId=1&turnId=1001&token=<jwt>
```

后端连接建立时必须校验：

- JWT 有效。
- session 属于当前用户。
- turn 属于当前 session。
- turn 尚未提交回答。
- session 状态为 `in_progress`。

### 7.2 前端控制消息

```json
{ "type": "START", "sessionId": 1, "turnId": 1001 }
```

```json
{ "type": "END" }
```

### 7.3 前端音频帧

前端发送二进制音频帧：

```text
ArrayBuffer
格式：16kHz、16bit、mono PCM
```

### 7.4 后端识别事件

```json
{
  "type": "ASR_PARTIAL",
  "text": "我做过一个订单"
}
```

```json
{
  "type": "ASR_FINAL",
  "text": "我做过一个订单系统，主要负责订单创建、库存扣减和支付回调。"
}
```

```json
{
  "type": "ERROR",
  "message": "语音识别服务暂时不可用"
}
```

## 8. AI Prompt 约束

### 8.1 生成问题

输入：简历摘要、目标岗位、技术方向、工作年限、历史问答、当前题号。

输出 JSON：

```json
{
  "questionText": "你简历中提到使用 Redis 做缓存，请说明你如何设计缓存更新策略？"
}
```

约束：

- 只问技术问题。
- 优先围绕简历项目、技能栈、工作经历。
- 每次只输出一个问题。
- 不输出 Markdown。
- 不输出多个候选问题。

### 8.2 回答后决策

输出 JSON：

```json
{
  "nextAction": "FOLLOW_UP",
  "feedback": "回答提到了缓存更新，但缺少异常场景。",
  "questionText": "如果数据库更新成功但缓存删除失败，你会如何保证一致性？"
}
```

规则：

- 每道主问题最多追问一次。
- 回答明显过短或偏题时优先追问。
- 已追问过则必须进入下一道主问题或生成报告。

### 8.3 生成报告

输出 JSON：

```json
{
  "totalScore": 82,
  "accuracyScore": 80,
  "clarityScore": 85,
  "depthScore": 78,
  "matchingScore": 86,
  "summary": "整体表现较好，能结合项目说明技术方案，但对异常场景和权衡说明不足。",
  "suggestions": [
    "回答技术方案时补充为什么这样设计",
    "多说明故障场景和兜底方案",
    "项目经历中增加量化指标"
  ]
}
```

## 9. 前端页面设计

### 9.1 `/interview`

面试记录列表页。

内容：

- 顶部“开始技术面试”按钮。
- 历史面试卡片：标题、关联简历、状态、总分、创建时间。
- 操作：继续未完成面试、查看报告、取消。

### 9.2 `/interview/new`

创建面试页。

内容：

- 选择一份简历。
- 自动带出目标岗位。
- 填写或确认技术方向。
- 展示规则：5 道技术主问题、每题最多 1 次追问、语音面试、不保存音频。
- 点击开始后创建会话并跳转房间页。

### 9.3 `/interview/:id/room`

语音面试房间页。

内容：

- 当前进度：`第 2 / 5 题`。
- AI 面试官问题文本。
- 播放状态：`AI 正在提问`、`请开始回答`、`识别中`、`正在生成下一题`。
- 实时字幕区。
- 控制按钮：开始回答、结束回答、重试识别、取消面试。
- 右侧简历摘要：目标岗位、技能、项目名称。

### 9.4 `/interview/:id/report`

面试报告页。

内容：

- 总分卡片。
- 4 个维度进度条。
- 总体评价。
- 优化建议列表。
- 问答回顾：每轮问题、回答、AI 简短反馈。

## 10. 前端状态机

```ts
type InterviewRoomState =
  | 'idle'
  | 'loadingQuestion'
  | 'playingQuestion'
  | 'readyToAnswer'
  | 'recording'
  | 'recognizing'
  | 'submittingAnswer'
  | 'generatingNext'
  | 'completed'
  | 'error'
```

状态流：

```text
idle
→ loadingQuestion
→ playingQuestion
→ readyToAnswer
→ recording
→ recognizing
→ submittingAnswer
→ generatingNext
→ playingQuestion
...
→ completed
```

规则：

- `playingQuestion` 时禁用录音。
- `recording` 时持续发送音频帧到 WebSocket。
- `recognizing` 时等待最后一段 `ASR_FINAL`。
- `submittingAnswer` 时调用提交回答接口。
- 返回 `FOLLOW_UP` 或 `NEXT_QUESTION` 时进入 `playingQuestion`。
- 返回 `REPORT_READY` 时进入 `completed` 并跳转报告页。

## 11. 音频采集设计

前端使用 `navigator.mediaDevices.getUserMedia({ audio: true })` 获取麦克风权限。

音频处理要求：

- 采集浏览器原始音频流。
- 转换为 `16kHz 16bit mono PCM`。
- 通过 WebSocket 发送 `ArrayBuffer`。

首选实现：

- 使用 `AudioWorklet` 读取 PCM，便于后续维护。
- 如实现周期紧，可先使用 `ScriptProcessorNode` 完成首版演示，再在后续版本替换。

## 12. 阿里云语音配置

后端新增环境变量：

```text
ALIYUN_ACCESS_KEY_ID
ALIYUN_ACCESS_KEY_SECRET
ALIYUN_NLS_APP_KEY
ALIYUN_NLS_REGION
```

新增应用配置：

```yaml
app:
  speech:
    provider: aliyun
    aliyun:
      app-key: ${ALIYUN_NLS_APP_KEY}
      region: ${ALIYUN_NLS_REGION:cn-shanghai}
```

约束：

- 阿里云密钥只存在后端。
- 前端不直接调用阿里云。
- TTS 和 ASR 都由后端代理。
- TTS 音频首版以 Base64 返回前端，不依赖对象存储。
- ASR 不保存音频。

## 13. 错误处理

| 场景 | 处理方式 |
|------|----------|
| 麦克风权限被拒绝 | 前端提示用户允许麦克风权限 |
| 浏览器不支持音频采集 | 提示使用最新版 Chrome/Edge |
| WebSocket 断开 | 停止录音，保留已识别文本，允许重连或手动提交 |
| 阿里云 ASR 失败 | 提示识别失败，允许重试当前回答 |
| 阿里云 TTS 失败 | 返回问题文本，提示语音生成失败，允许阅读问题后继续 |
| AI 生成失败 | 保留当前会话，允许重新生成下一题或取消 |
| 用户中途退出 | 会话保持 `in_progress`，下次从当前未回答 turn 继续 |

## 14. 错误码扩展

在现有 `ErrorCode` 基础上新增：

```java
INTERVIEW_NOT_FOUND_ERROR(40410, "面试不存在"),
INTERVIEW_STATUS_ERROR(40010, "面试状态异常"),
INTERVIEW_TURN_ERROR(40011, "面试轮次异常"),
SPEECH_SERVICE_ERROR(50010, "语音服务调用失败"),
SPEECH_RECOGNITION_ERROR(50011, "语音识别失败"),
SPEECH_SYNTHESIS_ERROR(50012, "语音合成失败"),
```

## 15. 测试设计

### 15.1 后端测试

- `InterviewServiceImplTest`：创建会话、校验简历归属、生成首题、提交回答、生成报告。
- `InterviewControllerTest`：REST 接口参数校验、未登录、无权限、正常响应。
- `InterviewSpeechWsHandlerTest`：JWT 校验、turn 校验、START/END 消息处理。
- `AliyunSpeechServiceTest`：mock 阿里云 SDK，不真实调用外部服务。
- `InterviewReportServiceTest`：AI JSON 解析失败时抛出业务异常。

### 15.2 前端测试

- `InterviewNewPage.spec.ts`：选择简历并创建会话。
- `InterviewRoomPage.spec.ts`：状态机切换、提交回答、跳转报告。
- `InterviewReportPage.spec.ts`：报告维度渲染。
- 音频采集工具测试：mock `getUserMedia`、`WebSocket`、`AudioContext`。

## 16. 验收标准

- 用户能选择简历并创建技术语音面试。
- AI 能生成技术问题并通过阿里云 TTS 播放。
- 用户语音回答时，前端能实时展示识别文本。
- 用户结束回答后，系统能保存文字回答。
- AI 能根据回答进入追问或下一题。
- 5 道主问题完成后能生成报告。
- 报告包含总分、4 个维度、总体评价和优化建议。
- 历史记录能查看完整问答和报告。
- 系统不保存任何音频文件。
