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
  job_id BIGINT DEFAULT NULL COMMENT '关联职位 id',
  total_questions INT NOT NULL DEFAULT 5 COMMENT '主问题数量',
  current_question_no INT NOT NULL DEFAULT 0 COMMENT '当前主问题序号',
  status VARCHAR(32) NOT NULL DEFAULT 'created' COMMENT 'created/in_progress/generating_report/completed/cancelled',
  started_at DATETIME DEFAULT NULL COMMENT '开始时间',
  ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
  difficulty VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
  duration_minutes INT DEFAULT NULL COMMENT '计划面试时长（分钟）',
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

CREATE TABLE IF NOT EXISTS company (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '公司 id',
  name VARCHAR(128) NOT NULL COMMENT '公司名称',
  normalized_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '归一化公司名，用于名单匹配',
  website VARCHAR(512) NOT NULL DEFAULT '' COMMENT '公司官网',
  industry VARCHAR(128) NOT NULL DEFAULT '' COMMENT '行业方向',
  city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '主要城市',
  scale VARCHAR(64) NOT NULL DEFAULT '' COMMENT '公司规模',
  description TEXT DEFAULT NULL COMMENT '公司简介',
  main_business TEXT DEFAULT NULL COMMENT '主营业务',
  tech_direction VARCHAR(256) NOT NULL DEFAULT '' COMMENT '技术方向',
  is_specialized_new TINYINT NOT NULL DEFAULT 0 COMMENT '是否专精特新：0-否/未确认，1-是',
  is_little_giant TINYINT NOT NULL DEFAULT 0 COMMENT '是否小巨人：0-否/未确认，1-是',
  certification_confidence VARCHAR(32) NOT NULL DEFAULT 'unknown' COMMENT '资质可信度：confirmed/suspected/unknown',
  source_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '公司信息来源 URL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_name (name),
  KEY idx_normalized_name (normalized_name),
  KEY idx_industry (industry),
  KEY idx_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司画像表';

CREATE TABLE IF NOT EXISTS company_certification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '资质证据 id',
  company_id BIGINT NOT NULL COMMENT '公司 id',
  certification_type VARCHAR(64) NOT NULL COMMENT '资质类型：specialized_new/little_giant/high_tech/other',
  status VARCHAR(32) NOT NULL DEFAULT 'suspected' COMMENT '状态：confirmed/suspected/rejected',
  evidence_source VARCHAR(64) NOT NULL COMMENT '证据来源：official_list/website/news/ai_inferred',
  evidence_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '证据 URL',
  evidence_text TEXT DEFAULT NULL COMMENT '证据文本',
  confidence_score INT NOT NULL DEFAULT 0 COMMENT '可信度 0-100',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_company_id (company_id),
  KEY idx_certification_type (certification_type),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司资质证据表';

CREATE TABLE IF NOT EXISTS job (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '职位 id',
  company_id BIGINT DEFAULT NULL COMMENT '公司 id',
  title VARCHAR(128) NOT NULL COMMENT '职位名称',
  company_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '公司名称快照',
  source_platform VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源平台，如 boss/lagou/company_website',
  source_url VARCHAR(1024) NOT NULL COMMENT '职位来源 URL',
  city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '工作城市',
  salary_range VARCHAR(64) NOT NULL DEFAULT '' COMMENT '薪资范围',
  experience_requirement VARCHAR(64) NOT NULL DEFAULT '' COMMENT '经验要求',
  education_requirement VARCHAR(64) NOT NULL DEFAULT '' COMMENT '学历要求',
  job_description TEXT DEFAULT NULL COMMENT '岗位职责',
  job_requirement TEXT DEFAULT NULL COMMENT '岗位要求',
  tech_stack JSON DEFAULT NULL COMMENT '技术栈列表',
  raw_content MEDIUMTEXT DEFAULT NULL COMMENT '抓取原始内容',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态：active/expired/unknown',
  application_status VARCHAR(32) NOT NULL DEFAULT 'favorite' COMMENT '投递状态：favorite/preparing/applied/interviewing/rejected/offer',
  keywords_json JSON DEFAULT NULL COMMENT 'JD关键词分析',
  predicted_questions_json JSON DEFAULT NULL COMMENT '预测面试题',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_company_id (company_id),
  KEY idx_title (title),
  KEY idx_city (city),
  KEY idx_source_platform (source_platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';

CREATE TABLE IF NOT EXISTS job_analysis (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位分析 id',
  job_id BIGINT NOT NULL COMMENT '职位 id',
  requirement_summary TEXT NOT NULL COMMENT '岗位要求总结',
  core_skills JSON NOT NULL COMMENT '核心技能列表',
  hidden_requirements JSON NOT NULL COMMENT '隐含能力要求',
  interview_focus JSON NOT NULL COMMENT '面试准备重点',
  resume_suggestions JSON NOT NULL COMMENT '简历优化建议',
  risk_points JSON NOT NULL COMMENT '风险点或不匹配点',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位 AI 分析表';

CREATE TABLE IF NOT EXISTS job_match (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '匹配记录 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  resume_id BIGINT DEFAULT NULL COMMENT '简历 id',
  job_id BIGINT NOT NULL COMMENT '职位 id',
  match_score INT NOT NULL DEFAULT 0 COMMENT '岗位匹配度 0-100',
  growth_score INT NOT NULL DEFAULT 0 COMMENT '企业成长性 0-100',
  tech_growth_score INT NOT NULL DEFAULT 0 COMMENT '技术成长价值 0-100',
  salary_city_score INT NOT NULL DEFAULT 0 COMMENT '薪资城市匹配 0-100',
  experience_fit_score INT NOT NULL DEFAULT 0 COMMENT '经验门槛适配 0-100',
  total_score INT NOT NULL DEFAULT 0 COMMENT '综合推荐分 0-100',
  recommendation VARCHAR(32) NOT NULL DEFAULT 'cautious' COMMENT '推荐结论：recommended/cautious/stretch/not_recommended',
  reason TEXT NOT NULL COMMENT '推荐原因',
  gaps JSON NOT NULL COMMENT '能力缺口',
  keyword_coverage INT DEFAULT NULL COMMENT '关键词覆盖率 0-100',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_resume_id (resume_id),
  KEY idx_job_id (job_id),
  KEY idx_total_score (total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户职位匹配表';

CREATE TABLE IF NOT EXISTS job_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  job_id BIGINT NOT NULL COMMENT '职位 id',
  note VARCHAR(512) NOT NULL DEFAULT '' COMMENT '用户备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_job (user_id, job_id),
  KEY idx_user_id (user_id),
  KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位收藏表';

CREATE TABLE IF NOT EXISTS job_application (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '投递记录 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  job_id BIGINT DEFAULT NULL COMMENT '关联职位 id',
  resume_id BIGINT DEFAULT NULL COMMENT '关联简历 id',
  company_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '公司名',
  job_title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '岗位名',
  source VARCHAR(64) NOT NULL DEFAULT '' COMMENT '投递渠道',
  status VARCHAR(32) NOT NULL DEFAULT 'pending_submit' COMMENT 'pending_submit/submitted/hr_contact/written_test/first_interview/second_interview/final_interview/offer/rejected/withdrawn',
  applied_at DATETIME DEFAULT NULL COMMENT '投递时间',
  next_event_at DATETIME DEFAULT NULL COMMENT '下一事件时间',
  salary_range VARCHAR(64) NOT NULL DEFAULT '' COMMENT '薪资范围',
  location VARCHAR(64) NOT NULL DEFAULT '' COMMENT '工作城市',
  contact_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '联系人',
  contact_info VARCHAR(128) NOT NULL DEFAULT '' COMMENT '联系方式',
  notes TEXT DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_application_user_status (user_id, status),
  KEY idx_application_user_next_event (user_id, next_event_at),
  KEY idx_application_job_id (job_id),
  KEY idx_application_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求职投递记录表';

CREATE TABLE IF NOT EXISTS application_todo (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '投递待办 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  application_id BIGINT DEFAULT NULL COMMENT '关联投递记录 id，空表示全局待办',
  title VARCHAR(128) NOT NULL COMMENT '待办标题',
  description TEXT DEFAULT NULL COMMENT '待办说明',
  priority VARCHAR(16) NOT NULL DEFAULT 'medium' COMMENT 'low/medium/high',
  due_at DATETIME DEFAULT NULL COMMENT '截止时间',
  completed TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：0-未完成，1-已完成',
  completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_todo_user_completed_due (user_id, completed, due_at),
  KEY idx_todo_application_id (application_id),
  KEY idx_todo_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求职投递待办表';

CREATE TABLE IF NOT EXISTS training_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '训练计划 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  title VARCHAR(255) NOT NULL COMMENT '计划标题',
  source_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型：manual/interview_report/job_analysis',
  source_id BIGINT DEFAULT NULL COMMENT '来源 id',
  target_days INT NOT NULL DEFAULT 7 COMMENT '目标天数',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/completed/cancelled',
  summary TEXT DEFAULT NULL COMMENT '计划摘要',
  focus_topics JSON DEFAULT NULL COMMENT '重点主题列表',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_tp_user_status (user_id, status),
  KEY idx_tp_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练计划表';

CREATE TABLE IF NOT EXISTS training_question (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '训练题 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  plan_id BIGINT NOT NULL COMMENT '关联训练计划 id',
  day_index INT NOT NULL DEFAULT 1 COMMENT '第几天',
  title VARCHAR(255) NOT NULL COMMENT '题目标题',
  content TEXT NOT NULL COMMENT '题目内容',
  topic VARCHAR(64) DEFAULT NULL COMMENT '主题分类',
  skill_tags JSON DEFAULT NULL COMMENT '技能标签列表',
  difficulty VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT 'easy/medium/hard',
  source_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型',
  reference_answer TEXT DEFAULT NULL COMMENT '参考答案',
  follow_up_questions JSON DEFAULT NULL COMMENT '追问列表',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/answered/reviewed',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_tq_user_plan (user_id, plan_id),
  KEY idx_tq_user_status (user_id, status),
  KEY idx_tq_plan_day (plan_id, day_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练题表';

CREATE TABLE IF NOT EXISTS training_answer (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '训练答案 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  question_id BIGINT NOT NULL COMMENT '关联训练题 id',
  answer_text TEXT NOT NULL COMMENT '答案内容',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_ta_user_question (user_id, question_id),
  KEY idx_ta_question_create_time (question_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练答案表';

CREATE TABLE IF NOT EXISTS training_answer_review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '答案评审 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  question_id BIGINT NOT NULL COMMENT '关联训练题 id',
  answer_id BIGINT NOT NULL COMMENT '关联训练答案 id',
  total_score INT NOT NULL DEFAULT 0 COMMENT '总分',
  accuracy_score INT NOT NULL DEFAULT 0 COMMENT '技术准确性',
  clarity_score INT NOT NULL DEFAULT 0 COMMENT '表达清晰度',
  depth_score INT NOT NULL DEFAULT 0 COMMENT '项目深度',
  project_score INT NOT NULL DEFAULT 0 COMMENT '项目评分',
  strengths_json JSON DEFAULT NULL COMMENT '优点列表',
  mistakes_json JSON DEFAULT NULL COMMENT '错误列表',
  missing_points_json JSON DEFAULT NULL COMMENT '遗漏点列表',
  suggestions_json JSON DEFAULT NULL COMMENT '建议列表',
  recommended_answer TEXT DEFAULT NULL COMMENT '推荐答案',
  follow_up_questions_json JSON DEFAULT NULL COMMENT '追问列表',
  mastery_level VARCHAR(32) NOT NULL DEFAULT 'basic' COMMENT 'basic/intermediate/advanced/expert',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_tar_user_question (user_id, question_id),
  KEY idx_tar_answer (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练答案评审表';

CREATE TABLE IF NOT EXISTS algorithm_recommendation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '算法推荐 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  plan_id BIGINT NOT NULL COMMENT '关联训练计划 id',
  category VARCHAR(64) NOT NULL COMMENT '算法分类',
  platform VARCHAR(64) NOT NULL DEFAULT 'LeetCode' COMMENT '刷题平台',
  problem_ref VARCHAR(255) NOT NULL COMMENT '题目引用',
  reason TEXT DEFAULT NULL COMMENT '推荐理由',
  completed TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：0-未完成，1-已完成',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_ar_user_plan (user_id, plan_id),
  KEY idx_ar_user_completed (user_id, completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法推荐表';

CREATE TABLE IF NOT EXISTS training_mastery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    practice_count INT DEFAULT 0,
    question_count INT DEFAULT 0,
    average_score DECIMAL(5,2) DEFAULT 0,
    weak_count INT DEFAULT 0,
    mastered_count INT DEFAULT 0,
    mastery_level VARCHAR(32) DEFAULT 'basic',
    last_practiced_at DATETIME NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_target (user_id, target_type, target_name),
    INDEX idx_user_type_level (user_id, target_type, mastery_level),
    INDEX idx_user_last_practiced (user_id, last_practiced_at)
);
