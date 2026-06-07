package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.ResumeChatMessageMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.dto.resume.ResumeImportRequest;
import com.mianshiba.ai.model.dto.resume.ResumeWholeOptimizeRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeChatMessage;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeImportPreviewVO;
import com.mianshiba.ai.model.vo.resume.ResumeWholeOptimizeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GENERATE_SYSTEM_PROMPT =
            "你是一位专业的简历撰写助手。请根据以下信息生成一份简历：\n" +
            "- 目标岗位：%s\n" +
            "- 技术方向：%s\n" +
            "- 工作年限：%s\n\n" +
            "请以 JSON 数组格式返回，每个元素包含 sectionType 和 sectionData 字段。\n" +
            "sectionType 可选值：basic, education, work, project, skills, summary\n" +
            "sectionData 是一个 JSON 对象，包含该模块的具体内容。\n" +
            "请直接返回 JSON 数组，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String OPTIMIZE_SYSTEM_PROMPT =
            "你是一位专业的简历优化助手。请优化以下简历模块内容，目标岗位是 %s，模块类型是 %s。\n" +
            "请返回优化后的 JSON 对象，保持原有的数据结构，只优化内容质量。\n" +
            "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String SCORE_SYSTEM_PROMPT =
            "你是一位专业的简历评估助手。请对以下简历内容进行评分，目标岗位是 %s。\n" +
            "请返回 JSON 格式的评分结果，包含：\n" +
            "- score：总分（0-100）\n" +
            "- dimensions：维度评分\n" +
            "  - completeness：完整性（0-100）\n" +
            "  - completenessComment：完整性评语\n" +
            "  - professionalism：专业度（0-100）\n" +
            "  - professionalismComment：专业度评语\n" +
            "  - matching：岗位匹配度（0-100）\n" +
            "  - matchingComment：匹配度评语\n" +
            "- suggestions：改进建议列表\n\n" +
            "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String CHAT_SYSTEM_PROMPT =
            "你是一位专业的简历顾问助手。用户正在编辑简历，以下是当前简历的模块内容摘要：\n%s\n\n" +
            "请根据用户的问题提供建议。回答要简洁专业。";

    private static final String IMPORT_PARSE_PROMPT =
            "你是一位专业的简历解析助手。请将以下简历文本解析为结构化的 JSON 格式。\n\n" +
            "简历文本：\n%s\n\n" +
            "请以 JSON 数组格式返回，每个元素包含 sectionType 和 sectionData 字段。\n" +
            "sectionType 可选值：basic, education, work, project, skills, summary\n" +
            "sectionData 是一个 JSON 对象，包含该模块的具体内容。\n" +
            "如果有无法归类的信息，放入 summary 模块。\n" +
            "请直接返回 JSON 数组，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String WHOLE_OPTIMIZE_PROMPT =
            "你是一位专业的简历优化助手。请对以下完整简历内容进行整体优化。%s\n\n" +
            "当前简历模块：\n%s\n\n" +
            "请返回 JSON 格式的优化建议，包含：\n" +
            "- globalSuggestions：全局建议列表（字符串数组）\n" +
            "- optimizedSections：优化后的模块数组，每个元素包含 sectionType 和 sectionData\n\n" +
            "保持原有数据结构，只优化内容质量，增强 STAR 法则表达和量化成果。\n" +
            "请直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private final ChatClient chatClient;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final JobMapper jobMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final ResumeChatMessageMapper chatMessageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO generateResume(String authorizationHeader, AiGenerateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        String techDirection = request.getTechDirection() != null ? request.getTechDirection() : "不限";
        String workYears = request.getWorkYears() != null ? String.valueOf(request.getWorkYears()) : "不限";
        String systemPrompt = String.format(GENERATE_SYSTEM_PROMPT,
                request.getTargetPosition(), techDirection, workYears);
        String userMessage = String.format("请为「%s」岗位生成一份简历。", request.getTargetPosition());

        String aiResponse = callAi(systemPrompt, userMessage);

        String json = extractJsonFromResponse(aiResponse);
        List<Map<String, Object>> sectionItems;
        try {
            sectionItems = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 生成简历响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(request.getTargetPosition() + " - 简历");
        resume.setTemplateType("minimal_tech");
        resume.setStatus("draft");
        resume.setSource("ai");
        resume.setVersion(1);
        resume.setIsDelete(0);
        resumeMapper.insert(resume);

        List<ResumeSection> sectionEntities = new ArrayList<>();
        for (int i = 0; i < sectionItems.size(); i++) {
            Map<String, Object> item = sectionItems.get(i);
            ResumeSection section = new ResumeSection();
            section.setResumeId(resume.getId());
            section.setSectionType((String) item.get("sectionType"));
            @SuppressWarnings("unchecked")
            Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");
            section.setSectionData(sectionData);
            section.setSortOrder(i);
            section.setAiGenerated(1);
            section.setIsDelete(0);
            resumeSectionMapper.insert(section);
            sectionEntities.add(section);
        }

        return toResumeDetailVO(resume, sectionEntities);
    }

    @Override
    public Map<String, Object> optimizeSection(AiOptimizeRequest request, String targetPosition) {
        String sectionDataJson;
        try {
            sectionDataJson = objectMapper.writeValueAsString(request.getSectionData());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        String systemPrompt = String.format(OPTIMIZE_SYSTEM_PROMPT,
                targetPosition != null ? targetPosition : "未知岗位",
                request.getSectionType());

        if (request.getJobId() != null) {
            Job job = jobMapper.selectById(request.getJobId());
            if (job == null) {
                throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
            }
            JobAnalysis jobAnalysis = jobAnalysisMapper.selectOne(
                    Wrappers.lambdaQuery(JobAnalysis.class)
                            .eq(JobAnalysis::getJobId, request.getJobId()));

            if (jobAnalysis != null) {
                String jobContext = String.format(
                        "\n\n目标岗位信息：\n职位名称：%s\n岗位要求：%s\n核心技能：%s\n面试重点：%s\n请针对以上岗位要求进行优化。",
                        job.getTitle(),
                        jobAnalysis.getRequirementSummary() != null ? jobAnalysis.getRequirementSummary() : "",
                        jobAnalysis.getCoreSkills() != null ? jobAnalysis.getCoreSkills() : "",
                        jobAnalysis.getInterviewFocus() != null ? jobAnalysis.getInterviewFocus() : "");
                systemPrompt += jobContext;
            }
        }

        String aiResponse = callAi(systemPrompt, sectionDataJson);

        String json = extractJsonFromResponse(aiResponse);
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 优化模块响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    @Override
    public AiScoreVO scoreResume(List<SectionVO> sections, String targetPosition) {
        String sectionsJson;
        try {
            sectionsJson = objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        String systemPrompt = String.format(SCORE_SYSTEM_PROMPT,
                targetPosition != null ? targetPosition : "未知岗位");

        String aiResponse = callAi(systemPrompt, sectionsJson);

        String json = extractJsonFromResponse(aiResponse);
        try {
            return objectMapper.readValue(json, AiScoreVO.class);
        } catch (JsonProcessingException e) {
            log.error("AI 评分响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

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

    @Override
    public ResumeImportPreviewVO importResumePreview(String authorizationHeader, ResumeImportRequest request) {
        resolveUserId(authorizationHeader);

        if (request.getRawText() == null || request.getRawText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String systemPrompt = String.format(IMPORT_PARSE_PROMPT, request.getRawText());
        String aiResponse = callAi(systemPrompt, "请解析这份简历。");

        String json = extractJsonFromResponse(aiResponse);
        List<Map<String, Object>> sectionItems;
        try {
            sectionItems = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 导入简历响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.RESUME_IMPORT_ERROR);
        }

        List<String> warnings = new ArrayList<>();
        String title = "导入简历";

        List<SectionVO> sectionVOs = new ArrayList<>();
        for (int i = 0; i < sectionItems.size(); i++) {
            Map<String, Object> item = sectionItems.get(i);
            String sectionType = (String) item.get("sectionType");
            @SuppressWarnings("unchecked")
            Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");

            if (sectionType == null || sectionData == null) {
                warnings.add("第 " + (i + 1) + " 个模块缺少 sectionType 或 sectionData");
                continue;
            }

            if ("basic".equals(sectionType) && sectionData.containsKey("name")) {
                title = sectionData.get("name").toString() + " - 简历";
            }

            SectionVO sectionVO = new SectionVO();
            sectionVO.setSectionType(sectionType);
            sectionVO.setSectionData(sectionData);
            sectionVO.setSortOrder(i);
            sectionVO.setAiGenerated(1);
            sectionVOs.add(sectionVO);
        }

        ResumeImportPreviewVO vo = new ResumeImportPreviewVO();
        vo.setTitle(title);
        vo.setTemplateType("minimal_tech");
        vo.setSections(sectionVOs);
        vo.setWarnings(warnings);
        return vo;
    }

    @Override
    public ResumeWholeOptimizeVO optimizeWholeResume(String authorizationHeader, ResumeWholeOptimizeRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        Resume resume = getResumeAndCheckOwner(request.getResumeId(), userId);

        List<ResumeSection> sections = resumeSectionMapper.selectList(
                Wrappers.lambdaQuery(ResumeSection.class)
                        .eq(ResumeSection::getResumeId, resume.getId())
                        .orderByAsc(ResumeSection::getSortOrder));

        List<SectionVO> currentSections = sections.stream().map(this::toSectionVO).collect(Collectors.toList());
        String targetPosition = request.getTargetPosition() != null ? request.getTargetPosition() : extractTargetPosition(currentSections);

        AiScoreVO beforeScoreResult = scoreResume(currentSections, targetPosition);
        Integer beforeScore = beforeScoreResult.getScore();

        String jobContext = "";
        if (request.getJobId() != null) {
            Job job = jobMapper.selectById(request.getJobId());
            if (job != null) {
                JobAnalysis jobAnalysis = jobAnalysisMapper.selectOne(
                        Wrappers.lambdaQuery(JobAnalysis.class)
                                .eq(JobAnalysis::getJobId, request.getJobId()));
                if (jobAnalysis != null) {
                    jobContext = String.format(
                            "\n\n目标岗位信息：\n职位名称：%s\n岗位要求：%s\n核心技能：%s\n面试重点：%s\n请针对以上岗位要求进行优化。",
                            job.getTitle(),
                            jobAnalysis.getRequirementSummary() != null ? jobAnalysis.getRequirementSummary() : "",
                            jobAnalysis.getCoreSkills() != null ? jobAnalysis.getCoreSkills() : "",
                            jobAnalysis.getInterviewFocus() != null ? jobAnalysis.getInterviewFocus() : "");
                }
            }
        }

        if (request.getOptimizeGoal() != null && !request.getOptimizeGoal().isBlank()) {
            jobContext += "\n\n用户优化目标：" + request.getOptimizeGoal();
        }

        String sectionsJson;
        try {
            sectionsJson = objectMapper.writeValueAsString(currentSections);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.RESUME_OPTIMIZE_ERROR);
        }

        String systemPrompt = String.format(WHOLE_OPTIMIZE_PROMPT, jobContext, sectionsJson);
        String aiResponse = callAi(systemPrompt, "请优化这份简历。");

        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 整份优化响应解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.RESUME_OPTIMIZE_ERROR);
        }

        @SuppressWarnings("unchecked")
        List<String> globalSuggestions = (List<String>) result.get("globalSuggestions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> optimizedSectionMaps = (List<Map<String, Object>>) result.get("optimizedSections");

        List<SectionVO> optimizedSections = new ArrayList<>();
        if (optimizedSectionMaps != null) {
            for (int i = 0; i < optimizedSectionMaps.size(); i++) {
                Map<String, Object> item = optimizedSectionMaps.get(i);
                SectionVO sectionVO = new SectionVO();
                sectionVO.setSectionType((String) item.get("sectionType"));
                @SuppressWarnings("unchecked")
                Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");
                sectionVO.setSectionData(sectionData);
                sectionVO.setSortOrder(i);
                sectionVO.setAiGenerated(1);
                optimizedSections.add(sectionVO);
            }
        }

        ResumeWholeOptimizeVO vo = new ResumeWholeOptimizeVO();
        vo.setBeforeScore(beforeScore);
        vo.setEstimatedAfterScore(Math.min(100, beforeScore + 10));
        vo.setGlobalSuggestions(globalSuggestions != null ? globalSuggestions : new ArrayList<>());
        vo.setOptimizedSections(optimizedSections);
        return vo;
    }

    private String extractTargetPosition(List<SectionVO> sections) {
        return sections.stream()
                .filter(s -> "basic".equals(s.getSectionType()))
                .findFirst()
                .map(SectionVO::getSectionData)
                .filter(data -> data != null && data.containsKey("targetPosition"))
                .map(data -> data.get("targetPosition").toString())
                .orElse(null);
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
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return resume;
    }

    private String callAi(String systemPrompt, String userMessage) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private String extractJsonFromResponse(String text) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.trim();
    }

    private String buildSectionsSummary(List<ResumeSection> sections) {
        return sections.stream()
                .map(s -> {
                    try {
                        return s.getSectionType() + ": " + objectMapper.writeValueAsString(s.getSectionData());
                    } catch (JsonProcessingException e) {
                        return s.getSectionType() + ": (序列化失败)";
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    private ResumeDetailVO toResumeDetailVO(Resume resume, List<ResumeSection> sections) {
        ResumeDetailVO vo = new ResumeDetailVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setTemplateType(resume.getTemplateType());
        vo.setStatus(resume.getStatus());
        vo.setSource(resume.getSource());
        vo.setVersion(resume.getVersion());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        vo.setSections(sections.stream().map(this::toSectionVO).collect(Collectors.toList()));
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
