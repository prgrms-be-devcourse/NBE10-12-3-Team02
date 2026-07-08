package com.back.domain.waiting.service;

import com.back.domain.concert.service.ConcertService;
import com.back.domain.queue.event.EntryAllowedEvent;
import com.back.domain.queue.event.QueueRankUpdatedEvent;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.waiting.dto.ActiveEntry;
import com.back.domain.waiting.dto.WaitingQueueResponse;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {
    private final WaitingQueueManager waitingQueueManager;
    private final UserRepository userRepository;
    private final ConcertService concertService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Value("${queue.entry-token.ttl}")
    private Duration entryTokenTtl;
    @Value("${queue.batch-size}")
    private int batchSize;

    public WaitingQueueResponse registerWaiting(Long concertId, Long scheduleId, Long userId) {
        validateUser(userId);
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        Long rank = waitingQueueManager.registerWaiting(scheduleId, userId);

        return WaitingQueueResponse.of(
                concertId,
                scheduleId,
                userId,
                rank
        );
    }

    public WaitingQueueResponse showWaitingRank(Long concertId, Long scheduleId, Long userId) {
        validateUser(userId);
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        Long rank = waitingQueueManager.showWaitingRank(scheduleId, userId);

        return WaitingQueueResponse.of(
                concertId,
                scheduleId,
                userId,
                rank
        );
    }

    public void cancelWaiting(Long concertId, Long scheduleId, Long userId) {
        validateUser(userId);
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        waitingQueueManager.cancelWaiting(scheduleId, userId);
    }

    public void allowEntry(Long concertId, Long scheduleId) {

        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        long remainingSeats =
                scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(
                        scheduleId,
                        SeatStatus.AVAILABLE
                );

        long activeUsers = waitingQueueManager.countActiveUsers(scheduleId);

        long availableSlots = Math.max(0, remainingSeats - activeUsers);

        int count = (int) Math.min(availableSlots, batchSize);

        if (count == 0) {
            return;
        }

        List<Long> userIds = waitingQueueManager.popUsers(scheduleId, count);

        for (Long userId : userIds) {

            ActiveEntry activeEntry =
                    waitingQueueManager.addActiveUser(
                            scheduleId,
                            userId,
                            entryTokenTtl
                    );

            eventPublisher.publishEvent(
                    new EntryAllowedEvent(
                            scheduleId,
                            userId,
                            activeEntry.entryToken(),
                            activeEntry.expiredAt()
                    )
            );
        }
        if (!userIds.isEmpty()) {
            long remaining = waitingQueueManager.getRemainingCount(scheduleId);
            eventPublisher.publishEvent(
                    QueueRankUpdatedEvent.of(scheduleId, -1L, -1L, remaining)
            );
        }

        return;
    }


    private void validateUser(Long userId) {
        userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }
}
