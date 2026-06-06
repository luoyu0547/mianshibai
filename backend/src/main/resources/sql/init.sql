CREATE TABLE IF NOT EXISTS user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 id',
  user_account VARCHAR(32) NOT NULL COMMENT '登录账号',
  user_password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  user_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户昵称',
  user_avatar VARCHAR(512) NOT NULL DEFAULT '' COMMENT '用户头像 URL',
  user_role VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  user_status TINYINT NOT NULL DEFAULT 0 COMMENT '用户状态：0-正常，1-禁用',
  email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱，后续扩展使用',
  phone VARCHAR(32) NOT NULL DEFAULT '' COMMENT '手机号，后续扩展使用',
  target_position VARCHAR(128) NOT NULL DEFAULT '' COMMENT '目标岗位，如 Java 后端工程师',
  tech_direction VARCHAR(128) NOT NULL DEFAULT '' COMMENT '技术方向，如 Java/Spring Boot/AI 应用',
  work_years TINYINT NOT NULL DEFAULT 0 COMMENT '工作年限，0 表示应届/无经验',
  city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '期望/所在城市',
  job_status VARCHAR(32) NOT NULL DEFAULT '' COMMENT '求职状态，如 looking/open/not_looking',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_account (user_account),
  KEY idx_target_position (target_position),
  KEY idx_tech_direction (tech_direction),
  KEY idx_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

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
