package com.mianshiba.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        JavaTimeModule module = new JavaTimeModule();
        LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss]"));
        module.addDeserializer(LocalDateTime.class, deserializer);
        return new Jackson2ObjectMapperBuilder()
                .modules(module);
    }
}
