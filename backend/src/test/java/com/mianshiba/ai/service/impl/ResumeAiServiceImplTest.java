package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobMapper;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

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
    private ResumeSectionMapper resumeSectionMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private JobAnalysisMapper jobAnalysisMapper;

    private JwtUtils jwtUtils;
    private ResumeAiServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        service = new ResumeAiServiceImpl(chatClient, resumeMapper, resumeSectionMapper, userMapper, jwtUtils, jobMapper, jobAnalysisMapper);
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

        Map<String, Object> result = service.optimizeSection(request, "Java开发工程师");

        assertThat(result).containsEntry("company", "优化后公司");
        assertThat(result).containsKey("highlights");
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
