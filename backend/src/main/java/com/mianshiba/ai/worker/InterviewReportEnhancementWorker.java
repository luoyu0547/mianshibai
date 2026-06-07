package com.mianshiba.ai.worker;

import com.mianshiba.ai.service.InterviewReportEnhancementQueue;
import com.mianshiba.ai.service.InterviewReportEnhancementService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewReportEnhancementWorker {

    private final StringRedisTemplate stringRedisTemplate;
    private final InterviewReportEnhancementService enhancementService;

    private static final String CONSUMER_NAME = "worker-1";

    @PostConstruct
    public void init() {
        try {
            stringRedisTemplate.opsForStream().createGroup(
                    InterviewReportEnhancementQueue.STREAM_KEY,
                    InterviewReportEnhancementQueue.GROUP_NAME);
        } catch (Exception e) {
            log.info("Consumer group already exists or Redis unavailable: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(InterviewReportEnhancementQueue.GROUP_NAME, CONSUMER_NAME),
                    StreamReadOptions.empty().count(5).block(Duration.ofSeconds(1)),
                    StreamOffset.create(InterviewReportEnhancementQueue.STREAM_KEY, ReadOffset.lastConsumed())
            );
            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    String enhancementId = String.valueOf(record.getValue().get("enhancementId"));
                    enhancementService.runTask(Long.valueOf(enhancementId));
                } catch (Exception e) {
                    log.error("Failed to process enhancement record {}", record.getId(), e);
                }
                stringRedisTemplate.opsForStream().acknowledge(
                        InterviewReportEnhancementQueue.STREAM_KEY,
                        InterviewReportEnhancementQueue.GROUP_NAME,
                        record.getId());
            }
        } catch (Exception e) {
            log.error("Redis Stream poll error", e);
        }
    }
}
