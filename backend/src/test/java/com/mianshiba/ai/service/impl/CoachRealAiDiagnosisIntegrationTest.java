package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地真实 AI 求职诊断排障测试。
 */
@SpringBootTest(properties = "app.infrastructure.validate-on-startup=false")
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "coach.real-ai", matches = "true")
class CoachRealAiDiagnosisIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CoachServiceImpl coachService;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private UserMapper userMapper;

    @Test
    void printsAdminPromptAndAiResponse() throws Exception {
        User admin = userMapper.selectById(1L);
        assertThat(admin).as("admin user id=1 must exist").isNotNull();

        CoachGenerateRequest request = new CoachGenerateRequest();
        request.setFocus("排查求职诊断为什么固定 60 分且显示数据不足");

        var snapshot = coachService.buildSnapshot(admin, request);
        String userPrompt = coachService.buildUserPrompt(snapshot);
        Path outputDir = Path.of("target", "coach-real-ai");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("system-prompt.txt"), CoachServiceImpl.SYSTEM_PROMPT, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("snapshot.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot), StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("user-prompt.json"), userPrompt, StandardCharsets.UTF_8);

        System.out.println("\n========== COACH SYSTEM PROMPT ==========");
        System.out.println(CoachServiceImpl.SYSTEM_PROMPT);
        System.out.println("\n========== COACH SNAPSHOT ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot));
        System.out.println("\n========== COACH USER PROMPT ==========");
        System.out.println(userPrompt);

        String response = chatClient.prompt()
                .system(CoachServiceImpl.SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();

        System.out.println("\n========== COACH AI RAW RESPONSE ==========");
        System.out.println(response);
        Files.writeString(outputDir.resolve("ai-response.json"), response, StandardCharsets.UTF_8);
        System.out.println("\nUTF-8 debug files written to: " + outputDir.toAbsolutePath());

        assertThat(response).isNotBlank();
    }
}
