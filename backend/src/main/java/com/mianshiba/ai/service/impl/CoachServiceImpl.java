package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.CoachDiagnosis;
import com.mianshiba.ai.model.entity.CoachPlan;
import com.mianshiba.ai.model.entity.CoachTask;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Set<String> TASK_TYPES = Set.of("resume", "interview", "training", "application", "job", "habit");
    private static final Set<String> PRIORITIES = Set.of("high", "medium", "low");
    private static final Set<String> REFERENCE_TYPES = Set.of("resume", "interview_session", "interview_report", "training_question", "training_plan", "job_application", "job");

    private static final String SYSTEM_PROMPT = "你是一位资深程序员求职教练。请基于用户数据生成求职诊断和 7 天计划。只返回 JSON，不要解释。";

    private final JwtUtils jwtUtils;
    private final ChatClient chatClient;
    private final CoachDiagnosisMapper diagnosisMapper;
    private final CoachPlanMapper planMapper;
    private final CoachTaskMapper taskMapper;
    private final UserMapper userMapper;

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
        diagnosis.setDataCompleteness(calculateCompleteness(user));
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

        List<CoachTask> tasks = normalizeTasks(userId, plan.getId(), generated.tasks());
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
        CoachDiagnosis latest = diagnosisMapper.selectOne(Wrappers.lambdaQuery(CoachDiagnosis.class)
                .eq(CoachDiagnosis::getUserId, userId)
                .orderByDesc(CoachDiagnosis::getCreateTime)
                .last("LIMIT 1"));
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
                .orderByDesc(CoachDiagnosis::getCreateTime)).stream().map(this::toDiagnosisVO).toList();
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

    private Map<String, Object> buildSnapshot(User user, CoachGenerateRequest request) {
        Map<String, Object> snapshot = new HashMap<>();
        String targetPosition = request != null && request.getTargetPosition() != null && !request.getTargetPosition().isBlank()
                ? request.getTargetPosition().trim() : user.getTargetPosition();
        snapshot.put("targetPosition", targetPosition == null ? "" : targetPosition);
        snapshot.put("techDirection", user.getTechDirection() == null ? "" : user.getTechDirection());
        snapshot.put("workYears", user.getWorkYears() == null ? 0 : user.getWorkYears());
        snapshot.put("city", user.getCity() == null ? "" : user.getCity());
        snapshot.put("jobStatus", user.getJobStatus() == null ? "" : user.getJobStatus());
        snapshot.put("focus", request == null || request.getFocus() == null ? "" : request.getFocus().trim());
        return snapshot;
    }

    private GeneratedCoach tryGenerateWithAi(Map<String, Object> snapshot) {
        try {
            String response = chatClient.prompt().system(SYSTEM_PROMPT).user(OBJECT_MAPPER.writeValueAsString(snapshot)).call().content();
            String json = extractJson(response);
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            return parseGenerated(parsed, "ai");
        } catch (Exception e) {
            log.warn("AI 求职教练生成失败，使用兜底计划", e);
            return null;
        }
    }

    private GeneratedCoach fallback(Map<String, Object> snapshot) {
        String target = String.valueOf(snapshot.getOrDefault("targetPosition", "Java 后端开发"));
        List<CoachTaskDraft> tasks = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            tasks.add(new CoachTaskDraft(day, "完成 Day " + day + " 八股复习", "围绕 " + target + " 高频知识点完成 2 道八股题复盘。", "training", "high", null, null));
            tasks.add(new CoachTaskDraft(day, "整理 Day " + day + " 求职行动", "优化一个简历或投递相关动作，并记录结果。", "habit", "medium", null, null));
        }
        return new GeneratedCoach("求职准备诊断", 60, "当前数据不足或 AI 暂不可用，已生成基础 7 天求职启动计划。",
                List.of("已经开始系统化准备求职"), List.of("需要补齐更多训练和投递数据"), List.of("先完成 7 天基础行动计划"),
                "7 天求职启动计划", "每天完成八股复习和一个求职行动。", tasks, "fallback");
    }

    private GeneratedCoach parseGenerated(Map<String, Object> parsed, String source) {
        Map<String, Object> diagnosis = objectMap(parsed.get("diagnosis"));
        Map<String, Object> plan = objectMap(parsed.get("plan"));
        List<CoachTaskDraft> tasks = new ArrayList<>();
        for (Map<String, Object> item : listOfMaps(plan.get("tasks"))) {
            tasks.add(new CoachTaskDraft(number(item.get("dayIndex"), 1), string(item.get("title"), "求职任务"), string(item.get("description"), "完成一个求职准备动作。"),
                    string(item.get("taskType"), "habit"), string(item.get("priority"), "medium"), nullableString(item.get("referenceType")), nullableLong(item.get("referenceId"))));
        }
        return new GeneratedCoach(string(diagnosis.get("title"), "求职诊断"), number(diagnosis.get("overallScore"), 60), string(diagnosis.get("summary"), "已生成求职诊断。"),
                stringList(diagnosis.get("strengths")), stringList(diagnosis.get("weaknesses")), stringList(diagnosis.get("suggestions")),
                string(plan.get("title"), "7 天求职计划"), string(plan.get("summary"), "完成 7 天求职准备任务。"), tasks, source);
    }

    private List<CoachTask> normalizeTasks(Long userId, Long planId, List<CoachTaskDraft> drafts) {
        List<CoachTaskDraft> normalized = new ArrayList<>(drafts == null ? List.of() : drafts);
        while (normalized.size() < 14) {
            int day = normalized.size() % 7 + 1;
            normalized.add(new CoachTaskDraft(day, "补充求职准备任务", "完成一次简历、八股或投递相关复盘。", "habit", "medium", null, null));
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
        CoachDiagnosisVO vo = new CoachDiagnosisVO();
        vo.setId(diagnosis.getId());
        vo.setTitle(diagnosis.getTitle());
        vo.setOverallScore(diagnosis.getOverallScore());
        vo.setSummary(diagnosis.getSummary());
        vo.setStrengths(defaultList(diagnosis.getStrengthsJson()));
        vo.setWeaknesses(defaultList(diagnosis.getWeaknessesJson()));
        vo.setSuggestions(defaultList(diagnosis.getSuggestionsJson()));
        vo.setDataSnapshot(diagnosis.getDataSnapshotJson() == null ? Map.of() : diagnosis.getDataSnapshotJson());
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

    private int calculateCompleteness(User user) {
        int score = 20;
        if (user.getTargetPosition() != null && !user.getTargetPosition().isBlank()) score += 20;
        if (user.getTechDirection() != null && !user.getTechDirection().isBlank()) score += 20;
        if (user.getCity() != null && !user.getCity().isBlank()) score += 20;
        if (user.getJobStatus() != null && !user.getJobStatus().isBlank()) score += 20;
        return clamp(score, 0, 100);
    }

    private String extractJson(String response) {
        var matcher = JSON_CODE_BLOCK_PATTERN.matcher(response == null ? "" : response);
        return matcher.find() ? matcher.group(1).trim() : response;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int number(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }
    private static String string(Object value, String fallback) { return value instanceof String s && !s.isBlank() ? s : fallback; }
    private static String nullableString(Object value) { return value instanceof String s && !s.isBlank() ? s : null; }
    private static Long nullableLong(Object value) { return value instanceof Number number ? number.longValue() : null; }
    private static List<String> defaultList(List<String> value) { return value == null ? List.of() : value; }
    private static List<String> stringList(Object value) { return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : Collections.emptyList(); }
    private static Map<String, Object> objectMap(Object value) { return value instanceof Map<?, ?> map ? map.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(String.valueOf(e.getKey()), e.getValue()), HashMap::putAll) : new HashMap<>(); }
    private static List<Map<String, Object>> listOfMaps(Object value) { return value instanceof List<?> list ? list.stream().filter(Map.class::isInstance).map(Map.class::cast).map(CoachServiceImpl::objectMap).toList() : List.of(); }

    private record GeneratedCoach(String diagnosisTitle, int overallScore, String diagnosisSummary, List<String> strengths, List<String> weaknesses, List<String> suggestions, String planTitle, String planSummary, List<CoachTaskDraft> tasks, String source) {}
    private record CoachTaskDraft(int dayIndex, String title, String description, String taskType, String priority, String referenceType, Long referenceId) {}
}
