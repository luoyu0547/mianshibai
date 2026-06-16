package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.CoachDiagnosis;
import com.mianshiba.ai.model.entity.CoachPlan;
import com.mianshiba.ai.model.entity.CoachTask;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.TrainingAnswer;
import com.mianshiba.ai.model.entity.TrainingMastery;
import com.mianshiba.ai.model.entity.TrainingPlan;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.coach.CoachDiagnosisVO;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.model.vo.coach.CoachPlanVO;
import com.mianshiba.ai.model.vo.coach.CoachTaskVO;
import com.mianshiba.ai.service.CoachService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Set<String> TASK_TYPES = Set.of("resume", "interview", "training", "application", "job", "habit");
    private static final Set<String> PRIORITIES = Set.of("high", "medium", "low");
    private static final Set<String> REFERENCE_TYPES = Set.of("resume", "interview_session", "interview_report", "training_question", "training_plan", "job_application", "job");

    static final String SYSTEM_PROMPT = "你是一位资深程序员求职教练。请基于用户数据生成求职诊断和 7 天计划。" +
            "返回 JSON 格式，不要任何解释。必须严格分析以下数据并给出有数据支撑的诊断：\n" +
            "1. 简历内容：分析技能列表是否匹配目标岗位、项目经验深度、工作经历连贯性。指出简历中的具体问题（如技能栈过时、项目描述缺乏量化成果）。\n" +
            "2. 面试表现：根据各维度评分（accuracy/clarity/depth/matching）找出最弱的 1-2 个维度，结合面试报告中的建议给出改进方向。\n" +
            "3. 投递进展：根据投递状态分布判断求职策略是否合理（如只投不面、面试转化率低）。\n" +
            "4. 训练掌握度：根据薄弱知识点安排针对性复习。\n" +
            "overallScore（0-100）必须基于数据计算：有简历且内容匹配 +20，有面试经验且平均分>70 +20，有投递且进入面试 +20，有训练记录 +10，" +
            "其余根据数据丰富度浮动。不要固定给 60 分。\n" +
            "tasks 数组每项必须包含 dayIndex(1-7)、title、description、taskType、priority。taskType 只能是 resume/interview/training/application/job/habit。每天至少 1 个任务，总共 7-14 个。" +
            "任务必须针对用户真实数据中的具体问题（如简历缺项目、面试深度分低），不要给通用任务。\n" +
            "必须返回以下 JSON 结构，字段名不能改变：" +
            "{\"diagnosis\":{\"title\":\"\",\"overallScore\":0,\"summary\":\"\",\"strengths\":[\"\"],\"weaknesses\":[\"\"],\"suggestions\":[\"\"]}," +
            "\"plan\":{\"title\":\"\",\"summary\":\"\",\"tasks\":[{\"dayIndex\":1,\"title\":\"\",\"description\":\"\",\"taskType\":\"resume\",\"priority\":\"high\"}]}}。";

    private final JwtUtils jwtUtils;
    private final ChatClient chatClient;
    private final CoachDiagnosisMapper diagnosisMapper;
    private final CoachPlanMapper planMapper;
    private final CoachTaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingAnswerMapper trainingAnswerMapper;
    private final TrainingMasteryMapper trainingMasteryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachGenerateResultVO generate(String authorizationHeader, CoachGenerateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        User user = requireUser(userId);
        Map<String, Object> snapshot = buildSnapshot(user, request);
        GeneratedCoach generated = tryGenerateWithAi(snapshot);
        if (generated == null) {
            generated = fallback(snapshot);
        }

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<CoachPlan> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("user_id", userId).eq("status", "active").set("status", "archived");
        planMapper.update(null, updateWrapper);

        CoachDiagnosis diagnosis = new CoachDiagnosis();
        diagnosis.setUserId(userId);
        diagnosis.setTitle(generated.diagnosisTitle());
        diagnosis.setOverallScore(clamp(generated.overallScore(), 0, 100));
        diagnosis.setSummary(generated.diagnosisSummary());
        diagnosis.setStrengthsJson(generated.strengths());
        diagnosis.setWeaknessesJson(generated.weaknesses());
        diagnosis.setSuggestionsJson(generated.suggestions());
        diagnosis.setDataSnapshotJson(snapshot);
        diagnosis.setDataCompleteness(calculateCompleteness(snapshot));
        diagnosis.setSource(generated.source());
        diagnosisMapper.insert(diagnosis);

        CoachPlan plan = new CoachPlan();
        plan.setUserId(userId);
        plan.setDiagnosisId(diagnosis.getId());
        plan.setTitle(generated.planTitle());
        plan.setSummary(generated.planSummary());
        plan.setTargetPosition(String.valueOf(snapshot.getOrDefault("targetPosition", "")));
        plan.setTargetDays(7);
        plan.setStatus("active");
        plan.setSource(generated.source());
        planMapper.insert(plan);

        List<CoachTask> tasks = normalizeTasks(userId, plan.getId(), generated.tasks(), snapshot);
        for (CoachTask task : tasks) {
            taskMapper.insert(task);
        }

        CoachGenerateResultVO result = new CoachGenerateResultVO();
        result.setDiagnosis(toDiagnosisVO(diagnosis));
        result.setPlan(toPlanVO(plan, tasks));
        return result;
    }

    @Override
    public CoachOverviewVO getOverview(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        CoachOverviewVO vo = new CoachOverviewVO();
        List<CoachDiagnosis> diagnoses = diagnosisMapper.selectList(Wrappers.lambdaQuery(CoachDiagnosis.class)
                .eq(CoachDiagnosis::getUserId, userId));
        CoachDiagnosis latest = diagnoses.stream().max(Comparator.comparing(CoachDiagnosis::getCreateTime)).orElse(null);
        CoachPlan active = planMapper.selectOne(Wrappers.lambdaQuery(CoachPlan.class)
                .eq(CoachPlan::getUserId, userId)
                .eq(CoachPlan::getStatus, "active")
                .last("LIMIT 1"));
        vo.setLatestDiagnosis(latest == null ? null : toDiagnosisVO(latest));
        vo.setActivePlan(active == null ? null : toPlanVO(active));
        vo.setTodayTasks(active == null ? List.of() : taskMapper.selectList(Wrappers.lambdaQuery(CoachTask.class)
                .eq(CoachTask::getPlanId, active.getId())
                .eq(CoachTask::getDayIndex, 1)
                .orderByAsc(CoachTask::getId)).stream().map(this::toTaskVO).toList());
        vo.setDiagnosisCount(diagnosisMapper.selectCount(Wrappers.lambdaQuery(CoachDiagnosis.class).eq(CoachDiagnosis::getUserId, userId)));
        vo.setPlanCount(planMapper.selectCount(Wrappers.lambdaQuery(CoachPlan.class).eq(CoachPlan::getUserId, userId)));
        return vo;
    }

    @Override
    public List<CoachDiagnosisVO> listDiagnoses(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        return diagnosisMapper.selectList(Wrappers.lambdaQuery(CoachDiagnosis.class)
                .eq(CoachDiagnosis::getUserId, userId)
                .select(CoachDiagnosis.class, column -> !"data_snapshot_json".equals(column.getColumn())))
                .stream()
                .sorted(Comparator.comparing(CoachDiagnosis::getCreateTime).reversed())
                .map(d -> toDiagnosisVO(d, false))
                .toList();
    }

    @Override
    public CoachDiagnosisVO getDiagnosis(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachDiagnosis diagnosis = diagnosisMapper.selectById(id);
        if (diagnosis == null || !userId.equals(diagnosis.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toDiagnosisVO(diagnosis);
    }

    @Override
    public List<CoachPlanVO> listPlans(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        return planMapper.selectList(Wrappers.lambdaQuery(CoachPlan.class)
                .eq(CoachPlan::getUserId, userId)
                .orderByDesc(CoachPlan::getCreateTime)).stream().map(this::toPlanVO).toList();
    }

    @Override
    public CoachPlanVO getPlan(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachPlan plan = planMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toPlanVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachTaskVO completeTask(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachTask task = getOwnedTask(userId, id);
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        CoachPlan plan = planMapper.selectById(task.getPlanId());
        Long pendingCount = taskMapper.selectCount(Wrappers.lambdaQuery(CoachTask.class)
                .eq(CoachTask::getPlanId, task.getPlanId())
                .eq(CoachTask::getStatus, "pending"));
        if (plan != null && pendingCount == 0) {
            plan.setStatus("completed");
            planMapper.updateById(plan);
        }
        return toTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachTaskVO reopenTask(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachTask task = getOwnedTask(userId, id);
        task.setStatus("pending");
        task.setCompletedAt(null);
        taskMapper.updateById(task);
        CoachPlan plan = planMapper.selectById(task.getPlanId());
        if (plan != null && "completed".equals(plan.getStatus())) {
            plan.setStatus("active");
            planMapper.updateById(plan);
        }
        return toTaskVO(task);
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        if (token == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return jwtUtils.parseToken(token).userId();
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    Map<String, Object> buildSnapshot(User user, CoachGenerateRequest request) {
        Map<String, Object> snapshot = new HashMap<>();
        Long userId = user.getId();

        String targetPosition = request != null && request.getTargetPosition() != null && !request.getTargetPosition().isBlank()
                ? request.getTargetPosition().trim() : user.getTargetPosition();
        snapshot.put("targetPosition", targetPosition == null ? "" : targetPosition);
        snapshot.put("techDirection", user.getTechDirection() == null ? "" : user.getTechDirection());
        snapshot.put("workYears", user.getWorkYears() == null ? 0 : user.getWorkYears());
        snapshot.put("city", user.getCity() == null ? "" : user.getCity());
        snapshot.put("jobStatus", user.getJobStatus() == null ? "" : user.getJobStatus());
        snapshot.put("focus", request == null || request.getFocus() == null ? "" : request.getFocus().trim());

        List<Resume> resumes = resumeMapper.selectList(
                Wrappers.lambdaQuery(Resume.class).eq(Resume::getUserId, userId).orderByDesc(Resume::getUpdateTime));
        snapshot.put("resumeCount", resumes.size());
        if (!resumes.isEmpty()) {
            Resume latest = resumes.get(0);
            List<ResumeSection> sections = resumeSectionMapper.selectList(
                    Wrappers.lambdaQuery(ResumeSection.class).eq(ResumeSection::getResumeId, latest.getId()).orderByAsc(ResumeSection::getSortOrder));
            List<Map<String, Object>> resumeSections = sections.stream()
                    .map(this::extractResumeSectionSummary)
                    .filter(s -> !s.isEmpty())
                    .toList();
            snapshot.put("resume", Map.of("title", latest.getTitle(), "sections", resumeSections));
        }

        long interviewTotal = interviewSessionMapper.selectCount(Wrappers.lambdaQuery(InterviewSession.class).eq(InterviewSession::getUserId, userId));
        long interviewCompleted = interviewSessionMapper.selectCount(Wrappers.lambdaQuery(InterviewSession.class)
                .eq(InterviewSession::getUserId, userId).eq(InterviewSession::getStatus, "completed"));
        snapshot.put("interviewTotal", interviewTotal);
        snapshot.put("interviewCompleted", interviewCompleted);

        if (interviewCompleted > 0) {
            List<InterviewSession> completedSessions = interviewSessionMapper.selectList(
                    Wrappers.lambdaQuery(InterviewSession.class).eq(InterviewSession::getUserId, userId).eq(InterviewSession::getStatus, "completed")
                            .orderByDesc(InterviewSession::getEndedAt).last("LIMIT 5"));
            List<Long> sessionIds = completedSessions.stream().map(InterviewSession::getId).toList();
            List<InterviewReport> reports = interviewReportMapper.selectList(
                    Wrappers.lambdaQuery(InterviewReport.class).in(InterviewReport::getSessionId, sessionIds));
            double avgScore = reports.stream().filter(r -> r.getTotalScore() != null)
                    .mapToInt(InterviewReport::getTotalScore).average().orElse(0);
            snapshot.put("interviewAverageScore", Math.round(avgScore));
            List<Map<String, Object>> reportList = reports.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("totalScore", r.getTotalScore());
                m.put("accuracyScore", r.getAccuracyScore());
                m.put("clarityScore", r.getClarityScore());
                m.put("depthScore", r.getDepthScore());
                m.put("matchingScore", r.getMatchingScore());
                String summary = r.getSummary();
                m.put("summary", summary != null && summary.length() > 200 ? summary.substring(0, 200) : summary);
                List<String> suggestions = r.getSuggestions();
                m.put("suggestions", suggestions != null ? suggestions.stream().limit(3).toList() : List.of());
                return m;
            }).toList();
            snapshot.put("interviewReports", reportList);
        } else {
            snapshot.put("interviewAverageScore", 0);
        }

        long applicationTotal = jobApplicationMapper.selectCount(Wrappers.lambdaQuery(JobApplication.class).eq(JobApplication::getUserId, userId));
        snapshot.put("applicationTotal", applicationTotal);
        if (applicationTotal > 0) {
            List<JobApplication> applications = jobApplicationMapper.selectList(
                    Wrappers.lambdaQuery(JobApplication.class).eq(JobApplication::getUserId, userId));
            snapshot.put("applicationByStatus", applications.stream()
                    .collect(Collectors.groupingBy(
                            app -> app.getStatus() != null ? app.getStatus() : "unknown",
                            Collectors.counting())));
            snapshot.put("applications", applications.stream().limit(10).map(a -> {
                Map<String, String> m = new HashMap<>();
                m.put("company", a.getCompanyName());
                m.put("position", a.getJobTitle());
                m.put("status", a.getStatus());
                return m;
            }).toList());
        }

        long trainingPlanCount = trainingPlanMapper.selectCount(Wrappers.lambdaQuery(TrainingPlan.class).eq(TrainingPlan::getUserId, userId));
        long trainingAnswerCount = trainingAnswerMapper.selectCount(Wrappers.lambdaQuery(TrainingAnswer.class).eq(TrainingAnswer::getUserId, userId));
        snapshot.put("trainingPlanCount", trainingPlanCount);
        snapshot.put("trainingAnswerCount", trainingAnswerCount);

        List<TrainingMastery> masteries = trainingMasteryMapper.selectList(
                Wrappers.lambdaQuery(TrainingMastery.class).eq(TrainingMastery::getUserId, userId)
                        .orderByDesc(TrainingMastery::getWeakCount).last("LIMIT 10"));
        double masteryAvgScore = masteries.stream().filter(m -> m.getAverageScore() != null)
                .mapToDouble(m -> m.getAverageScore().doubleValue()).average().orElse(0);
        snapshot.put("trainingMasteryAverageScore", Math.round(masteryAvgScore));
        snapshot.put("trainingWeakTopics", masteries.stream()
                .filter(m -> m.getWeakCount() != null && m.getWeakCount() > 0)
                .map(TrainingMastery::getTargetName)
                .toList());

        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResumeSectionSummary(ResumeSection section) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("type", section.getSectionType());
        Map<String, Object> data = section.getSectionData();
        if (data == null || data.isEmpty()) return summary;

        switch (section.getSectionType()) {
            case "basic" -> {
                copyField(summary, data, "name");
                copyField(summary, data, "title");
            }
            case "skills" -> {
                Object skills = data.get("skills");
                if (skills instanceof List) {
                    summary.put("skills", skills);
                } else if (skills instanceof String s) {
                    summary.put("skills", List.of(s.split("[,，、\\s]+")));
                }
            }
            case "work_experience" -> {
                copyField(summary, data, "company");
                copyField(summary, data, "position");
                copyField(summary, data, "startDate");
                copyField(summary, data, "endDate");
                copyField(summary, data, "technologies");
                String desc = stringField(data, "description");
                if (desc != null) summary.put("description", desc.length() > 150 ? desc.substring(0, 150) + "..." : desc);
            }
            case "projects" -> {
                copyField(summary, data, "name");
                copyField(summary, data, "role");
                copyField(summary, data, "technologies");
                String desc = stringField(data, "description");
                if (desc != null) summary.put("description", desc.length() > 150 ? desc.substring(0, 150) + "..." : desc);
            }
            case "education" -> {
                copyField(summary, data, "school");
                copyField(summary, data, "major");
                copyField(summary, data, "degree");
                copyField(summary, data, "startDate");
                copyField(summary, data, "endDate");
            }
            default -> {
                for (Map.Entry<String, Object> e : data.entrySet()) {
                    Object val = e.getValue();
                    if (val instanceof String s && s.length() < 300 && !isLikelyBase64(s)) {
                        summary.put(e.getKey(), s);
                    } else if (val instanceof Number || val instanceof Boolean) {
                        summary.put(e.getKey(), val);
                    } else if (val instanceof List && !((List<?>) val).isEmpty()) {
                        Object first = ((List<?>) val).get(0);
                        if (first instanceof String && ((String) first).length() < 200) {
                            summary.put(e.getKey(), val);
                        }
                    }
                }
            }
        }
        return summary;
    }

    private void copyField(Map<String, Object> target, Map<String, Object> source, String key) {
        Object val = source.get(key);
        if (val instanceof String s && !s.isBlank() && s.length() < 500 && !isLikelyBase64(s)) {
            target.put(key, s);
        } else if (val instanceof Number || val instanceof Boolean) {
            target.put(key, val);
        }
    }

    private String stringField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String s && !s.isBlank() ? s : null;
    }

    private boolean isLikelyBase64(String s) {
        if (s.length() < 100) return false;
        int nonBase64 = 0;
        for (int i = 0; i < Math.min(s.length(), 200); i++) {
            char c = s.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=')) {
                nonBase64++;
            }
        }
        return nonBase64 < 10;
    }

    private GeneratedCoach tryGenerateWithAi(Map<String, Object> snapshot) {
        try {
            String response = chatClient.prompt().system(SYSTEM_PROMPT).user(buildUserPrompt(snapshot)).call().content();
            String json = extractJson(response);
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            return parseGenerated(parsed, "ai");
        } catch (Exception e) {
            log.warn("AI 求职教练生成失败，使用兜底计划: {}", e.getMessage());
            log.debug("AI 求职教练生成失败详情", e);
            return null;
        }
    }

    private GeneratedCoach fallback(Map<String, Object> snapshot) {
        String target = String.valueOf(snapshot.getOrDefault("targetPosition", "Java 后端开发"));

        int score = 40;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<CoachTaskDraft> tasks = new ArrayList<>();

        boolean hasResume = snapshot.containsKey("resume") && snapshot.get("resume") instanceof Map<?, ?> resumeMap && !resumeMap.isEmpty();
        long interviewCompleted = ((Number) snapshot.getOrDefault("interviewCompleted", 0)).longValue();
        long applicationTotal = ((Number) snapshot.getOrDefault("applicationTotal", 0)).longValue();
        long trainingAnswerCount = ((Number) snapshot.getOrDefault("trainingAnswerCount", 0)).longValue();

        if (hasResume) {
            score += 20;
            strengths.add("已创建简历，有明确的求职方向");
            Map<?, ?> resumeData = (Map<?, ?>) snapshot.get("resume");
            List<?> sections = resumeData.get("sections") instanceof List ? (List<?>) resumeData.get("sections") : List.of();
            boolean hasSkills = sections.stream().anyMatch(s -> s instanceof Map && "skills".equals(((Map<?, ?>) s).get("type")));
            boolean hasExperience = sections.stream().anyMatch(s -> s instanceof Map && "work_experience".equals(((Map<?, ?>) s).get("type")));
            boolean hasProjects = sections.stream().anyMatch(s -> s instanceof Map && "projects".equals(((Map<?, ?>) s).get("type")));
            if (!hasExperience && !hasProjects) {
                weaknesses.add("简历缺少工作经历和项目经验模块");
                suggestions.add("补充工作经历和项目经验，突出技术栈和量化成果");
                tasks.add(new CoachTaskDraft(1, "完善简历项目经验",
                        "当前简历缺少项目经验，请补充 2-3 个核心项目，描述技术难点和你的贡献。", "resume", "high", null, null));
            }
            if (!hasSkills) {
                weaknesses.add("简历未列出技能清单");
                suggestions.add("在简历中明确列出技术栈和熟练度");
                tasks.add(new CoachTaskDraft(2, "列出技能清单",
                        "整理你掌握的技术栈（语言/框架/工具），按熟练度分级写入简历。", "resume", "high", null, null));
            }
            if (hasSkills && hasExperience) {
                strengths.add("简历包含技能和经历模块，结构完整");
            }
        } else {
            weaknesses.add("还未创建简历");
            suggestions.add("立即创建一份针对目标岗位的简历");
            tasks.add(new CoachTaskDraft(1, "创建简历",
                    "你还没有简历，请先创建一份针对 " + target + " 的简历。", "resume", "high", null, null));
        }

        if (interviewCompleted > 0) {
            score += 15;
            strengths.add("已完成 " + interviewCompleted + " 次模拟面试，有实战经验");
            Object avgScoreObj = snapshot.get("interviewAverageScore");
            int avgScore = avgScoreObj instanceof Number ? ((Number) avgScoreObj).intValue() : 0;
            if (avgScore >= 70) {
                strengths.add("面试平均分 " + avgScore + "，基础扎实");
                score += 5;
            } else if (avgScore > 0) {
                weaknesses.add("面试平均分 " + avgScore + "，有提升空间");
                suggestions.add("针对低分维度重点突破");
                tasks.add(new CoachTaskDraft(3, "分析面试薄弱点",
                        "回顾面试报告，针对 accuracy/depth 中得分低的维度专项练习。", "interview", "high", null, null));
            }
            List<?> reports = snapshot.get("interviewReports") instanceof List ? (List<?>) snapshot.get("interviewReports") : List.of();
            if (!reports.isEmpty()) {
                Map<?, ?> last = reports.size() > 0 && reports.get(reports.size() - 1) instanceof Map ? (Map<?, ?>) reports.get(reports.size() - 1) : null;
                if (last != null) {
                    Integer depth = last.get("depthScore") instanceof Number ? ((Number) last.get("depthScore")).intValue() : null;
                    Integer clarity = last.get("clarityScore") instanceof Number ? ((Number) last.get("clarityScore")).intValue() : null;
                    if (depth != null && depth < 70) {
                        weaknesses.add("项目深度不足（" + depth + "分），需加强技术深度和细节描述");
                        suggestions.add("准备 2-3 个技术难点案例，练习 STAR 法则表达");
                        tasks.add(new CoachTaskDraft(4, "准备项目深度问题",
                                "挑选 2 个最有技术挑战的项目，准备技术选型、难点、解决方案的详细回答。", "interview", "high", null, null));
                    }
                    if (clarity != null && clarity < 70) {
                        weaknesses.add("表达清晰度不足（" + clarity + "分），需提升结构化表达");
                        suggestions.add("练习用结论先行+分点阐述的方式回答面试题");
                        tasks.add(new CoachTaskDraft(5, "练习结构化表达",
                                "每天选 1 道面试题，用「结论→分点→总结」的结构练习回答并录音复盘。", "interview", "medium", null, null));
                    }
                }
            }
        } else {
            weaknesses.add("还未完成模拟面试");
            suggestions.add("建议先进行一次模拟面试，了解自身水平");
            tasks.add(new CoachTaskDraft(2, "进行一次模拟面试",
                    "还没有面试记录，预约一场 " + target + " 方向的模拟面试。", "interview", "high", null, null));
        }

        if (applicationTotal > 0) {
            score += 15;
            strengths.add("已投递 " + applicationTotal + " 个岗位，积极求职中");
            Object appByStatus = snapshot.get("applicationByStatus");
            if (appByStatus instanceof Map<?, ?> statusMap) {
                Object interviewingObj = ((Map) statusMap).getOrDefault("interviewing", 0);
                Object offerObj = ((Map) statusMap).getOrDefault("offer", 0);
                long interviewing = interviewingObj instanceof Number ? ((Number) interviewingObj).longValue() : 0;
                long offer = offerObj instanceof Number ? ((Number) offerObj).longValue() : 0;
                if (offer > 0) {
                    strengths.add("已获得 " + offer + " 个 Offer");
                    score += 10;
                } else if (interviewing > 0) {
                    strengths.add(interviewing + " 个岗位进入面试环节");
                } else {
                    weaknesses.add("投递后未进入面试环节，简历可能需要优化");
                    suggestions.add("根据岗位要求调整简历关键词匹配度");
                }
            }
        } else {
            weaknesses.add("还未开始投递");
            suggestions.add("建议每天投递 3-5 个岗位");
            tasks.add(new CoachTaskDraft(6, "搜索并投递岗位",
                    "还没有投递记录，每天投递 3-5 个匹配 " + target + " 的岗位。", "application", "high", null, null));
        }

        if (trainingAnswerCount > 0) {
            score += 10;
            strengths.add("有刷题训练记录，持续学习");
        } else {
            weaknesses.add("还未开始八股刷题");
            suggestions.add("建议从高频面试题开始刷题练习");
            tasks.add(new CoachTaskDraft(7, "开始八股刷题",
                    "还没有刷题记录，从 " + target + " 高频知识点开始练习。", "training", "high", null, null));
        }

        score = clamp(score, 0, 100);
        if (tasks.isEmpty()) {
            for (int day = 1; day <= 7; day++) {
                tasks.add(new CoachTaskDraft(day, "完成 Day " + day + " 求职准备",
                        "围绕 " + target + " 完成一项求职准备任务（简历优化、面试复盘或岗位投递）。", "habit", "medium", null, null));
            }
        } else {
            while (tasks.size() < 7) {
                int day = tasks.size() % 7 + 1;
                tasks.add(new CoachTaskDraft(day, "补充求职准备",
                        "完成一项简历、面试或投递相关的复盘任务。", "habit", "medium", null, null));
            }
        }

        String summary = String.format("求职竞争力评分 %d 分。", score);
        return new GeneratedCoach(target + " 求职诊断", score, summary,
                strengths.isEmpty() ? List.of("开始系统化求职准备") : strengths,
                weaknesses.isEmpty() ? List.of("需要更多数据和练习") : weaknesses,
                suggestions.isEmpty() ? List.of("按计划逐步完成 7 天任务") : suggestions,
                "7 天求职提升计划", "针对你的具体情况制定的 7 天提升计划。", tasks, "fallback");
    }

    private GeneratedCoach parseGenerated(Map<String, Object> parsed, String source) {
        Map<String, Object> diagnosis = objectMap(parsed.get("diagnosis"));
        if (diagnosis.isEmpty()) {
            diagnosis = parsed;
        }
        Map<String, Object> plan = objectMap(parsed.get("plan"));
        List<CoachTaskDraft> tasks = new ArrayList<>();
        Object taskSource = plan.containsKey("tasks") ? plan.get("tasks") : parsed.get("tasks");
        for (Map<String, Object> item : listOfMaps(taskSource)) {
            tasks.add(new CoachTaskDraft(number(item.get("dayIndex"), 1), string(item.get("title"), "求职任务"), string(item.get("description"), "完成一个求职准备动作。"),
                    string(item.get("taskType"), "habit"), string(item.get("priority"), "medium"), nullableString(item.get("referenceType")), nullableLong(item.get("referenceId"))));
        }
        return new GeneratedCoach(string(diagnosis.get("title"), "求职诊断"), number(diagnosis.get("overallScore"), 60), string(diagnosis.get("summary"), "已生成求职诊断。"),
                stringList(diagnosis.get("strengths")), stringList(diagnosis.get("weaknesses")), stringList(diagnosis.get("suggestions")),
                string(firstNonNull(plan.get("title"), parsed.get("planTitle")), "7 天求职计划"),
                string(firstNonNull(plan.get("summary"), parsed.get("planSummary")), "完成 7 天求职准备任务。"), tasks, source);
    }

    private List<CoachTask> normalizeTasks(Long userId, Long planId, List<CoachTaskDraft> drafts, Map<String, Object> snapshot) {
        List<CoachTaskDraft> normalized = new ArrayList<>(drafts == null ? List.of() : drafts);
        while (normalized.size() < 7) {
            int day = normalized.size() % 7 + 1;
            normalized.add(buildFallbackTask(day, snapshot));
        }
        if (normalized.size() > 21) {
            normalized = normalized.subList(0, 21);
        }
        List<CoachTask> tasks = new ArrayList<>();
        for (CoachTaskDraft draft : normalized) {
            CoachTask task = new CoachTask();
            task.setUserId(userId);
            task.setPlanId(planId);
            task.setDayIndex(clamp(draft.dayIndex(), 1, 7));
            task.setTitle(draft.title());
            task.setDescription(draft.description());
            task.setTaskType(TASK_TYPES.contains(draft.taskType()) ? draft.taskType() : "habit");
            task.setPriority(PRIORITIES.contains(draft.priority()) ? draft.priority() : "medium");
            task.setStatus("pending");
            task.setReferenceType(draft.referenceType() != null && REFERENCE_TYPES.contains(draft.referenceType()) ? draft.referenceType() : null);
            task.setReferenceId(task.getReferenceType() == null ? null : draft.referenceId());
            tasks.add(task);
        }
        return tasks;
    }

    private CoachTaskDraft buildFallbackTask(int day, Map<String, Object> snapshot) {
        boolean hasResume = snapshot.containsKey("resume") && snapshot.get("resume") instanceof Map<?, ?> && !((Map<?, ?>) snapshot.get("resume")).isEmpty();
        long interviewCompleted = ((Number) snapshot.getOrDefault("interviewCompleted", 0)).longValue();
        long applicationTotal = ((Number) snapshot.getOrDefault("applicationTotal", 0)).longValue();
        long trainingAnswerCount = ((Number) snapshot.getOrDefault("trainingAnswerCount", 0)).longValue();

        if (!hasResume) {
            return new CoachTaskDraft(day, "创建并完善简历",
                    "你还没有创建简历，建议先撰写一份针对目标岗位的简历，突出技术栈和项目经验。",
                    "resume", "high", null, null);
        }
        if (interviewCompleted == 0) {
            return new CoachTaskDraft(day, "进行一次模拟面试",
                    "还没有完成过模拟面试，建议预约一场目标岗位的面试练习，熟悉面试流程。",
                    "interview", "high", null, null);
        }
        if (applicationTotal == 0) {
            return new CoachTaskDraft(day, "搜索并投递岗位",
                    "还没有投递记录，建议每天投递 3-5 个匹配度高的岗位。",
                    "application", "high", null, null);
        }
        if (trainingAnswerCount == 0) {
            return new CoachTaskDraft(day, "开始八股刷题训练",
                    "还没有刷题记录，建议从目标岗位高频知识点开始练习。",
                    "training", "high", null, null);
        }
        return new CoachTaskDraft(day, "补充求职准备任务",
                "完成一次简历、八股或投递相关复盘。", "habit", "medium", null, null);
    }

    private CoachTask getOwnedTask(Long userId, Long id) {
        CoachTask task = taskMapper.selectById(id);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return task;
    }

    private CoachPlanVO toPlanVO(CoachPlan plan) {
        List<CoachTask> tasks = taskMapper.selectList(Wrappers.lambdaQuery(CoachTask.class).eq(CoachTask::getPlanId, plan.getId()).orderByAsc(CoachTask::getDayIndex).orderByAsc(CoachTask::getId));
        return toPlanVO(plan, tasks);
    }

    private CoachPlanVO toPlanVO(CoachPlan plan, List<CoachTask> tasks) {
        CoachPlanVO vo = new CoachPlanVO();
        vo.setId(plan.getId());
        vo.setDiagnosisId(plan.getDiagnosisId());
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setTargetPosition(plan.getTargetPosition());
        vo.setTargetDays(plan.getTargetDays());
        vo.setStatus(plan.getStatus());
        vo.setSource(plan.getSource());
        vo.setTasks(tasks.stream().map(this::toTaskVO).toList());
        vo.setTotalTaskCount(tasks.size());
        vo.setCompletedTaskCount((int) tasks.stream().filter(t -> "completed".equals(t.getStatus())).count());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());
        return vo;
    }

    private CoachDiagnosisVO toDiagnosisVO(CoachDiagnosis diagnosis) {
        return toDiagnosisVO(diagnosis, true);
    }

    private CoachDiagnosisVO toDiagnosisVO(CoachDiagnosis diagnosis, boolean includeSnapshot) {
        CoachDiagnosisVO vo = new CoachDiagnosisVO();
        vo.setId(diagnosis.getId());
        vo.setTitle(diagnosis.getTitle());
        vo.setOverallScore(diagnosis.getOverallScore());
        vo.setSummary(diagnosis.getSummary());
        vo.setStrengths(defaultList(diagnosis.getStrengthsJson()));
        vo.setWeaknesses(defaultList(diagnosis.getWeaknessesJson()));
        vo.setSuggestions(defaultList(diagnosis.getSuggestionsJson()));
        if (includeSnapshot) {
            vo.setDataSnapshot(diagnosis.getDataSnapshotJson() == null ? Map.of() : diagnosis.getDataSnapshotJson());
        } else {
            vo.setDataSnapshot(Map.of());
        }
        vo.setDataCompleteness(diagnosis.getDataCompleteness());
        vo.setSource(diagnosis.getSource());
        vo.setCreateTime(diagnosis.getCreateTime());
        vo.setUpdateTime(diagnosis.getUpdateTime());
        return vo;
    }

    private CoachTaskVO toTaskVO(CoachTask task) {
        CoachTaskVO vo = new CoachTaskVO();
        vo.setId(task.getId());
        vo.setPlanId(task.getPlanId());
        vo.setDayIndex(task.getDayIndex());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setTaskType(task.getTaskType());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setReferenceType(task.getReferenceType());
        vo.setReferenceId(task.getReferenceId());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private int calculateCompleteness(Map<String, Object> snapshot) {
        int score = 0;
        if (hasText(snapshot.get("targetPosition"))) score += 5;
        if (hasText(snapshot.get("techDirection"))) score += 5;
        if (hasText(snapshot.get("city"))) score += 5;
        if (hasText(snapshot.get("jobStatus"))) score += 5;
        if (snapshot.get("resume") instanceof Map<?, ?> resume && !resume.isEmpty()) score += 30;
        if (((Number) snapshot.getOrDefault("interviewCompleted", 0)).longValue() > 0) score += 25;
        if (((Number) snapshot.getOrDefault("applicationTotal", 0)).longValue() > 0) score += 10;
        if (((Number) snapshot.getOrDefault("trainingAnswerCount", 0)).longValue() > 0) score += 10;
        return clamp(score, 0, 100);
    }

    String buildUserPrompt(Map<String, Object> snapshot) throws com.fasterxml.jackson.core.JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(snapshot);
    }

    private String extractJson(String response) {
        var matcher = JSON_CODE_BLOCK_PATTERN.matcher(response == null ? "" : response);
        return matcher.find() ? matcher.group(1).trim() : response;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int number(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }
    private static String string(Object value, String fallback) { return value instanceof String s && !s.isBlank() ? s : fallback; }
    private static boolean hasText(Object value) { return value instanceof String s && !s.isBlank(); }
    private static Object firstNonNull(Object first, Object second) { return first != null ? first : second; }
    private static String nullableString(Object value) { return value instanceof String s && !s.isBlank() ? s : null; }
    private static Long nullableLong(Object value) { return value instanceof Number number ? number.longValue() : null; }
    private static List<String> defaultList(List<String> value) { return value == null ? List.of() : value; }
    private static List<String> stringList(Object value) { return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : Collections.emptyList(); }
    private static Map<String, Object> objectMap(Object value) { return value instanceof Map<?, ?> map ? map.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(String.valueOf(e.getKey()), e.getValue()), HashMap::putAll) : new HashMap<>(); }
    private static List<Map<String, Object>> listOfMaps(Object value) { return value instanceof List<?> list ? list.stream().filter(Map.class::isInstance).map(Map.class::cast).map(CoachServiceImpl::objectMap).toList() : List.of(); }

    private record GeneratedCoach(String diagnosisTitle, int overallScore, String diagnosisSummary, List<String> strengths, List<String> weaknesses, List<String> suggestions, String planTitle, String planSummary, List<CoachTaskDraft> tasks, String source) {}
    private record CoachTaskDraft(int dayIndex, String title, String description, String taskType, String priority, String referenceType, Long referenceId) {}
}
