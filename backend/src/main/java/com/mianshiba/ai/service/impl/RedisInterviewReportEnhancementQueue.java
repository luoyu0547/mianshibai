package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.model.entity.InterviewReportEnhancement;
import com.mianshiba.ai.service.InterviewReportEnhancementQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisInterviewReportEnhancementQueue implements InterviewReportEnhancementQueue {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void publish(InterviewReportEnhancement enhancement) {
        try {
            Map<String, String> body = Map.of(
                    "enhancementId", String.valueOf(enhancement.getId()),
                    "reportId", String.valueOf(enhancement.getReportId()),
                    "sessionId", String.valueOf(enhancement.getSessionId()),
                    "userId", String.valueOf(enhancement.getUserId())
            );
            stringRedisTemplate.opsForStream().add(STREAM_KEY, body);
        } catch (Exception e) {
            log.error("Redis Stream publish failed for enhancement {}", enhancement.getId(), e);
        }
    }
}
