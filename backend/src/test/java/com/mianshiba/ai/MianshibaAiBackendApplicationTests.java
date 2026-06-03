package com.mianshiba.ai;

import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "app.infrastructure.validate-on-startup=false",
        "spring.ai.deepseek.api-key=test-api-key",
        "spring.security.jwt.secret=test-jwt-secret-key-must-be-at-least-32-bytes",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class MianshibaAiBackendApplicationTests {

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ResumeMapper resumeMapper;

    @MockBean
    private ResumeSectionMapper resumeSectionMapper;

    @Test
    void contextLoads() {
    }
}
