package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase2Test {

    @Test
    void initSqlContainsPhase2ReviewTables() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS interview_report_enhancement");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS interview_turn_review");
        assertThat(sql).contains("UNIQUE KEY uk_report_id (report_id)");
        assertThat(sql).contains("KEY idx_report_id (report_id)");
        assertThat(sql).contains("KEY idx_turn_id (turn_id)");
    }
}
