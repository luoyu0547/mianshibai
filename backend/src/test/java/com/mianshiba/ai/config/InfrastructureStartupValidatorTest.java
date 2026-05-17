package com.mianshiba.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfrastructureStartupValidatorTest {

    @Test
    void runPassesWhenMysqlAndRedisAreAvailable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        InfrastructureStartupValidator validator = new InfrastructureStartupValidator(dataSource, redisConnectionFactory);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }
}
