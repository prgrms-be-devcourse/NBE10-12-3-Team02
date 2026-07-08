package com.back.domain.waiting.service;

import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.repository.ScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {
    private final WaitingQueueService waitingQueueService;
    private final WaitingQueueManager waitingQueueManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ScheduleRepository scheduleRepository;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processExpiredActiveUsers() {
        Set<String> keys = stringRedisTemplate.keys("queue:active:schedule:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            Long scheduleId = Long.parseLong(key.replace("queue:active:schedule:", ""));

            long expired = waitingQueueManager.removeExpiredActiveUsers(scheduleId);
            if (expired > 0) {
                Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
                if (schedule == null) continue;

                waitingQueueService.allowEntry(
                        schedule.getConcert().getConcertId(),
                        scheduleId
                );
            }
        }
    }
}
