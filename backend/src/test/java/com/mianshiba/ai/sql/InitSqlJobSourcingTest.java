package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlJobSourcingTest {

    @Test
    void initSqlContainsJobSourcingTablesAndColumns() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_crawl_task");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_crawl_run");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_crawl_item");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_recommendation");
        assertThat(sql).contains("crawl_task_id BIGINT");
        assertThat(sql).contains("recommendation_id BIGINT");
    }
}
