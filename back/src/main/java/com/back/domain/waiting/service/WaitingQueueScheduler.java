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

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processExpiredActiveUsers() {
        Set<String> activeScheduleIds = stringRedisTemplate.opsForSet().members("queue:active:schedules");
        if (activeScheduleIds == null || activeScheduleIds.isEmpty()) return;
        for (String idStr : activeScheduleIds) {
            Long scheduleId = Long.parseLong(idStr);
            long expired = waitingQueueManager.removeExpiredActiveUsers(scheduleId);

            if (expired > 0) {
                Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
                if (schedule == null) continue;
                waitingQueueService.allowEntry(
                        schedule.getConcert().getConcertId(),
                        scheduleId
                );
            }

            if (isQueueEmpty(scheduleId)) {
                stringRedisTemplate.opsForSet().remove("queue:active:schedules", idStr);
            }
        }
    }

    private boolean isQueueEmpty(Long scheduleId) {
        String waitKey = "queue:wait:schedule:" + scheduleId;
        String activeKey = "queue:active:schedule:" + scheduleId;

        Long waitCount = stringRedisTemplate.opsForZSet().zCard(waitKey);
        Long activeCount = stringRedisTemplate.opsForZSet().zCard(activeKey);

        return (waitCount == null || waitCount == 0) && (activeCount == null || activeCount == 0);
    }
}