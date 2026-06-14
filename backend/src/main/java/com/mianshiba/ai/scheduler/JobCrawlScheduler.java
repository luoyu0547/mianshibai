package com.mianshiba.ai.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobBatchCrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobCrawlScheduler {

    private final JobCrawlTaskMapper taskMapper;
    private final JobBatchCrawlService jobBatchCrawlService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 60000)
    public void runDueTasks() {
        if (!running.compareAndSet(false, true)) return;
        try {
            List<JobCrawlTask> tasks = taskMapper.selectList(
                    Wrappers.lambdaQuery(JobCrawlTask.class)
                            .eq(JobCrawlTask::getStatus, "enabled")
                            .le(JobCrawlTask::getNextRunAt, LocalDateTime.now()));
            for (JobCrawlTask task : tasks) {
                try {
                    jobBatchCrawlService.runTask(task.getId());
                    task.setLastRunAt(LocalDateTime.now());
                    task.setNextRunAt(calculateNextRun(task));
                    taskMapper.updateById(task);
                } catch (Exception e) {
                    log.error("Scheduler failed for task {}", task.getId(), e);
                }
            }
        } finally {
            running.set(false);
        }
    }

    private LocalDateTime calculateNextRun(JobCrawlTask task) {
        if (task.getScheduleType() == null) {
            return null;
        }
        return switch (task.getScheduleType()) {
            case "manual" -> null;
            case "daily" -> LocalDateTime.now().plusDays(1);
            case "weekly" -> LocalDateTime.now().plusWeeks(1);
            default -> LocalDateTime.now().plusHours(1);
        };
    }
}
