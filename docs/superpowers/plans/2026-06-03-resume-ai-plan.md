# AI 简历优化与生成功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为面试吧平台新增 AI 驱动的个人简历优化与生成功能，支持从零生成、对话填充、模块级优化、简历评分、多模板预览、PDF 导出和版本管理。

**Architecture:** 后端新增 resume 相关的 4 张表（resume、resume_section、resume_chat_message、resume_version），通过 ResumeService 处理 CRUD，ResumeAiService 封装 DeepSeek AI 调用。前端新增简历编辑器页面（左右分栏：编辑区 + 实时预览），3 种 Vue 模板组件接收统一的 ResumeData props 渲染。AI 对话通过 SSE 流式传输。

**Tech Stack:** Spring Boot 3.5 + Spring AI DeepSeek + MyBatis-Plus + MySQL JSON 列; Vue 3 + TypeScript + Element Plus + Pinia + html2canvas + jsPDF + vuedraggable

**Design Spec:** `docs/superpowers/specs/2026-06-03-resume-ai-design.md`

---

## File Structure

### Backend — 新建文件

| 文件 | 职责 |
|------|------|
| `backend/src/main/resources/sql/init.sql` | 追加 4 张表的 CREATE TABLE |
| `backend/.../exception/ErrorCode.java` | 追加 6 个错误码 |
| `backend/.../model/entity/Resume.java` | 简历主表实体 |
| `backend/.../model/entity/ResumeSection.java` | 简历模块实体 |
| `backend/.../model/entity/ResumeChatMessage.java` | AI 对话记录实体 |
| `backend/.../model/entity/ResumeVersion.java` | 版本快照实体 |
| `backend/.../mapper/ResumeMapper.java` | 简历 Mapper |
| `backend/.../mapper/ResumeSectionMapper.java` | 模块 Mapper |
| `backend/.../mapper/ResumeChatMessageMapper.java` | 对话 Mapper |
| `backend/.../mapper/ResumeVersionMapper.java` | 版本 Mapper |
| `backend/.../model/dto/resume/ResumeCreateRequest.java` | 创建简历请求 |
| `backend/.../model/dto/resume/ResumeUpdateRequest.java` | 更新简历请求 |
| `backend/.../model/dto/resume/SectionCreateRequest.java` | 创建模块请求 |
| `backend/.../model/dto/resume/SectionUpdateRequest.java` | 更新模块请求 |
| `backend/.../model/dto/resume/SectionSortRequest.java` | 模块排序请求 |
| `backend/.../model/dto/resume/AiGenerateRequest.java` | AI 生成请求 |
| `backend/.../model/dto/resume/AiOptimizeRequest.java` | AI 优化请求 |
| `backend/.../model/dto/resume/ChatRequest.java` | 对话请求 |
| `backend/.../model/vo/resume/ResumeVO.java` | 简历列表 VO |
| `backend/.../model/vo/resume/ResumeDetailVO.java` | 简历详情 VO |
| `backend/.../model/vo/resume/SectionVO.java` | 模块 VO |
| `backend/.../model/vo/resume/AiScoreVO.java` | AI 评分 VO |
| `backend/.../model/vo/resume/VersionVO.java` | 版本列表 VO |
| `backend/.../model/vo/resume/VersionSnapshotVO.java` | 版本快照 VO |
| `backend/.../service/ResumeService.java` | 简历服务接口 |
| `backend/.../service/impl/ResumeServiceImpl.java` | 简历服务实现 |
| `backend/.../service/ResumeAiService.java` | AI 服务接口 |
| `backend/.../service/impl/ResumeAiServiceImpl.java` | AI 服务实现 |
| `backend/.../controller/ResumeController.java` | 简历 CRUD 控制器 |
| `backend/.../controller/ResumeAiController.java` | AI 功能控制器 |
| `backend/.../config/AiConfig.java` | AI 相关配置 |

### Backend — 测试文件

| 文件 | 职责 |
|------|------|
| `backend/.../service/impl/ResumeServiceImplTest.java` | 简历服务测试 |
| `backend/.../controller/ResumeControllerTest.java` | 简历控制器测试 |
| `backend/.../service/impl/ResumeAiServiceImplTest.java` | AI 服务测试 |
| `backend/.../controller/ResumeAiControllerTest.java` | AI 控制器测试 |

### Frontend — 新建文件

| 文件 | 职责 |
|------|------|
| `frontend/src/types/resume.ts` | 简历类型定义 |
| `frontend/src/api/resume.ts` | 简历 API 调用 |
| `frontend/src/stores/resume.ts` | 简历 Pinia Store |
| `frontend/src/views/resume/ResumeListPage.vue` | 简历列表页 |
| `frontend/src/views/resume/ResumeEditPage.vue` | 简历编辑器（核心页面） |
| `frontend/src/views/resume/ResumePreviewPage.vue` | 全屏预览页 |
| `frontend/src/components/resume/sections/BasicInfoEditor.vue` | 基本信息编辑 |
| `frontend/src/components/resume/sections/EducationEditor.vue` | 教育经历编辑 |
| `frontend/src/components/resume/sections/WorkExperienceEditor.vue` | 工作经历编辑 |
| `frontend/src/components/resume/sections/ProjectEditor.vue` | 项目经历编辑 |
| `frontend/src/components/resume/sections/SkillsEditor.vue` | 技能标签编辑 |
| `frontend/src/components/resume/sections/SummaryEditor.vue` | 自我评价编辑 |
| `frontend/src/components/resume/AiChatPanel.vue` | AI 对话面板 |
| `frontend/src/components/resume/AiScorePanel.vue` | AI 评分展示 |
| `frontend/src/components/resume/VersionHistory.vue` | 版本历史 |
| `frontend/src/components/resume/TemplateSelector.vue` | 模板选择器 |
| `frontend/src/templates/MinimalTech.vue` | 简约技术风模板 |
| `frontend/src/templates/ModernTwoCol.vue` | 现代双栏模板 |
| `frontend/src/templates/ClassicFormal.vue` | 经典正式模板 |

### Frontend — 修改文件

| 文件 | 变更 |
|------|------|
| `frontend/src/router/index.ts` | 新增 4 条简历路由 |
| `frontend/src/views/home/HomePage.vue` | 简历入口按钮 |
| `frontend/src/layouts/MainLayout.vue` | 导航栏新增"我的简历"链接 |
| `frontend/package.json` | 新增 html2canvas、jspdf、vuedraggable 依赖 |

---

## Phase 1 (P0): Backend Data Layer + CRUD

### Task 1: ErrorCode 扩展 + SQL Schema

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- Modify: `backend/src/main/resources/sql/init.sql`

- [ ] **Step 1: 在 ErrorCode 中新增错误码**

在 `ErrorCode.java` 的 `SYSTEM_ERROR` 之前插入：

```java
NOT_FOUND_ERROR(40400, "资源不存在"),
RESUME_LIMIT_ERROR(40001, "简历数量超出限制"),
RESUME_SECTION_ERROR(40002, "模块操作异常"),
AI_SERVICE_ERROR(50001, "AI 服务调用失败"),
AI_RESPONSE_PARSE_ERROR(50002, "AI 响应解析失败"),
```

- [ ] **Step 2: 在 init.sql 末尾追加 4 张表**

在现有 `user` 表 CREATE TABLE 之后追加：

```sql
CREATE TABLE IF NOT EXISTS resume (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '简历 id',
  user_id BIGINT NOT NULL COMMENT '所属用户 id',
  title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '简历标题',
  template_type VARCHAR(32) NOT NULL DEFAULT 'minimal_tech' COMMENT '模板类型',
  status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published',
  source VARCHAR(16) NOT NULL DEFAULT 'scratch' COMMENT '来源：scratch/ai_chat',
  version INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';

CREATE TABLE IF NOT EXISTS resume_section (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模块 id',
  resume_id BIGINT NOT NULL COMMENT '所属简历 id',
  section_type VARCHAR(32) NOT NULL COMMENT '模块类型：basic/education/work/project/skills/summary',
  section_data JSON NOT NULL COMMENT '模块结构化数据',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同类型模块排序',
  ai_generated TINYINT NOT NULL DEFAULT 0 COMMENT '是否由 AI 生成：0-否，1-是',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_resume_id (resume_id),
  KEY idx_section_type (section_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历模块表';

CREATE TABLE IF NOT EXISTS resume_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息 id',
  resume_id BIGINT NOT NULL COMMENT '所属简历 id',
  role VARCHAR(16) NOT NULL COMMENT '角色：user/assistant',
  content TEXT NOT NULL COMMENT '消息内容',
  related_section_type VARCHAR(32) DEFAULT NULL COMMENT '关联模块类型',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话记录表';

CREATE TABLE IF NOT EXISTS resume_version (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本 id',
  resume_id BIGINT NOT NULL COMMENT '所属简历 id',
  version INT NOT NULL COMMENT '版本号',
  snapshot JSON NOT NULL COMMENT '完整简历数据快照',
  change_summary VARCHAR(256) NOT NULL DEFAULT '' COMMENT '变更摘要',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_resume_version (resume_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历版本表';
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java backend/src/main/resources/sql/init.sql
git commit -m "feat(resume): add error codes and database schema for resume module"
```

---

### Task 2: Entity 类

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/Resume.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/ResumeSection.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/ResumeChatMessage.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/ResumeVersion.java`

- [ ] **Step 1: 创建 Resume.java**

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String templateType;

    private String status;

    private String source;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 2: 创建 ResumeSection.java**

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
import java.util.Map;

@Data
@TableName(value = "resume_section", autoResultMap = true)
public class ResumeSection implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String sectionType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> sectionData;

    private Integer sortOrder;

    private Integer aiGenerated;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 3: 创建 ResumeChatMessage.java**

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("resume_chat_message")
public class ResumeChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String role;

    private String content;

    private String relatedSectionType;

    private LocalDateTime createTime;
}
```

- [ ] **Step 4: 创建 ResumeVersion.java**

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "resume_version", autoResultMap = true)
public class ResumeVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private Integer version;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> snapshot;

    private String changeSummary;

    private LocalDateTime createTime;
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/model/entity/Resume.java backend/src/main/java/com/mianshiba/ai/model/entity/ResumeSection.java backend/src/main/java/com/mianshiba/ai/model/entity/ResumeChatMessage.java backend/src/main/java/com/mianshiba/ai/model/entity/ResumeVersion.java
git commit -m "feat(resume): add entity classes for resume module"
```

---

### Task 3: Mapper 接口

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ResumeMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ResumeSectionMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ResumeChatMessageMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ResumeVersionMapper.java`

- [ ] **Step 1: 创建 4 个 Mapper 接口**

每个都继承 `BaseMapper<T>`，加上 `@Mapper` 注解，与 UserMapper 模式一致。

**ResumeMapper.java:**
```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
```

**ResumeSectionMapper.java:**
```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ResumeSection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeSectionMapper extends BaseMapper<ResumeSection> {
}
```

**ResumeChatMessageMapper.java:**
```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ResumeChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeChatMessageMapper extends BaseMapper<ResumeChatMessage> {
}
```

**ResumeVersionMapper.java:**
```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ResumeVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeVersionMapper extends BaseMapper<ResumeVersion> {
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/mapper/Resume*.java
git commit -m "feat(resume): add mapper interfaces for resume module"
```

---

### Task 4: DTOs 和 VOs

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumeCreateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ResumeUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/SectionCreateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/SectionUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/SectionSortRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiGenerateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/AiOptimizeRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/resume/ChatRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeDetailVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/SectionVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/AiScoreVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/VersionVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/VersionSnapshotVO.java`

- [ ] **Step 1: 创建请求 DTOs**

**ResumeCreateRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResumeCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最长 128 字符")
    private String title;

    private String templateType;
}
```

**ResumeUpdateRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResumeUpdateRequest {

    @Size(max = 128, message = "标题最长 128 字符")
    private String title;

    private String templateType;

    private String status;
}
```

**SectionCreateRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SectionCreateRequest {

    @NotBlank(message = "模块类型不能为空")
    private String sectionType;

    @NotNull(message = "模块数据不能为空")
    private Map<String, Object> sectionData;

    private Integer sortOrder;
}
```

**SectionUpdateRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import lombok.Data;

import java.util.Map;

@Data
public class SectionUpdateRequest {

    private Map<String, Object> sectionData;

    private Integer sortOrder;
}
```

**SectionSortRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SectionSortRequest {

    @NotNull(message = "排序列表不能为空")
    private List<SortItem> orders;

    @Data
    public static class SortItem {
        @NotNull
        private Long sectionId;
        @NotNull
        private Integer sortOrder;
    }
}
```

**AiGenerateRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiGenerateRequest {

    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

    private String techDirection;

    private Integer workYears;
}
```

**AiOptimizeRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class AiOptimizeRequest {

    @NotNull(message = "模块 id 不能为空")
    private Long sectionId;

    @NotBlank(message = "模块类型不能为空")
    private String sectionType;

    @NotNull(message = "模块数据不能为空")
    private Map<String, Object> sectionData;
}
```

**ChatRequest.java:**
```java
package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    private String message;
}
```

- [ ] **Step 2: 创建响应 VOs**

**ResumeVO.java:**
```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeVO {

    private Long id;

    private String title;

    private String templateType;

    private String status;

    private String source;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**SectionVO.java:**
```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SectionVO {

    private Long id;

    private Long resumeId;

    private String sectionType;

    private Map<String, Object> sectionData;

    private Integer sortOrder;

    private Integer aiGenerated;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**ResumeDetailVO.java:**
```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResumeDetailVO {

    private Long id;

    private String title;

    private String templateType;

    private String status;

    private String source;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<SectionVO> sections;
}
```

**AiScoreVO.java:**
```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.util.List;

@Data
public class AiScoreVO {

    private Integer score;

    private ScoreDimensions dimensions;

    private List<String> suggestions;

    @Data
    public static class ScoreDimensions {
        private Integer completeness;
        private String completenessComment;
        private Integer professionalism;
        private String professionalismComment;
        private Integer matching;
        private String matchingComment;
    }
}
```

**VersionVO.java:**
```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VersionVO {

    private Long id;

    private Integer version;

    private String changeSummary;

    private LocalDateTime createTime;
}
```

**VersionSnapshotVO.java:**
```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class VersionSnapshotVO {

    private Long id;

    private Integer version;

    private String changeSummary;

    private LocalDateTime createTime;

    private List<SectionVO> sections;
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/model/dto/resume/ backend/src/main/java/com/mianshiba/ai/model/vo/resume/
git commit -m "feat(resume): add DTOs and VOs for resume module"
```

---

### Task 5: ResumeService + 测试

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/ResumeService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeServiceImpl.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeServiceImplTest.java`

- [ ] **Step 1: 编写测试 — ResumeServiceImplTest.java**

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";
    private static final int MAX_RESUMES_PER_USER = 10;

    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeSectionMapper sectionMapper;
    @Mock
    private ResumeVersionMapper versionMapper;
    @Mock
    private UserMapper userMapper;

    private JwtUtils jwtUtils;
    private ResumeServiceImpl resumeService;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        resumeService = new ResumeServiceImpl(resumeMapper, sectionMapper, versionMapper, userMapper, jwtUtils);
    }

    @Test
    void createResumeSavesAndReturnsVO() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectCount(any())).thenReturn(3L);
        when(resumeMapper.insert(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(2001L);
            return 1;
        });

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("Java 后端简历");
        request.setTemplateType("minimal_tech");

        ResumeVO vo = resumeService.createResume(auth, request);

        assertThat(vo.getId()).isEqualTo(2001L);
        assertThat(vo.getTitle()).isEqualTo("Java 后端简历");
        assertThat(vo.getTemplateType()).isEqualTo("minimal_tech");
    }

    @Test
    void createResumeThrowsWhenLimitReached() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectCount(any())).thenReturn((long) MAX_RESUMES_PER_USER);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("多余简历");

        assertThatThrownBy(() -> resumeService.createResume(auth, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("简历数量超出限制")
                .extracting("code").isEqualTo(ErrorCode.RESUME_LIMIT_ERROR.getCode());
        verify(resumeMapper, never()).insert(any());
    }

    @Test
    void listResumesReturnsOnlyOwnResumes() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectList(any())).thenReturn(List.of(
                createResume(2001L, "简历一"),
                createResume(2002L, "简历二")
        ));

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        List<ResumeVO> list = resumeService.listResumes(auth);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getTitle()).isEqualTo("简历一");
    }

    @Test
    void getResumeDetailIncludesSections() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume resume = createResume(2001L, "Java 后端简历");
        when(resumeMapper.selectById(2001L)).thenReturn(resume);
        ResumeSection section = new ResumeSection();
        section.setId(3001L);
        section.setResumeId(2001L);
        section.setSectionType("work");
        section.setSectionData(Map.of("company", "字节跳动"));
        section.setSortOrder(0);
        section.setAiGenerated(0);
        when(sectionMapper.selectList(any())).thenReturn(List.of(section));

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        ResumeDetailVO detail = resumeService.getResumeDetail(auth, 2001L);

        assertThat(detail.getId()).isEqualTo(2001L);
        assertThat(detail.getSections()).hasSize(1);
        assertThat(detail.getSections().get(0).getSectionData())
                .containsEntry("company", "字节跳动");
    }

    @Test
    void getResumeDetailThrowsWhenNotOwner() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume otherResume = createResume(2001L, "别人的简历");
        otherResume.setUserId(9999L);
        when(resumeMapper.selectById(2001L)).thenReturn(otherResume);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        assertThatThrownBy(() -> resumeService.getResumeDetail(auth, 2001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
    }

    @Test
    void addSectionInsertsAndReturnsVO() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(2001L)).thenReturn(createResume(2001L, "测试简历"));
        when(sectionMapper.insert(any(ResumeSection.class))).thenAnswer(inv -> {
            ResumeSection s = inv.getArgument(0);
            s.setId(3001L);
            return 1;
        });

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        SectionCreateRequest request = new SectionCreateRequest();
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "阿里巴巴"));

        SectionVO vo = resumeService.addSection(auth, 2001L, request);

        assertThat(vo.getId()).isEqualTo(3001L);
        assertThat(vo.getSectionType()).isEqualTo("work");
    }

    @Test
    void updateSectionModifiesData() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(2001L)).thenReturn(createResume(2001L, "测试简历"));
        ResumeSection existing = new ResumeSection();
        existing.setId(3001L);
        existing.setResumeId(2001L);
        existing.setSectionType("work");
        existing.setSectionData(Map.of("company", "旧公司"));
        existing.setSortOrder(0);
        existing.setAiGenerated(0);
        when(sectionMapper.selectById(3001L)).thenReturn(existing);
        when(sectionMapper.updateById(any(ResumeSection.class))).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        SectionUpdateRequest request = new SectionUpdateRequest();
        request.setSectionData(Map.of("company", "新公司"));

        SectionVO vo = resumeService.updateSection(auth, 2001L, 3001L, request);

        assertThat(vo.getSectionData()).containsEntry("company", "新公司");
    }

    @Test
    void deleteSectionRemovesFromResume() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(2001L)).thenReturn(createResume(2001L, "测试简历"));
        ResumeSection section = new ResumeSection();
        section.setId(3001L);
        section.setResumeId(2001L);
        when(sectionMapper.selectById(3001L)).thenReturn(section);
        when(sectionMapper.deleteById(3001L)).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        resumeService.deleteSection(auth, 2001L, 3001L);

        verify(sectionMapper).deleteById(3001L);
    }

    @Test
    void deleteResumeLogicallyDeletes() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(2001L)).thenReturn(createResume(2001L, "待删除"));
        when(resumeMapper.deleteById(2001L)).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        resumeService.deleteResume(auth, 2001L);

        verify(resumeMapper).deleteById(2001L);
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("dev_001");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
    }

    private Resume createResume(Long id, String title) {
        Resume resume = new Resume();
        resume.setId(id);
        resume.setUserId(1001L);
        resume.setTitle(title);
        resume.setTemplateType("minimal_tech");
        resume.setStatus("draft");
        resume.setSource("scratch");
        resume.setVersion(1);
        return resume;
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeServiceImplTest -DfailIfNoTests=false`
Expected: 编译失败（ResumeService 接口不存在）

- [ ] **Step 3: 创建 ResumeService 接口**

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.ResumeUpdateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionSortRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;

import java.util.List;

public interface ResumeService {

    ResumeVO createResume(String authorizationHeader, ResumeCreateRequest request);

    List<ResumeVO> listResumes(String authorizationHeader);

    ResumeDetailVO getResumeDetail(String authorizationHeader, Long resumeId);

    ResumeVO updateResume(String authorizationHeader, Long resumeId, ResumeUpdateRequest request);

    void deleteResume(String authorizationHeader, Long resumeId);

    SectionVO addSection(String authorizationHeader, Long resumeId, SectionCreateRequest request);

    SectionVO updateSection(String authorizationHeader, Long resumeId, Long sectionId, SectionUpdateRequest request);

    void deleteSection(String authorizationHeader, Long resumeId, Long sectionId);

    void sortSections(String authorizationHeader, Long resumeId, SectionSortRequest request);
}
```

- [ ] **Step 4: 创建 ResumeServiceImpl**

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.ResumeUpdateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionSortRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final int MAX_RESUMES_PER_USER = 10;
    private static final Set<String> VALID_SECTION_TYPES = Set.of(
            "basic", "education", "work", "project", "skills", "summary"
    );
    private static final Set<String> VALID_TEMPLATE_TYPES = Set.of(
            "minimal_tech", "modern_two_col", "classic_formal"
    );
    private static final Set<String> VALID_STATUSES = Set.of("draft", "published");

    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper sectionMapper;
    private final ResumeVersionMapper versionMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO createResume(String authorizationHeader, ResumeCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        Long count = resumeMapper.selectCount(Wrappers.lambdaQuery(Resume.class)
                .eq(Resume::getUserId, userId));
        if (count != null && count >= MAX_RESUMES_PER_USER) {
            throw new BusinessException(ErrorCode.RESUME_LIMIT_ERROR, "简历数量超出限制");
        }

        String templateType = request.getTemplateType();
        if (templateType == null || templateType.isBlank()) {
            templateType = "minimal_tech";
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(request.getTitle());
        resume.setTemplateType(templateType);
        resume.setStatus("draft");
        resume.setSource("scratch");
        resume.setVersion(1);
        resume.setIsDelete(0);
        resumeMapper.insert(resume);
        return toResumeVO(resume);
    }

    @Override
    public List<ResumeVO> listResumes(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<Resume> resumes = resumeMapper.selectList(Wrappers.lambdaQuery(Resume.class)
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdateTime));
        return resumes.stream().map(this::toResumeVO).collect(Collectors.toList());
    }

    @Override
    public ResumeDetailVO getResumeDetail(String authorizationHeader, Long resumeId) {
        Long userId = resolveUserId(authorizationHeader);
        Resume resume = getResumeAndCheckOwner(resumeId, userId);
        List<ResumeSection> sections = sectionMapper.selectList(
                Wrappers.lambdaQuery(ResumeSection.class)
                        .eq(ResumeSection::getResumeId, resumeId)
                        .orderByAsc(ResumeSection::getSortOrder));
        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setId(resume.getId());
        detail.setTitle(resume.getTitle());
        detail.setTemplateType(resume.getTemplateType());
        detail.setStatus(resume.getStatus());
        detail.setSource(resume.getSource());
        detail.setVersion(resume.getVersion());
        detail.setCreateTime(resume.getCreateTime());
        detail.setUpdateTime(resume.getUpdateTime());
        detail.setSections(sections.stream().map(this::toSectionVO).collect(Collectors.toList()));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO updateResume(String authorizationHeader, Long resumeId, ResumeUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        Resume resume = getResumeAndCheckOwner(resumeId, userId);
        if (request.getTitle() != null) {
            resume.setTitle(request.getTitle());
        }
        if (request.getTemplateType() != null && VALID_TEMPLATE_TYPES.contains(request.getTemplateType())) {
            resume.setTemplateType(request.getTemplateType());
        }
        if (request.getStatus() != null && VALID_STATUSES.contains(request.getStatus())) {
            resume.setStatus(request.getStatus());
        }
        resumeMapper.updateById(resume);
        return toResumeVO(resume);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResume(String authorizationHeader, Long resumeId) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        resumeMapper.deleteById(resumeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionVO addSection(String authorizationHeader, Long resumeId, SectionCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        if (!VALID_SECTION_TYPES.contains(request.getSectionType())) {
            throw new BusinessException(ErrorCode.RESUME_SECTION_ERROR, "不支持的模块类型");
        }

        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setSectionType(request.getSectionType());
        section.setSectionData(request.getSectionData());
        section.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        section.setAiGenerated(0);
        section.setIsDelete(0);
        sectionMapper.insert(section);
        return toSectionVO(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionVO updateSection(String authorizationHeader, Long resumeId, Long sectionId, SectionUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        ResumeSection section = sectionMapper.selectById(sectionId);
        if (section == null || !section.getResumeId().equals(resumeId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "模块不存在");
        }
        if (request.getSectionData() != null) {
            section.setSectionData(request.getSectionData());
        }
        if (request.getSortOrder() != null) {
            section.setSortOrder(request.getSortOrder());
        }
        sectionMapper.updateById(section);
        return toSectionVO(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSection(String authorizationHeader, Long resumeId, Long sectionId) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        ResumeSection section = sectionMapper.selectById(sectionId);
        if (section == null || !section.getResumeId().equals(resumeId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "模块不存在");
        }
        sectionMapper.deleteById(sectionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortSections(String authorizationHeader, Long resumeId, SectionSortRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        for (SectionSortRequest.SortItem item : request.getOrders()) {
            ResumeSection section = sectionMapper.selectById(item.getSectionId());
            if (section != null && section.getResumeId().equals(resumeId)) {
                section.setSortOrder(item.getSortOrder());
                sectionMapper.updateById(section);
            }
        }
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

    private Resume getResumeAndCheckOwner(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "简历不存在");
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问此简历");
        }
        return resume;
    }

    private ResumeVO toResumeVO(Resume resume) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setTemplateType(resume.getTemplateType());
        vo.setStatus(resume.getStatus());
        vo.setSource(resume.getSource());
        vo.setVersion(resume.getVersion());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        return vo;
    }

    private SectionVO toSectionVO(ResumeSection section) {
        SectionVO vo = new SectionVO();
        vo.setId(section.getId());
        vo.setResumeId(section.getResumeId());
        vo.setSectionType(section.getSectionType());
        vo.setSectionData(section.getSectionData());
        vo.setSortOrder(section.getSortOrder());
        vo.setAiGenerated(section.getAiGenerated());
        vo.setCreateTime(section.getCreateTime());
        vo.setUpdateTime(section.getUpdateTime());
        return vo;
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeServiceImplTest`
Expected: 全部 PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/service/ResumeService.java backend/src/main/java/com/mianshiba/ai/service/impl/ResumeServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeServiceImplTest.java
git commit -m "feat(resume): add ResumeService with CRUD operations and tests"
```

---

### Task 6: ResumeController + 测试

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/controller/ResumeController.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/ResumeControllerTest.java`

- [ ] **Step 1: 编写测试 — ResumeControllerTest.java**

```java
package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ResumeController(resumeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createResumeReturnsVO() throws Exception {
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("Java 后端简历");
        request.setTemplateType("minimal_tech");
        ResumeVO vo = new ResumeVO();
        vo.setId(1L);
        vo.setTitle("Java 后端简历");
        when(resumeService.createResume(any(), any(ResumeCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/resume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Java 后端简历"));
    }

    @Test
    void listResumesReturnsArray() throws Exception {
        ResumeVO vo = new ResumeVO();
        vo.setId(1L);
        vo.setTitle("简历一");
        when(resumeService.listResumes(any())).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/resume/list")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].title").value("简历一"));
    }

    @Test
    void getResumeDetailReturnsSections() throws Exception {
        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setId(1L);
        detail.setTitle("测试简历");
        SectionVO section = new SectionVO();
        section.setId(10L);
        section.setSectionType("work");
        section.setSectionData(Map.of("company", "字节跳动"));
        detail.setSections(List.of(section));
        when(resumeService.getResumeDetail(any(), eq(1L))).thenReturn(detail);

        mockMvc.perform(get("/api/resume/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sections[0].sectionType").value("work"));
    }

    @Test
    void addSectionReturnsSectionVO() throws Exception {
        SectionCreateRequest request = new SectionCreateRequest();
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "阿里巴巴"));
        SectionVO vo = new SectionVO();
        vo.setId(10L);
        vo.setSectionType("work");
        when(resumeService.addSection(any(), eq(1L), any(SectionCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/resume/1/section")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sectionType").value("work"));
    }

    @Test
    void deleteResumeReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/resume/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void createResumeReturnsErrorWhenTitleBlank() throws Exception {
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("");

        mockMvc.perform(post("/api/resume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeControllerTest -DfailIfNoTests=false`
Expected: 编译失败（ResumeController 不存在）

- [ ] **Step 3: 创建 ResumeController**

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.ResumeUpdateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionSortRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume")
@Tag(name = "简历接口")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    @Operation(summary = "创建简历")
    public BaseResponse<ResumeVO> createResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ResumeCreateRequest request) {
        return ResultUtils.success(resumeService.createResume(authorizationHeader, request));
    }

    @GetMapping("/list")
    @Operation(summary = "获取简历列表")
    public BaseResponse<List<ResumeVO>> listResumes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(resumeService.listResumes(authorizationHeader));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取简历详情")
    public BaseResponse<ResumeDetailVO> getResumeDetail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id) {
        return ResultUtils.success(resumeService.getResumeDetail(authorizationHeader, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新简历")
    public BaseResponse<ResumeVO> updateResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody ResumeUpdateRequest request) {
        return ResultUtils.success(resumeService.updateResume(authorizationHeader, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除简历")
    public BaseResponse<Void> deleteResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id) {
        resumeService.deleteResume(authorizationHeader, id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{resumeId}/section")
    @Operation(summary = "添加模块")
    public BaseResponse<SectionVO> addSection(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody SectionCreateRequest request) {
        return ResultUtils.success(resumeService.addSection(authorizationHeader, resumeId, request));
    }

    @PutMapping("/{resumeId}/section/{sectionId}")
    @Operation(summary = "更新模块")
    public BaseResponse<SectionVO> updateSection(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId,
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionUpdateRequest request) {
        return ResultUtils.success(resumeService.updateSection(authorizationHeader, resumeId, sectionId, request));
    }

    @DeleteMapping("/{resumeId}/section/{sectionId}")
    @Operation(summary = "删除模块")
    public BaseResponse<Void> deleteSection(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId,
            @PathVariable Long sectionId) {
        resumeService.deleteSection(authorizationHeader, resumeId, sectionId);
        return ResultUtils.success(null);
    }

    @PutMapping("/{resumeId}/section/sort")
    @Operation(summary = "调整模块排序")
    public BaseResponse<Void> sortSections(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody SectionSortRequest request) {
        resumeService.sortSections(authorizationHeader, resumeId, request);
        return ResultUtils.success(null);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeControllerTest`
Expected: 全部 PASS

- [ ] **Step 5: 运行全部后端测试确认无回归**

Run: `cd backend && .\mvnw.cmd test`
Expected: 全部 PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/controller/ResumeController.java backend/src/test/java/com/mianshiba/ai/controller/ResumeControllerTest.java
git commit -m "feat(resume): add ResumeController with CRUD endpoints and tests"
```

---

## Phase 2 (P2): Backend AI Layer

### Task 7: AiConfig + ResumeAiService + 测试

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/config/AiConfig.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`

- [ ] **Step 1: 创建 AiConfig 配置类**

Spring AI DeepSeek starter 会自动配置 `ChatModel` bean。我们注册一个 `ChatClient` bean 以便使用 builder 模式。

```java
package com.mianshiba.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

- [ ] **Step 2: 编写测试 — ResumeAiServiceImplTest.java**

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.message.AssistantMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAiServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private ChatModel chatModel;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeSectionMapper sectionMapper;
    @Mock
    private UserMapper userMapper;

    private JwtUtils jwtUtils;
    private ResumeAiServiceImpl resumeAiService;
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        chatClient = ChatClient.builder(chatModel).build();
        resumeAiService = new ResumeAiServiceImpl(chatClient, resumeMapper, sectionMapper, userMapper, jwtUtils);
    }

    @Test
    void generateResumeReturnsSections() {
        String aiResponse = """
                ```json
                [
                  {"sectionType":"basic","sectionData":{"name":"张三","email":"test@test.com"}},
                  {"sectionType":"summary","sectionData":{"content":"5年Java开发经验"}}
                ]
                ```
                """;
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(aiResponse))
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(resumeMapper.insert(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        when(sectionMapper.selectList(any())).thenReturn(List.of());

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "dev_001", "user");
        when(userMapper.selectById(1001L)).thenReturn(normalUser());

        AiGenerateRequest request = new AiGenerateRequest();
        request.setTargetPosition("Java 后端工程师");
        request.setTechDirection("Spring Boot");
        request.setWorkYears(3);

        ResumeDetailVO result = resumeAiService.generateResume(auth, request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).contains("Java 后端工程师");
    }

    @Test
    void optimizeSectionReturnsOptimizedData() {
        String aiResponse = """
                ```json
                {"company":"字节跳动","description":"负责核心系统设计","highlights":["QPS提升300%"]}
                ```
                """;
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(aiResponse))
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "字节跳动", "description", "做后端开发"));

        Map<String, Object> result = resumeAiService.optimizeSection(request, "Java 后端工程师");

        assertThat(result).containsEntry("company", "字节跳动");
        assertThat(result).containsKey("highlights");
    }

    @Test
    void scoreResumeReturnsScoreAndSuggestions() {
        String aiResponse = """
                ```json
                {
                  "score": 75,
                  "dimensions": {
                    "completeness": 70,
                    "completenessComment": "缺少教育经历",
                    "professionalism": 80,
                    "professionalismComment": "工作描述较专业",
                    "matching": 75,
                    "matchingComment": "技能匹配度尚可"
                  },
                  "suggestions": ["添加教育经历", "补充量化成果"]
                }
                ```
                """;
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(aiResponse))
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        List<SectionVO> sections = List.of(
                createSectionVO("work", Map.of("company", "字节跳动"))
        );

        AiScoreVO score = resumeAiService.scoreResume(sections, "Java 后端工程师");

        assertThat(score.getScore()).isEqualTo(75);
        assertThat(score.getSuggestions()).hasSize(2);
        assertThat(score.getDimensions().getProfessionalism()).isEqualTo(80);
    }

    @Test
    void optimizeSectionThrowsWhenInvalidJsonResponse() {
        String badResponse = "这不是JSON";
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(badResponse))
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "字节跳动"));

        assertThatThrownBy(() -> resumeAiService.optimizeSection(request, "Java"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.AI_RESPONSE_PARSE_ERROR.getCode());
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("dev_001");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
    }

    private SectionVO createSectionVO(String type, Map<String, Object> data) {
        SectionVO vo = new SectionVO();
        vo.setSectionType(type);
        vo.setSectionData(data);
        return vo;
    }
}
```

- [ ] **Step 3: 运行测试验证失败**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeAiServiceImplTest -DfailIfNoTests=false`
Expected: 编译失败

- [ ] **Step 4: 创建 ResumeAiService 接口**

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface ResumeAiService {

    ResumeDetailVO generateResume(String authorizationHeader, AiGenerateRequest request);

    Map<String, Object> optimizeSection(AiOptimizeRequest request, String targetPosition);

    AiScoreVO scoreResume(List<SectionVO> sections, String targetPosition);

    Flux<String> chatStream(String authorizationHeader, Long resumeId, String message);
}
```

- [ ] **Step 5: 创建 ResumeAiServiceImpl**

```java
package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiServiceImpl implements ResumeAiService {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[\\s*\\{.*?}\\s*]", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*?}", Pattern.DOTALL);
    private static final String GENERATE_SYSTEM_PROMPT = """
            你是一位资深技术简历顾问。根据用户提供的求职信息，生成一份完整的技术简历。
            目标岗位：%s
            技术方向：%s
            工作年限：%d 年
            
            请生成包含以下模块的简历：
            1. basic（基本信息）：name, email, phone, targetPosition, city, github
            2. education（教育经历）：school, major, degree, startDate, endDate, highlights
            3. work（工作经历）：company, position, startDate, endDate, description, highlights
            4. project（项目经历）：name, role, techStack, startDate, endDate, description, highlights
            5. skills（技能标签）：categories 数组，每项含 name 和 items
            6. summary（自我评价）：content
            
            请以 JSON 数组格式返回，每个元素包含 sectionType 和 sectionData 字段。
            生成的内容要真实感强、专业度高，符合目标岗位的要求。
            仅返回 JSON，不要包含其他文字说明。
            """;

    private static final String OPTIMIZE_SYSTEM_PROMPT = """
            你是一位简历润色专家。请优化以下简历模块内容。
            目标岗位：%s
            模块类型：%s
            
            优化要求：
            - 使用 STAR 法则描述经历（情境-任务-行动-结果）
            - 突出量化成果（如性能提升百分比、用户量等）
            - 保持技术准确性
            - 控制篇幅简洁有力
            
            请返回与输入相同结构的 JSON 对象。仅返回 JSON，不要包含其他文字。
            """;

    private static final String SCORE_SYSTEM_PROMPT = """
            你同时扮演 HR 和技术面试官的角色。请对以下简历进行评分。
            目标岗位：%s
            
            评分维度：
            1. 完整性（0-100）：模块是否齐全，信息是否完整
            2. 专业性（0-100）：用词是否专业，描述是否规范
            3. 岗位匹配度（0-100）：技能和经历是否匹配目标岗位
            
            请以 JSON 格式返回：
            {
              "score": 总分(0-100),
              "dimensions": {
                "completeness": 分数,
                "completenessComment": "评语",
                "professionalism": 分数,
                "professionalismComment": "评语",
                "matching": 分数,
                "matchingComment": "评语"
              },
              "suggestions": ["建议1", "建议2"]
            }
            仅返回 JSON，不要包含其他文字。
            """;

    private static final String CHAT_SYSTEM_PROMPT = """
            你是一位简历助手，帮助用户完善简历内容。当前简历数据如下：
            %s
            
            你需要：
            1. 引导用户描述他们的经历和技能
            2. 从用户的描述中提取结构化信息
            3. 当提取到有效信息时，在回复末尾用以下格式标记：
            
            [EXTRACTED_DATA]
            {"sectionType":"work","sectionData":{"company":"...","position":"..."}}
            [/EXTRACTED_DATA]
            
            回复要简洁友好。
            """;

    private final ChatClient chatClient;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper sectionMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO generateResume(String authorizationHeader, AiGenerateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        String systemPrompt = String.format(GENERATE_SYSTEM_PROMPT,
                request.getTargetPosition(),
                request.getTechDirection() != null ? request.getTechDirection() : "未指定",
                request.getWorkYears() != null ? request.getWorkYears() : 0);

        String aiResponse = callAi(systemPrompt, "请生成简历");
        List<Map<String, Object>> sectionsData = parseJsonArray(aiResponse);

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(request.getTargetPosition() + " - 简历");
        resume.setTemplateType("minimal_tech");
        resume.setStatus("draft");
        resume.setSource("scratch");
        resume.setVersion(1);
        resume.setIsDelete(0);
        resumeMapper.insert(resume);

        List<SectionVO> sectionVOs = new ArrayList<>();
        for (int i = 0; i < sectionsData.size(); i++) {
            Map<String, Object> item = sectionsData.get(i);
            String sectionType = (String) item.get("sectionType");
            @SuppressWarnings("unchecked")
            Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");

            if (sectionType == null || sectionData == null) {
                continue;
            }

            ResumeSection section = new ResumeSection();
            section.setResumeId(resume.getId());
            section.setSectionType(sectionType);
            section.setSectionData(sectionData);
            section.setSortOrder(i);
            section.setAiGenerated(1);
            section.setIsDelete(0);
            sectionMapper.insert(section);
            sectionVOs.add(toSectionVO(section));
        }

        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setId(resume.getId());
        detail.setTitle(resume.getTitle());
        detail.setTemplateType(resume.getTemplateType());
        detail.setStatus(resume.getStatus());
        detail.setSource(resume.getSource());
        detail.setVersion(resume.getVersion());
        detail.setSections(sectionVOs);
        return detail;
    }

    @Override
    public Map<String, Object> optimizeSection(AiOptimizeRequest request, String targetPosition) {
        try {
            String sectionJson = objectMapper.writeValueAsString(request.getSectionData());
            String systemPrompt = String.format(OPTIMIZE_SYSTEM_PROMPT,
                    targetPosition != null ? targetPosition : "未指定",
                    request.getSectionType());

            String aiResponse = callAi(systemPrompt, "请优化以下内容：\n" + sectionJson);
            return parseJsonObject(aiResponse);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("优化模块失败", e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "AI 响应解析失败");
        }
    }

    @Override
    public AiScoreVO scoreResume(List<SectionVO> sections, String targetPosition) {
        try {
            String sectionsJson = objectMapper.writeValueAsString(sections);
            String systemPrompt = String.format(SCORE_SYSTEM_PROMPT,
                    targetPosition != null ? targetPosition : "未指定");

            String aiResponse = callAi(systemPrompt, "请评分以下简历：\n" + sectionsJson);
            return parseJsonObject(aiResponse, AiScoreVO.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("简历评分失败", e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "AI 响应解析失败");
        }
    }

    @Override
    public Flux<String> chatStream(String authorizationHeader, Long resumeId, String message) {
        Long userId = resolveUserId(authorizationHeader);
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问此简历");
        }

        List<ResumeSection> sections = sectionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeSection>()
                        .eq(ResumeSection::getResumeId, resumeId));
        String sectionsSummary = sections.stream()
                .map(s -> s.getSectionType() + ": " + s.getSectionData().toString())
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format(CHAT_SYSTEM_PROMPT, sectionsSummary);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(message)
        ));

        return chatClient.prompt(prompt)
                .stream()
                .chatResponse()
                .map(response -> {
                    if (response.getResult() != null && response.getResult().getOutput() != null) {
                        return response.getResult().getOutput().getText();
                    }
                    return "";
                })
                .filter(text -> text != null && !text.isEmpty());
    }

    private String callAi(String systemPrompt, String userMessage) {
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userMessage)
            ));
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                    || response.getResult().getOutput().getText() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务返回空结果");
            }
            return response.getResult().getOutput().getText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务调用失败");
        }
    }

    private String extractJson(String text) {
        Matcher blockMatcher = JSON_BLOCK_PATTERN.matcher(text);
        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }
        return text.trim();
    }

    private List<Map<String, Object>> parseJsonArray(String text) {
        try {
            String json = extractJson(text);
            Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(json);
            if (arrayMatcher.find()) {
                json = arrayMatcher.group();
            }
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("JSON 数组解析失败: {}", text, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "AI 响应解析失败");
        }
    }

    private Map<String, Object> parseJsonObject(String text) {
        try {
            String json = extractJson(text);
            Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(json);
            if (objectMatcher.find()) {
                json = objectMatcher.group();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON 对象解析失败: {}", text, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "AI 响应解析失败");
        }
    }

    private <T> T parseJsonObject(String text, Class<T> clazz) {
        try {
            String json = extractJson(text);
            Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(json);
            if (objectMatcher.find()) {
                json = objectMatcher.group();
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON 对象解析失败: {}", text, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "AI 响应解析失败");
        }
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

    private SectionVO toSectionVO(ResumeSection section) {
        SectionVO vo = new SectionVO();
        vo.setId(section.getId());
        vo.setResumeId(section.getResumeId());
        vo.setSectionType(section.getSectionType());
        vo.setSectionData(section.getSectionData());
        vo.setSortOrder(section.getSortOrder());
        vo.setAiGenerated(section.getAiGenerated());
        vo.setCreateTime(section.getCreateTime());
        vo.setUpdateTime(section.getUpdateTime());
        return vo;
    }
}
```

- [ ] **Step 6: 运行测试验证通过**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeAiServiceImplTest`
Expected: 全部 PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/config/AiConfig.java backend/src/main/java/com/mianshiba/ai/service/ResumeAiService.java backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java
git commit -m "feat(resume): add ResumeAiService with generate, optimize, score and chat"
```

---

### Task 8: ResumeAiController + 测试

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/ResumeAiControllerTest.java`

- [ ] **Step 1: 编写测试 — ResumeAiControllerTest.java**

```java
package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResumeAiControllerTest {

    @Mock
    private ResumeAiService resumeAiService;
    @Mock
    private ResumeService resumeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ResumeAiController(resumeAiService, resumeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void generateResumeReturnsDetail() throws Exception {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setTargetPosition("Java 后端工程师");
        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setId(1L);
        detail.setTitle("Java 后端工程师 - 简历");
        when(resumeAiService.generateResume(any(), any(AiGenerateRequest.class))).thenReturn(detail);

        mockMvc.perform(post("/api/resume/ai/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Java 后端工程师 - 简历"));
    }

    @Test
    void optimizeSectionReturnsData() throws Exception {
        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "字节跳动"));
        when(resumeAiService.optimizeSection(any(AiOptimizeRequest.class), any()))
                .thenReturn(Map.of("company", "字节跳动", "highlights", List.of("QPS 提升 300%")));

        mockMvc.perform(post("/api/resume/1/ai/optimize-section")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.company").value("字节跳动"));
    }

    @Test
    void scoreResumeReturnsScore() throws Exception {
        AiScoreVO scoreVO = new AiScoreVO();
        scoreVO.setScore(85);
        scoreVO.setSuggestions(List.of("添加教育经历"));
        when(resumeService.getResumeDetail(any(), eq(1L))).thenReturn(new ResumeDetailVO());
        when(resumeAiService.scoreResume(any(), any())).thenReturn(scoreVO);

        mockMvc.perform(post("/api/resume/1/ai/score")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.score").value(85));
    }

    @Test
    void generateReturnsErrorWhenTargetPositionBlank() throws Exception {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setTargetPosition("");

        mockMvc.perform(post("/api/resume/ai/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeAiControllerTest -DfailIfNoTests=false`
Expected: 编译失败

- [ ] **Step 3: 创建 ResumeAiController**

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.dto.resume.ChatRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume")
@Tag(name = "简历 AI 接口")
public class ResumeAiController {

    private final ResumeAiService resumeAiService;
    private final ResumeService resumeService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @PostMapping("/ai/generate")
    @Operation(summary = "AI 一键生成简历")
    public BaseResponse<ResumeDetailVO> generateResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody AiGenerateRequest request) {
        return ResultUtils.success(resumeAiService.generateResume(authorizationHeader, request));
    }

    @PostMapping("/{resumeId}/ai/optimize-section")
    @Operation(summary = "AI 优化单个模块")
    public BaseResponse<Map<String, Object>> optimizeSection(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody AiOptimizeRequest request) {
        ResumeDetailVO detail = resumeService.getResumeDetail(authorizationHeader, resumeId);
        String targetPosition = extractTargetPosition(detail.getSections());
        return ResultUtils.success(resumeAiService.optimizeSection(request, targetPosition));
    }

    @PostMapping("/{resumeId}/ai/score")
    @Operation(summary = "AI 简历评分")
    public BaseResponse<AiScoreVO> scoreResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId) {
        ResumeDetailVO detail = resumeService.getResumeDetail(authorizationHeader, resumeId);
        String targetPosition = extractTargetPosition(detail.getSections());
        return ResultUtils.success(resumeAiService.scoreResume(detail.getSections(), targetPosition));
    }

    @PostMapping(value = "/{resumeId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话（SSE 流式）")
    public SseEmitter chat(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        sseExecutor.execute(() -> {
            try {
                Flux<String> stream = resumeAiService.chatStream(authorizationHeader, resumeId, request.getMessage());
                stream.doOnNext(text -> {
                    try {
                        emitter.send(SseEmitter.event().data(text));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }).doOnComplete(emitter::complete).doOnError(emitter::completeWithError).subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private String extractTargetPosition(List<SectionVO> sections) {
        return sections.stream()
                .filter(s -> "basic".equals(s.getSectionType()))
                .findFirst()
                .map(s -> (String) s.getSectionData().get("targetPosition"))
                .orElse("未指定");
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd backend && .\mvnw.cmd test -pl . -Dtest=ResumeAiControllerTest`
Expected: 全部 PASS

- [ ] **Step 5: 运行全部后端测试**

Run: `cd backend && .\mvnw.cmd test`
Expected: 全部 PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/mianshiba/ai/controller/ResumeAiController.java backend/src/test/java/com/mianshiba/ai/controller/ResumeAiControllerTest.java
git commit -m "feat(resume): add ResumeAiController with generate, optimize, score and chat SSE"
```

---

## Phase 3 (P1): Frontend Foundation

### Task 9: 前端类型定义 + API + Store

**Files:**
- Create: `frontend/src/types/resume.ts`
- Create: `frontend/src/api/resume.ts`
- Create: `frontend/src/stores/resume.ts`

- [ ] **Step 1: 创建类型定义 — types/resume.ts**

```typescript
export interface BasicSectionData {
  name: string
  email: string
  phone: string
  targetPosition: string
  city: string
  github: string
  blog: string
}

export interface EducationSectionData {
  school: string
  major: string
  degree: string
  startDate: string
  endDate: string
  gpa: string
  highlights: string[]
}

export interface WorkSectionData {
  company: string
  position: string
  startDate: string
  endDate: string
  description: string
  highlights: string[]
}

export interface ProjectSectionData {
  name: string
  role: string
  techStack: string[]
  startDate: string
  endDate: string
  description: string
  highlights: string[]
}

export interface SkillCategory {
  name: string
  items: string[]
}

export interface SkillsSectionData {
  categories: SkillCategory[]
}

export interface SummarySectionData {
  content: string
}

export type SectionType = 'basic' | 'education' | 'work' | 'project' | 'skills' | 'summary'

export type SectionDataMap = {
  basic: BasicSectionData
  education: EducationSectionData
  work: WorkSectionData
  project: ProjectSectionData
  skills: SkillsSectionData
  summary: SummarySectionData
}

export interface SectionVO {
  id: number
  resumeId: number
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sortOrder: number
  aiGenerated: number
  createTime: string
  updateTime: string
}

export interface ResumeVO {
  id: number
  title: string
  templateType: string
  status: string
  source: string
  version: number
  createTime: string
  updateTime: string
}

export interface ResumeDetailVO {
  id: number
  title: string
  templateType: string
  status: string
  source: string
  version: number
  createTime: string
  updateTime: string
  sections: SectionVO[]
}

export interface ResumeCreateRequest {
  title: string
  templateType?: string
}

export interface ResumeUpdateRequest {
  title?: string
  templateType?: string
  status?: string
}

export interface SectionCreateRequest {
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sortOrder?: number
}

export interface SectionUpdateRequest {
  sectionData?: Record<string, unknown>
  sortOrder?: number
}

export interface SectionSortItem {
  sectionId: number
  sortOrder: number
}

export interface SectionSortRequest {
  orders: SectionSortItem[]
}

export interface AiGenerateRequest {
  targetPosition: string
  techDirection?: string
  workYears?: number
}

export interface AiOptimizeRequest {
  sectionId: number
  sectionType: SectionType
  sectionData: Record<string, unknown>
}

export interface AiScoreDimensions {
  completeness: number
  completenessComment: string
  professionalism: number
  professionalismComment: string
  matching: number
  matchingComment: string
}

export interface AiScoreVO {
  score: number
  dimensions: AiScoreDimensions
  suggestions: string[]
}

export interface VersionVO {
  id: number
  version: number
  changeSummary: string
  createTime: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  relatedSectionType?: string
}
```

- [ ] **Step 2: 创建 API — api/resume.ts**

```typescript
import request from '@/utils/request'
import type {
  BaseResponse,
} from '@/types/user'
import type {
  ResumeVO,
  ResumeDetailVO,
  ResumeCreateRequest,
  ResumeUpdateRequest,
  SectionVO,
  SectionCreateRequest,
  SectionUpdateRequest,
  SectionSortRequest,
  AiGenerateRequest,
  AiOptimizeRequest,
  AiScoreVO,
  VersionVO,
} from '@/types/resume'

export function createResume(data: ResumeCreateRequest) {
  return request.post<BaseResponse<ResumeVO>>('/api/resume', data)
}

export function listResumes() {
  return request.get<BaseResponse<ResumeVO[]>>('/api/resume/list')
}

export function getResumeDetail(id: number) {
  return request.get<BaseResponse<ResumeDetailVO>>(`/api/resume/${id}`)
}

export function updateResume(id: number, data: ResumeUpdateRequest) {
  return request.put<BaseResponse<ResumeVO>>(`/api/resume/${id}`, data)
}

export function deleteResume(id: number) {
  return request.delete<BaseResponse<null>>(`/api/resume/${id}`)
}

export function addSection(resumeId: number, data: SectionCreateRequest) {
  return request.post<BaseResponse<SectionVO>>(`/api/resume/${resumeId}/section`, data)
}

export function updateSection(resumeId: number, sectionId: number, data: SectionUpdateRequest) {
  return request.put<BaseResponse<SectionVO>>(`/api/resume/${resumeId}/section/${sectionId}`, data)
}

export function deleteSection(resumeId: number, sectionId: number) {
  return request.delete<BaseResponse<null>>(`/api/resume/${resumeId}/section/${sectionId}`)
}

export function sortSections(resumeId: number, data: SectionSortRequest) {
  return request.put<BaseResponse<null>>(`/api/resume/${resumeId}/section/sort`, data)
}

export function aiGenerateResume(data: AiGenerateRequest) {
  return request.post<BaseResponse<ResumeDetailVO>>('/api/resume/ai/generate', data)
}

export function aiOptimizeSection(resumeId: number, data: AiOptimizeRequest) {
  return request.post<BaseResponse<Record<string, unknown>>>(`/api/resume/${resumeId}/ai/optimize-section`, data)
}

export function aiScoreResume(resumeId: number) {
  return request.post<BaseResponse<AiScoreVO>>(`/api/resume/${resumeId}/ai/score`)
}

export function getResumeVersions(id: number) {
  return request.get<BaseResponse<VersionVO[]>>(`/api/resume/${id}/versions`)
}

export function chatWithAi(resumeId: number, message: string) {
  return request.post<string>(`/api/resume/${resumeId}/chat`, { message }, {
    headers: { 'Accept': 'text/event-stream' },
    responseType: 'stream',
  })
}
```

- [ ] **Step 3: 创建 Store — stores/resume.ts**

```typescript
import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  listResumes as listResumesApi,
  getResumeDetail as getDetailApi,
  createResume as createApi,
  updateResume as updateApi,
  deleteResume as deleteApi,
  aiGenerateResume as aiGenerateApi,
} from '@/api/resume'
import type {
  ResumeVO,
  ResumeDetailVO,
  ResumeCreateRequest,
  AiGenerateRequest,
} from '@/types/resume'

export const useResumeStore = defineStore('resume', () => {
  const resumeList = ref<ResumeVO[]>([])
  const currentResume = ref<ResumeDetailVO | null>(null)
  const loading = ref(false)

  async function fetchResumeList() {
    loading.value = true
    try {
      const res = await listResumesApi()
      if (res.code === 0) {
        resumeList.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchResumeDetail(id: number) {
    loading.value = true
    try {
      const res = await getDetailApi(id)
      if (res.code === 0) {
        currentResume.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createResume(data: ResumeCreateRequest) {
    const res = await createApi(data)
    if (res.code === 0) {
      return res.data
    }
    return null
  }

  async function deleteResume(id: number) {
    const res = await deleteApi(id)
    return res.code === 0
  }

  async function aiGenerateResume(data: AiGenerateRequest) {
    loading.value = true
    try {
      const res = await aiGenerateApi(data)
      if (res.code === 0) {
        currentResume.value = res.data
        return res.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    resumeList,
    currentResume,
    loading,
    fetchResumeList,
    fetchResumeDetail,
    createResume,
    deleteResume,
    aiGenerateResume,
  }
})
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/types/resume.ts frontend/src/api/resume.ts frontend/src/stores/resume.ts
git commit -m "feat(resume): add frontend types, API layer and Pinia store"
```

---

### Task 10: 路由更新 + 导航栏更新

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: 在 router/index.ts 中添加简历路由**

在现有路由数组中，`/profile` 路由之后追加：

```typescript
{
  path: '/resume',
  name: 'ResumeList',
  component: () => import('@/views/resume/ResumeListPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/resume/new',
  name: 'ResumeNew',
  component: () => import('@/views/resume/ResumeEditPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/resume/:id/edit',
  name: 'ResumeEdit',
  component: () => import('@/views/resume/ResumeEditPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/resume/:id/preview',
  name: 'ResumePreview',
  component: () => import('@/views/resume/ResumePreviewPage.vue'),
  meta: { requiresAuth: true },
},
```

- [ ] **Step 2: 在 MainLayout.vue 导航栏中添加"我的简历"链接**

读取 `MainLayout.vue` 找到导航链接位置（紧接首页和资料链接之后），添加简历链接：

```html
<router-link to="/resume">我的简历</router-link>
```

（具体位置和样式需根据现有 MainLayout.vue 的结构调整）

- [ ] **Step 3: 提交**

```bash
git add frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue
git commit -m "feat(resume): add resume routes and navigation link"
```

---

## Phase 4 (P1): Frontend Resume Editor

> 以下 Task 11-15 涉及大量 Vue SFC 文件。每个文件的完整代码较长，实现时需遵循项目的 Neubrutalism 设计系统（variables.css）和 Element Plus 组件使用模式。

### Task 11: 简历模块编辑组件

**Files:**
- Create: `frontend/src/components/resume/sections/BasicInfoEditor.vue`
- Create: `frontend/src/components/resume/sections/EducationEditor.vue`
- Create: `frontend/src/components/resume/sections/WorkExperienceEditor.vue`
- Create: `frontend/src/components/resume/sections/ProjectEditor.vue`
- Create: `frontend/src/components/resume/sections/SkillsEditor.vue`
- Create: `frontend/src/components/resume/sections/SummaryEditor.vue`

- [ ] **Step 1: 创建 BasicInfoEditor.vue**

基本信息编辑器，接收 `modelValue`（BasicSectionData），`emit('update:modelValue')`。使用 Element Plus 的 `el-form` + `el-input`。字段：姓名、邮箱、电话、目标岗位、城市、GitHub、博客。

- [ ] **Step 2: 创建 EducationEditor.vue**

教育经历编辑器，支持多条记录。每条包含：学校、专业、学历（Select：本科/硕士/博士）、起止日期、GPA、亮点标签。提供"+ 添加教育经历"按钮。

- [ ] **Step 3: 创建 WorkExperienceEditor.vue**

工作经历编辑器，支持多条记录。每条包含：公司、职位、起止日期、职责描述（textarea）、亮点标签。提供"+ 添加工作经历"按钮。

- [ ] **Step 4: 创建 ProjectEditor.vue**

项目经历编辑器，支持多条记录。每条包含：项目名、角色、技术栈（标签输入）、起止日期、描述、亮点。提供"+ 添加项目经历"按钮。

- [ ] **Step 5: 创建 SkillsEditor.vue**

技能标签编辑器，分类展示。每类含：分类名（如"编程语言"）、技能项列表。支持添加/删除分类和技能项。

- [ ] **Step 6: 创建 SummaryEditor.vue**

自我评价编辑器，单个 `el-input type="textarea"` 绑定 content 字段。

- [ ] **Step 7: 提交**

```bash
git add frontend/src/components/resume/sections/
git commit -m "feat(resume): add section editor components"
```

---

### Task 12: ResumeListPage

**Files:**
- Create: `frontend/src/views/resume/ResumeListPage.vue`

- [ ] **Step 1: 创建 ResumeListPage.vue**

使用 MainLayout 布局。内容包含：
- 顶部标题"我的简历" + "新建简历"按钮（下拉：空白简历 / AI 生成）
- 简历卡片网格（NbCard），每个卡片显示标题、模板类型、更新时间
- 卡片操作：编辑（跳转 /resume/:id/edit）、预览（/resume/:id/preview）、删除（确认弹窗）
- onMounted 调用 `useResumeStore().fetchResumeList()`
- 空状态提示：当列表为空时显示引导文案

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/resume/ResumeListPage.vue
git commit -m "feat(resume): add ResumeListPage with card grid"
```

---

### Task 13: ResumeEditPage（核心编辑器页面）

**Files:**
- Create: `frontend/src/views/resume/ResumeEditPage.vue`
- Create: `frontend/src/components/resume/TemplateSelector.vue`

- [ ] **Step 1: 创建 TemplateSelector.vue**

模板选择下拉组件，3 个选项（简约技术风/现代双栏/经典正式），选择后 emit `update:modelValue`。

- [ ] **Step 2: 创建 ResumeEditPage.vue**

核心页面，左右分栏布局（CSS Grid 或 Flexbox）：

**顶栏**：可编辑标题（el-input）| TemplateSelector | 保存按钮 | 预览按钮（跳转 /resume/:id/preview）

**左侧编辑区**（滚动）：
- 使用 el-collapse 折叠面板组织各模块
- 每个面板标题旁有"AI 优化"小按钮
- 底部"一键 AI 生成/优化全部"按钮
- 面板内容对应各 Section Editor 组件

**右侧预览区**（固定）：
- 上方：A4 比例缩放的模板预览（使用 CSS transform: scale() 缩放到容器宽度）
- 下方：可折叠的 AI 对话面板

**数据流**：
- onMounted 时，如果有 route.params.id，调用 `fetchResumeDetail(id)`
- 编辑组件通过 v-model 双向绑定 sectionData
- 保存时，遍历所有模块调用 updateSection API

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/resume/ResumeEditPage.vue frontend/src/components/resume/TemplateSelector.vue
git commit -m "feat(resume): add ResumeEditPage with split-panel editor layout"
```

---

### Task 14: 简历模板组件

**Files:**
- Create: `frontend/src/templates/MinimalTech.vue`
- Create: `frontend/src/templates/ModernTwoCol.vue`
- Create: `frontend/src/templates/ClassicFormal.vue`

- [ ] **Step 1: 创建 MinimalTech.vue（简约技术风）**

单列布局，黑白为主。接收 `ResumeData` props，渲染各模块。使用 scoped CSS 实现 A4 纸张样式（210mm × 297mm）。

模板结构：
```
┌──────────────────────┐
│ 姓名          联系方式  │
│ 目标岗位              │
├──────────────────────┤
│ 专业技能              │
│ Java ●●●●○ Python...  │
├──────────────────────┤
│ 工作经历              │
│ 字节跳动 | Java 后端   │
│ · 负责核心系统...      │
├──────────────────────┤
│ 项目经历              │
│ 订单系统 | 核心开发者   │
│ · 采用 CQRS 架构...   │
├──────────────────────┤
│ 教育经历              │
│ 北京大学 | 计算机      │
├──────────────────────┤
│ 自我评价              │
│ 5年Java开发经验...    │
└──────────────────────┘
```

- [ ] **Step 2: 创建 ModernTwoCol.vue（现代双栏）**

双栏布局，左侧窄栏（30%）展示基本信息、技能、联系方式，右侧宽栏（70%）展示经历和项目。

- [ ] **Step 3: 创建 ClassicFormal.vue（经典正式）**

传统简历格式，表格式教育经历，段落式工作描述，适合国企/传统企业。

- [ ] **Step 4: 提交**

```bash
git add frontend/src/templates/
git commit -m "feat(resume): add three resume template components"
```

---

## Phase 5 (P3+P4): Frontend AI + Polish

### Task 15: AiChatPanel + AiScorePanel + VersionHistory

**Files:**
- Create: `frontend/src/components/resume/AiChatPanel.vue`
- Create: `frontend/src/components/resume/AiScorePanel.vue`
- Create: `frontend/src/components/resume/VersionHistory.vue`

- [ ] **Step 1: 创建 AiChatPanel.vue**

AI 对话面板组件：
- Props: `resumeId`
- 消息列表（气泡样式，区分 user/assistant）
- 底部输入框 + 发送按钮
- 使用 fetch + ReadableStream 处理 SSE 流式响应
- 解析 `[EXTRACTED_DATA]` 标记，提取 JSON 后 emit `extracted` 事件
- 父组件接收 `extracted` 事件后高亮提示用户"是否采纳"

SSE 调用方式（不使用 axios，使用原生 fetch）：
```typescript
const response = await fetch(`/api/resume/${resumeId}/chat`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  },
  body: JSON.stringify({ message }),
})
const reader = response.body?.getReader()
// 逐块读取并解析 SSE 事件
```

- [ ] **Step 2: 创建 AiScorePanel.vue**

AI 评分展示组件：
- Props: `resumeId`
- 点击"AI 评分"按钮调用 `aiScoreResume(resumeId)`
- 展示总分（大字体）+ 三个维度的进度条（el-progress）+ 评语
- 底部建议列表

- [ ] **Step 3: 创建 VersionHistory.vue**

版本历史抽屉组件：
- Props: `resumeId`
- 使用 el-drawer 从右侧弹出
- 调用 `getResumeVersions(resumeId)` 获取版本列表
- 每个版本项展示：版本号、变更摘要、时间
- 点击版本项可展开查看快照内容（只读）

- [ ] **Step 4: 提交**

```bash
git add frontend/src/components/resume/AiChatPanel.vue frontend/src/components/resume/AiScorePanel.vue frontend/src/components/resume/VersionHistory.vue
git commit -m "feat(resume): add AI chat panel, score panel and version history"
```

---

### Task 16: ResumePreviewPage + PDF 导出

**Files:**
- Create: `frontend/src/views/resume/ResumePreviewPage.vue`
- Modify: `frontend/package.json`（新增依赖）

- [ ] **Step 1: 安装新依赖**

Run: `cd frontend && npm install html2canvas jspdf vuedraggable@next`

- [ ] **Step 2: 创建 ResumePreviewPage.vue**

全屏预览页面：
- 顶部工具栏：返回编辑按钮、切换模板（3 个选项）、导出 PDF 按钮
- 主体区域：A4 纸张居中展示（白底 + 阴影），使用选中的模板组件渲染
- PDF 导出逻辑：

```typescript
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

async function exportPdf() {
  const el = document.querySelector('.resume-preview-content') as HTMLElement
  const canvas = await html2canvas(el, { scale: 2, useCORS: true })
  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pdfWidth = pdf.internal.pageSize.getWidth()
  const pdfHeight = pdf.internal.pageSize.getHeight()
  const imgWidth = canvas.width
  const imgHeight = canvas.height
  const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight)
  const imgX = (pdfWidth - imgWidth * ratio) / 2
  pdf.addImage(imgData, 'PNG', imgX, 0, imgWidth * ratio, imgHeight * ratio)
  pdf.save(`${resumeTitle}.pdf`)
}
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/resume/ResumePreviewPage.vue frontend/package.json frontend/package-lock.json
git commit -m "feat(resume): add ResumePreviewPage with PDF export"
```

---

### Task 17: HomePage 更新 + 最终验证

**Files:**
- Modify: `frontend/src/views/home/HomePage.vue`

- [ ] **Step 1: 在 HomePage 中添加简历入口**

在现有快捷操作卡片区域，将"开始面试"旁新增"我的简历"卡片，链接到 `/resume`。

- [ ] **Step 2: 运行前端 lint**

Run: `cd frontend && npm run lint`
Expected: 无错误

- [ ] **Step 3: 运行前端类型检查**

Run: `cd frontend && npm run type-check`
Expected: 无错误

- [ ] **Step 4: 运行全部后端测试**

Run: `cd backend && .\mvnw.cmd test`
Expected: 全部 PASS

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/home/HomePage.vue
git commit -m "feat(resume): add resume entry on HomePage and verify build"
```

---

## Self-Review Checklist

- [x] **Spec coverage**: 每个 spec 需求都有对应 Task 覆盖
  - 从零生成 → Task 7, 8
  - 对话填充 → Task 7 (chatStream), 15 (AiChatPanel)
  - 模块优化 → Task 7, 8, 11
  - 简历评分 → Task 7, 8, 15 (AiScorePanel)
  - 多模板 → Task 14
  - PDF 导出 → Task 16
  - 版本管理 → 数据模型 Task 1 (resume_version 表), Task 15 (VersionHistory)
  - CRUD → Task 5, 6
- [x] **Placeholder scan**: 无 TBD/TODO，所有步骤含具体代码或明确操作
- [x] **Type consistency**: Entity 字段、DTO 字段、VO 字段、前端 types 均保持一致
