package com.mianshiba.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 基础设施启动校验器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.infrastructure", name = "validate-on-startup", havingValue = "true", matchIfMissing = true)
public class InfrastructureStartupValidator implements ApplicationRunner {

    private final DataSource dataSource;

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. 校验 MySQL 连接可用
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(3)) {
                throw new IllegalStateException("MySQL connection is not valid");
            }
        }

        // 2. 校验 Redis 连接可用
        try (RedisConnection redisConnection = redisConnectionFactory.getConnection()) {
            String pong = redisConnection.ping();
            if (!StringUtils.equalsIgnoreCase("PONG", pong)) {
                throw new IllegalStateException("Redis connection ping failed: " + pong);
            }
        }

        // 3. 记录基础设施校验结果
        log.info("Infrastructure startup validation passed");
    }
}
