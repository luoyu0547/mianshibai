package com.mianshiba.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库初始化配置测试
 */
class DatabaseInitializationPropertiesTest {

    @Test
    void applicationConfigEnablesInitSqlOnStartup() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load("application", new ClassPathResource("application.yml"));

        assertThat(propertySources).anySatisfy(propertySource -> {
            assertThat(propertySource.getProperty("spring.sql.init.mode")).isEqualTo("always");
            assertThat(propertySource.getProperty("spring.sql.init.schema-locations")).isEqualTo("classpath:sql/init.sql");
        });
    }

    @Test
    void applicationConfigUsesLocalProfileByDefault() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load("application", new ClassPathResource("application.yml"));

        assertThat(propertySources).anySatisfy(propertySource ->
                assertThat(propertySource.getProperty("spring.profiles.active")).isEqualTo("local"));
    }

    @Test
    void datasourceUrlCanCreateMissingDatabase() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load("application", new ClassPathResource("application.yml"));

        assertThat(propertySources).anySatisfy(propertySource ->
                assertThat(propertySource.getProperty("spring.datasource.url").toString())
                        .contains("createDatabaseIfNotExist=true"));
    }
}
