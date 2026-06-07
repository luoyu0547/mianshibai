package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase3Test {

    @Test
    void initSql_containsApplicationTables() throws IOException {
        String sql = new String(
                getClass().getClassLoader().getResourceAsStream("sql/init.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_application");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS application_todo");
        assertThat(sql).contains("idx_application_user_status");
        assertThat(sql).contains("idx_todo_user_completed_due");
    }
}