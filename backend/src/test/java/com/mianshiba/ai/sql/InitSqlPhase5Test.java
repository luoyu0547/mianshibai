package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase5Test {

    @Test
    void initSqlContainsTrainingMasteryTable() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_mastery");
        assertThat(sql).contains("target_type VARCHAR(32) NOT NULL");
        assertThat(sql).contains("target_name VARCHAR(128) NOT NULL");
        assertThat(sql).contains("average_score DECIMAL(5,2) DEFAULT 0");
    }
}
