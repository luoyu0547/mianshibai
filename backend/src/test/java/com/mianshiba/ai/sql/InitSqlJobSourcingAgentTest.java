package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlJobSourcingAgentTest {

    @Test
    void initSql_shouldCreatePlatformAuthSessionTable() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS platform_auth_session");
        assertThat(sql).contains("platform VARCHAR(64)");
        assertThat(sql).contains("status VARCHAR(32) NOT NULL DEFAULT 'not_authorized'");
        assertThat(sql).contains("profile_path VARCHAR(1024)");
        assertThat(sql).contains("UNIQUE KEY uk_platform (platform)");
    }

    @Test
    void initSql_shouldDocumentAuthRequiredRunStatus() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("running/success/partial_success/failed/auth_required");
    }
}
