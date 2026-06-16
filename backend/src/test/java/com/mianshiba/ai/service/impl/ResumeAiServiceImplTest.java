package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeChatMessageMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeChatMessage;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ResumeAiServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private ChatModel chatModel;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private ResumeSectionMapper resumeSectionMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ResumeChatMessageMapper chatMessageMapper;

    private JwtUtils jwtUtils;
    private ResumeAiServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        service = new ResumeAiServiceImpl(chatClient, resumeMapper, resumeSectionMapper, userMapper, jwtUtils, chatMessageMapper, new ObjectMapper());
    }

    @Test
    void generateResumeReturnsSections() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());

        String aiResponse = """
                ```json
                [
                    {"sectionType": "basic", "sectionData": {"name": "张三", "targetPosition": "Java开发工程师"}},
                    {"sectionType": "summary", "sectionData": {"content": "5年Java开发经验"}}
                ]
                ```
                """;
        mockAiResponse(aiResponse);

        when(resumeMapper.insert(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(1L);
            return 1;
        });
        when(resumeSectionMapper.insert(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection section = invocation.getArgument(0);
            section.setId(section.getSortOrder() + 10L);
            return 1;
        });

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        AiGenerateRequest request = new AiGenerateRequest();
        request.setTargetPosition("Java开发工程师");
        request.setTechDirection("Java后端");
        request.setWorkYears(5);

        ResumeDetailVO result = service.generateResume(auth, request);

        assertThat(result.getTitle()).isEqualTo("Java开发工程师 - 简历");
        assertThat(result.getSections()).hasSize(2);
        assertThat(result.getSections().get(0).getSectionType()).isEqualTo("basic");
        assertThat(result.getSections().get(1).getSectionType()).isEqualTo("summary");
    }

    @Test
    void optimizeSectionReturnsOptimizedData() {
        mockAiResponse("""
                ```json
                {"company": "优化后公司", "highlights": ["主导核心项目", "团队技术负责人"]}
                ```
                """);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "测试公司"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

        assertThat(result).containsEntry("company", "优化后公司");
        assertThat(result).containsKey("highlights");
    }

    @Test
    void optimizeWorkSectionNormalizesDescriptionAndLimitsHighlights() {
        mockAiResponse("""
                ```json
                {
                    "company": "XX科技有限公司",
                    "position": "Java开发实习生",
                    "description": "参与CRM订单模块开发，完成接口联调，支持日处理订单量5000+。优化SQL查询。",
                    "highlights": [
                        "CRM订单模块",
                        "接口联调",
                        "这是一条过长的亮点内容，包含大量工作描述，不应该继续停留在亮点标签里，因为它本质上是正文描述",
                        "SQL优化"
                    ]
                }
                ```
                """);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "XX科技有限公司", "description", "参与订单模块开发"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

        assertThat(result.get("description").toString()).startsWith("<ul>");
        assertThat(result.get("description").toString()).contains("<li>");
        assertThat(result.get("description").toString()).doesNotContain("这是一条过长的亮点内容");
        assertThat((List<?>) result.get("highlights")).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void optimizeEducationSectionWritesActivitiesForFrontend() {
        mockAiResponse("""
                ```json
                {
                    "school": "江西财经大学",
                    "major": "软件工程",
                    "degree": "本科",
                    "highlights": [
                        "专业前15%",
                        "数据库课程设计",
                        "校级奖学金",
                        "参与公司内部CRM系统订单模块的迭代开发，独立完成订单查询、状态变更等接口编码与测试"
                    ]
                }
                ```
                """);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(11L);
        request.setSectionType("education");
        request.setSectionData(Map.of("school", "江西财经大学"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

        assertThat(result.get("activities").toString()).startsWith("<ul>");
        assertThat(result.get("activities").toString()).contains("专业前15%", "数据库课程设计", "校级奖学金");
        assertThat(result).doesNotContainKey("highlights");
    }

    @Test
    void optimizeSectionPromptContainsModuleSpecificRules() {
        mockAiResponse("""
                ```json
                {"description":"<ul><li>负责订单接口开发，支撑日处理订单量5000+。</li></ul>"}
                ```
                """);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(12L);
        request.setSectionType("project");
        request.setSectionData(Map.of("name", "在线考试系统", "description", "考试系统"));

        service.optimizeSection(request, "Java开发工程师");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String prompt = captor.getValue().getInstructions().toString();
        assertThat(prompt).contains("模块化写作规则");
        assertThat(prompt).contains("project");
        assertThat(prompt).contains("<ul><li>");
        assertThat(prompt).contains("highlights 只能作为短标签");
    }

    @Test
    void scoreResumeReturnsScoreAndSuggestions() {
        mockAiResponse("""
                ```json
                {
                    "score": 85,
                    "dimensions": {
                        "completeness": 80,
                        "completenessComment": "内容较完整",
                        "professionalism": 90,
                        "professionalismComment": "专业度高",
                        "matching": 85,
                        "matchingComment": "岗位匹配度较好"
                    },
                    "suggestions": ["建议补充项目量化数据", "建议增加技术栈描述"]
                }
                ```
                """);

        SectionVO section = new SectionVO();
        section.setSectionType("basic");
        section.setSectionData(Map.of("name", "张三"));

        AiScoreVO result = service.scoreResume(List.of(section), "Java开发工程师");

        assertThat(result.getScore()).isEqualTo(85);
        assertThat(result.getDimensions().getCompleteness()).isEqualTo(80);
        assertThat(result.getDimensions().getProfessionalism()).isEqualTo(90);
        assertThat(result.getDimensions().getMatching()).isEqualTo(85);
        assertThat(result.getSuggestions()).hasSize(2);
    }

    @Test
    void scoreResumeCapsInflatedScoreForWeakResume() {
        mockAiResponse("""
                ```json
                {
                    "score": 88,
                    "dimensions": {
                        "completeness": 90,
                        "completenessComment": "结构完整",
                        "professionalism": 85,
                        "professionalismComment": "项目较好",
                        "matching": 88,
                        "matchingComment": "匹配Java岗位"
                    },
                    "suggestions": ["继续补充项目细节"]
                }
                ```
                """);

        SectionVO basic = section("basic", Map.of("name", "张三", "targetPosition", "Java开发工程师"));
        SectionVO skills = section("skills", Map.of("categories", List.of(Map.of("name", "后端", "items", List.of("Java", "Spring Boot")))));
        SectionVO work = section("work", Map.of("company", "XX科技", "position", "Java实习生", "description", "负责后端接口开发。"));
        SectionVO project = section("project", Map.of("name", "在线考试系统", "description", "面向高校的在线考试平台。"));

        AiScoreVO result = service.scoreResume(List.of(basic, skills, work, project), "Java开发工程师");

        assertThat(result.getScore()).isLessThanOrEqualTo(70);
        assertThat(result.getDimensions().getProfessionalism()).isLessThanOrEqualTo(75);
        assertThat(result.getSuggestions()).anyMatch(item -> item.contains("项目经历") && item.contains("具体负责"));
    }

    @Test
    void scorePromptContainsStrictRubric() {
        mockAiResponse("""
                ```json
                {
                    "score": 62,
                    "dimensions": {
                        "completeness": 65,
                        "completenessComment": "内容偏薄",
                        "professionalism": 60,
                        "professionalismComment": "项目细节不足",
                        "matching": 62,
                        "matchingComment": "匹配度一般"
                    },
                    "suggestions": ["补充项目职责和量化结果"]
                }
                ```
                """);

        SectionVO basic = section("basic", Map.of("name", "张三", "targetPosition", "Java开发工程师"));

        service.scoreResume(List.of(basic), "Java开发工程师");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String prompt = captor.getValue().getInstructions().toString();
        assertThat(prompt).contains("不要鼓励式评分");
        assertThat(prompt).contains("40-65 分区间");
        assertThat(prompt).contains("80 分以上必须同时满足");
    }

    @Test
    void chatStreamFallsBackToCallWhenStreamBadRequest() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1001L);
        when(resumeMapper.selectById(1L)).thenReturn(resume);
        ResumeSection section = new ResumeSection();
        section.setSectionType("basic");
        section.setSectionData(Map.of("targetPosition", "Java开发工程师"));
        when(resumeSectionMapper.selectList(any())).thenReturn(List.of(section));
        when(chatModel.stream(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(Flux.error(WebClientResponseException.create(
                        400,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        "{\"error\":{\"message\":\"stream invalid\"}}".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8)));
        mockAiResponse("请补充项目量化成果。");

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");

        List<ResumeChatStreamEventVO> chunks = service.chatStream(auth, 1L, "怎么优化项目经历？").collectList().block();

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getEvent()).isEqualTo(ResumeChatStreamEventVO.EVENT_MESSAGE);
        assertThat(chunks.get(0).getContent()).isEqualTo("请补充项目量化成果。");
        ArgumentCaptor<ResumeChatMessage> messageCaptor = ArgumentCaptor.forClass(ResumeChatMessage.class);
        verify(chatMessageMapper, times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        assertThat(messageCaptor.getAllValues().get(1).getContent()).isEqualTo("请补充项目量化成果。");
    }

    @Test
    void chatStreamOmitsLargeBase64FieldsFromResumeContext() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1001L);
        when(resumeMapper.selectById(1L)).thenReturn(resume);
        ResumeSection section = new ResumeSection();
        section.setSectionType("basic");
        section.setSectionData(Map.of(
                "name", "张三",
                "avatar", "data:image/png;base64," + "A".repeat(200_000),
                "targetPosition", "Java开发工程师"));
        when(resumeSectionMapper.selectList(any())).thenReturn(List.of(section));
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException("force fallback")));
        mockAiResponse("可以优化项目经历。多写量化结果。");

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");

        service.chatStream(auth, 1L, "帮我优化简历").collectList().block();

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().getInstructions().toString();
        assertThat(promptText).contains("Java开发工程师");
        assertThat(promptText).doesNotContain("data:image");
        assertThat(promptText.length()).isLessThan(10_000);
    }

    @Test
    void optimizeSectionThrowsWhenInvalidJsonResponse() {
        mockAiResponse("这不是一个有效的JSON响应");

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "测试公司"));

        assertThatThrownBy(() -> service.optimizeSection(request, "Java开发工程师"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.AI_RESPONSE_PARSE_ERROR.getCode());
    }

    @Test
    void optimizeSectionParsesJsonWrappedWithExplanation() {
        mockAiResponse("""
                以下是优化后的结果：
                ```json
                {"company": "优化后公司", "highlights": ["主导核心项目"]}
                ```
                希望对你有帮助。
                """);

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "测试公司"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

        assertThat(result).containsEntry("company", "优化后公司");
    }

    @Test
    void optimizeSectionParsesJsonWithoutCodeBlock() {
        mockAiResponse("好的，这是优化结果：\n{\"company\": \"优化后公司\", \"highlights\": [\"主导核心项目\"]}\n请查看。");

        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "测试公司"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

        assertThat(result).containsEntry("company", "优化后公司");
    }

    @Test
    void scoreResumeParsesJsonWithControlCharacters() {
        mockAiResponse("```json\n{\"score\": 85, \"dimensions\": {\"completeness\": 80, \"completenessComment\": \"较完整\", \"professionalism\": 90, \"professionalismComment\": \"专业\", \"matching\": 85, \"matchingComment\": \"匹配\"}, \"suggestions\": [\"补充量化\"]}\n```");

        SectionVO section = new SectionVO();
        section.setSectionType("basic");
        section.setSectionData(Map.of("name", "张三"));

        AiScoreVO result = service.scoreResume(List.of(section), "Java开发工程师");

        assertThat(result.getScore()).isEqualTo(85);
    }

    private SectionVO section(String type, Map<String, Object> data) {
        SectionVO section = new SectionVO();
        section.setSectionType(type);
        section.setSectionData(data);
        return section;
    }

    private void mockAiResponse(String text) {
        AssistantMessage msg = new AssistantMessage(text);
        ChatResponse response = new ChatResponse(List.of(new Generation(msg)));
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(response);
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("developer_001");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
    }
}
