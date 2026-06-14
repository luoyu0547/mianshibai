# OSS 文件存储能力设计文档

## 背景

项目当前已有用户头像上传能力，后端存在 `/api/file/avatar`、`FileUploadProperties`、Aliyun OSS 依赖和 `app.file.*` 配置雏形。面试房间当前会采集用户语音并通过 WebSocket 发送给 ASR，但回答录音不会保存。简历 PDF 当前由前端通过 `html2canvas` 和 `jsPDF` 本地导出，不经过后端保存。

本次只覆盖项目现有功能需要的文件保存点：用户头像图片和面试回答录音。不扩展投递附件等当前不存在的业务能力。

## 目标

- 统一后端文件存储抽象，支持本地存储和 Aliyun OSS。
- 用户头像继续支持上传，并由统一存储能力保存。
- 面试回答录音支持用户主动开启后保存到 OSS 或本地存储。
- 录音不暴露公开 URL，播放时由后端按权限生成短期签名 URL。
- 文件校验、错误处理和测试覆盖项目当前需要的图片与音频场景。

## 非目标

- 不新增投递附件、公司附件、Offer 附件等当前业务不存在的文件模块。
- 不改变简历 PDF 当前本地导出流程。
- 不建设完整文件中心或通用 `file_asset` 数据表。
- 不默认保存所有面试录音，录音保存必须由用户主动开启。

## 方案选择

采用“统一文件存储 + 现有业务接入”方案。

备选方案：

- 最小改造：只把头像上传接入 OSS。改动小，但不能覆盖面试录音，也没有私有访问能力。
- 推荐方案：抽出统一存储服务，头像使用公开 URL，面试录音保存 object key 并使用短期签名 URL 播放。改动可控，贴合现有功能。
- 完整文件中心：新增 `file_asset` 表统一管理全部文件。扩展性强，但对当前仅头像和录音的需求偏重。

## 后端架构

新增或调整以下职责边界：

- `FileStorageService`：基础存储抽象，负责上传文件、生成公开访问 URL、生成短期签名 URL。
- `LocalFileStorageService`：本地开发环境实现，写入 `app.file.upload-dir`。
- `AliyunOssFileStorageService`：Aliyun OSS 实现，上传对象并生成公开 URL 或签名 URL。
- `FileService`：业务级文件服务，负责头像和录音的类型、大小、扩展名、路径分类等校验。
- `FileController`：保留头像上传接口，不承载 OSS 细节。
- `InterviewController` / `InterviewService`：新增面试录音上传和签名播放地址获取能力。

配置继续使用当前 `app.file.provider=local/aliyun` 和 `app.file.aliyun.*`。补充配置项：

- `app.file.signed-url-expiration`：签名 URL 有效期，默认 10 分钟。
- `app.file.avatar-max-size`：头像大小限制，默认 2MB，可兼容当前 `max-size`。
- `app.file.audio-max-size`：录音大小限制，建议默认 20MB。

OSS 配置缺失时不在启动阶段强制失败，实际上传或生成签名 URL 时返回系统错误并记录明确日志。

## 数据模型

在 `interview_turn` 增加录音相关字段：

```sql
answer_audio_object_key VARCHAR(512) NOT NULL DEFAULT '' COMMENT '回答录音对象 Key',
answer_audio_original_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '回答录音原始文件名',
answer_audio_size BIGINT NOT NULL DEFAULT 0 COMMENT '回答录音文件大小',
answer_audio_content_type VARCHAR(128) NOT NULL DEFAULT '' COMMENT '回答录音 Content-Type',
answer_audio_duration_seconds INT NOT NULL DEFAULT 0 COMMENT '回答录音时长秒数'
```

这些字段只保存元数据，不保存公开 URL。头像仍使用 `user.user_avatar` 保存 URL，保持当前前端和用户资料逻辑不变。

## 接口设计

### 头像上传

保留现有接口：

```http
POST /api/file/avatar
Content-Type: multipart/form-data
```

行为：

- 校验用户登录。
- 仅允许 `image/jpeg`、`image/png`、`image/webp`。
- 上传到当前配置的存储 provider。
- 返回可直接展示的头像 URL。

### 上传面试回答录音

新增接口：

```http
POST /api/interview/{sessionId}/turn/{turnId}/audio
Content-Type: multipart/form-data
```

表单字段：

- `file`：音频文件。
- `durationSeconds`：录音时长，秒。

校验：

- 当前用户必须是该面试 session 所属用户。
- turn 必须属于该 session。
- 文件不能为空。
- 仅允许浏览器录音实际产生的音频类型，例如 `audio/webm`、`audio/mp4`、`audio/mpeg`、`audio/wav`。
- 文件大小不能超过 `app.file.audio-max-size`。

响应：

- 返回录音元数据或通用成功结果。
- 不返回公开 URL。

### 获取录音播放 URL

新增接口：

```http
GET /api/interview/{sessionId}/turn/{turnId}/audio-url
```

行为：

- 当前用户必须是该面试 session 所属用户。
- turn 必须属于该 session。
- 如果没有录音，返回业务错误或空结果，具体实现保持与现有响应风格一致。
- 如果有录音，生成短期签名 URL 返回给前端播放。

## 前端设计

### 面试房间

`InterviewRoomPage.vue` 新增“保存本轮录音”开关，默认关闭。用户开启后：

- 现有 ASR 流程继续工作。
- 前端同时使用浏览器录音能力保留一份音频 Blob。
- 用户停止回答并提交文本成功后，异步上传本轮录音。
- 录音上传失败不影响回答提交和面试状态流转，只提示“录音保存失败，可稍后重试”。

录音保存默认关闭，避免在用户未明确同意时保存敏感音频。

### 报告或回放入口

在可展示面试轮次的页面中，如果后端返回该轮存在录音，则展示播放入口。播放时先调用 `audio-url` 接口获取短期 URL，再交给 `<audio controls>` 播放。

如果签名 URL 获取失败，前端展示“录音暂不可播放”。

### 头像上传

`ProfilePage.vue` 现有头像上传交互保持不变。前端仍调用 `uploadAvatar(file)`，无需感知本地存储或 OSS。

## 错误处理

- 文件为空、类型不允许、后缀不允许、大小超限：返回 `PARAMS_ERROR`。
- OSS 配置缺失、OSS 上传失败、签名 URL 生成失败：返回 `SYSTEM_ERROR`，日志记录 provider、bucket、object key、异常信息。
- 面试或轮次不存在：返回 `INTERVIEW_NOT_FOUND_ERROR`。
- 面试状态或轮次不匹配：返回 `INTERVIEW_STATUS_ERROR` 或 `INTERVIEW_TURN_ERROR`。
- 用户无权访问他人面试录音：返回 `NO_AUTH_ERROR`。
- 前端录音上传失败不回滚回答提交。

## 测试策略

后端测试：

- 头像上传校验：空文件、非法类型、超限、合法图片。
- 录音上传校验：空文件、非法类型、超限、合法音频。
- provider 选择：`local` 和 `aliyun` 分支调用正确。
- 签名 URL：只对有 object key 的录音生成，且只允许本人访问。
- 面试关联校验：turn 必须属于 session，session 必须属于当前用户。
- SQL 初始化测试覆盖 `interview_turn` 新增字段。

前端验证：

- 面试页类型检查通过。
- 保存录音开关默认关闭。
- 开启后回答提交成功再上传录音。
- 上传失败不阻塞面试流程。

验证命令：

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run type-check
npm run build
```

## 实施顺序

1. 调整后端存储抽象和配置。
2. 迁移头像上传到统一存储服务。
3. 扩展 `interview_turn` 实体、SQL 和 VO。
4. 新增录音上传和签名 URL 接口。
5. 前端面试页增加主动保存录音开关和上传流程。
6. 前端报告或轮次展示页增加录音播放入口。
7. 补充测试并运行后端、前端验证命令。

## 风险与约束

- 浏览器录音格式在不同浏览器上可能不同，后端允许列表需要覆盖主流 `MediaRecorder` 输出格式。
- 签名 URL 依赖 OSS bucket 权限配置，生产环境应避免直接公开录音对象。
- 本地 provider 下签名 URL 只能模拟为受控访问或短期无效的本地 URL，实现时需保持开发可用，不承诺与 OSS 完全一致。
- 录音保存涉及隐私，应始终默认关闭，并在 UI 上明确提示用户。
