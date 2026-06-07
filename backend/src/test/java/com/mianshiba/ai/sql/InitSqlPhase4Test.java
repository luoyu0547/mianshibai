package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase4Test {

    @Test
    void initSqlContainsPhase4TrainingTables() throws IOException {
        String sql = new String(
                getClass().getClassLoader().getResourceAsStream("sql/init.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_plan");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_question");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_answer");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_answer_review");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS algorithm_recommendation");
    }
}
